package byteremote.common.socket.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * This class is a wrapper for java.net.DatagramSocket
 * Every single UDPSocket.receive() corresponds to a single UDPSocket.send()
 */
public class UDPSocket
{
    /* State machine codes */
    private static final byte HANDSHAKE_ID = - 1;
    private static final byte DATA_ID      = - 2;

    /* Time out */
    private static final int MAX_TIMEOUT = 2000;

    /* Size */
    private static final int MAX_PACKET_SIZE = 60000; /* Much less than 65535 - (20 + 8) */
    private static final int MAX_DATA_SIZE   = MAX_PACKET_SIZE - 3 * Integer.BYTES - 1;

    private DatagramSocket udpSocket        = null;
    private Random         idGenerator      = null;
    private String         lastIpReceived   = null;
    private int            lastPortReceived = 0;
    private int            packetTimeout    = 1000;

    /**
     * Default constructor.
     *
     * @throws UDPSocketException exception
     */
    public UDPSocket()
            throws UDPSocketException
    {
        try
        {
            this.udpSocket   = new DatagramSocket();
            this.idGenerator = new Random();
        }
        catch (SocketException ex)
        {
            /* catch the SocketException and throw it as a UDPSocketException */
            throw new UDPSocketException(ex, true);
        }
    }

    /**
     * Parameterized constructor.
     *
     * @param port localPort
     * @throws UDPSocketException exception
     */
    public UDPSocket(int port)
            throws UDPSocketException
    {
        try
        {
            this.udpSocket   = new DatagramSocket(port);
            this.idGenerator = new Random();
        }
        catch (SocketException ex)
        {
            /* catch the SocketException and throw it as a UDPSocketException */
            throw new UDPSocketException(ex, true);
        }
    }


    /**
     * Method used to send a message to the specified address
     *
     * @param message - Message to be sent
     * @param ip      - The ip address
     * @param port    - The port address
     * @throws UDPSocketException - in case of error
     */
    public void send(byte[] message, String ip, int port)
            throws UDPSocketException
    {
        if (! this.isClosed() && this.isBound())
        {
            /* the id is used to check if a packet is from the expected message or another */
            int messageId       = this.idGenerator.nextInt();
            int packetCode      = 0;
            int packetDataSize  = 0;
            int leftMessageSize = message.length; /* Size of the message not yet split */
            int numberOfPackets = (int)Math.ceil((double)message.length / UDPSocket.MAX_DATA_SIZE);

            ByteBuffer       packetContent = null; /* all data contained in a packet */
            DatagramPacket   infoPacket    = null; /* Used to send/receive handshake/resend/finish packets */
            DatagramPacket[] dataPackets   = null; /* all data packets needed to be sent */

            infoPacket  = new DatagramPacket(new byte[1 + 2 * Integer.BYTES], 1 + 2 * Integer.BYTES);
            dataPackets = new DatagramPacket[numberOfPackets];

            try
            {
                /* Connect this socket to the target (receiver) socket */
                this.udpSocket.connect(new InetSocketAddress(ip, port));

                /* First of all send the size of the message
                 * This is called handshake packet and it's content is :
                 * [handshake_id][message_id][message_to_be_sent_size] */
                ByteBuffer.wrap(infoPacket.getData())
                          .put(UDPSocket.HANDSHAKE_ID)
                          .putInt(messageId)
                          .putInt(message.length);
                this.udpSocket.send(infoPacket);

                /* Now set timeout and wait for acknowledge.
                 * The acknowledge packet is expected to be the handshake packet sent
                 * If the acknowledge is not received, the operation is aborted. */
                this.udpSocket.setSoTimeout(this.packetTimeout);
                this.udpSocket.receive(infoPacket);

                /* Check if is an acknowledge packet */
                packetCode = ByteBuffer.wrap(infoPacket.getData()).get();

                if (UDPSocket.HANDSHAKE_ID != packetCode)
                {
                    /* An unexpected packet have been received. Abort operation */
                    throw new UDPSocketException("Expected handshake packet:" + UDPSocket.HANDSHAKE_ID
                                                 + ", but received: " + packetCode, false);
                }

                /* Now send the message split in packets */
                for (int i = 0; i < numberOfPackets; ++ i)
                {
                    /* A data packet content is:
                     * [data_id][message_id][packet_index][data_size][data]
                     * Find the packet data size */
                    if (leftMessageSize < UDPSocket.MAX_DATA_SIZE)
                    {
                        packetDataSize = leftMessageSize;
                    }
                    else
                    {
                        packetDataSize = UDPSocket.MAX_DATA_SIZE;
                    }

                    leftMessageSize -= packetDataSize;

                    /* Now send the data packet */
                    packetContent  = ByteBuffer.allocate(1 + 3 * Integer.BYTES + packetDataSize)
                                               .put(UDPSocket.DATA_ID).putInt(messageId)
                                               .putInt(i).putInt(packetDataSize)
                                               .put(message, i * UDPSocket.MAX_DATA_SIZE, packetDataSize);
                    dataPackets[i] = new DatagramPacket(packetContent.array(), packetContent.array().length);
                    this.udpSocket.send(dataPackets[i]);
                }
            }
            catch (SocketTimeoutException ex)
            {
                /* First of all, catch the SocketTimeoutException
                 * and throw it as a non critical UDPSocketException */
                throw new UDPSocketException(ex, false);
            }
            catch (IOException ex)
            {
                /* Now, catch every DatagramSocket related exception
                 * and throw it as a critical UDPSocketException */
                throw new UDPSocketException(ex, true);
            }
            finally
            {
                /* End the connection with the receiver */
                this.udpSocket.disconnect();
            }
        }
        else
        {
            throw new UDPSocketException("Socket not bound", true);
        }
    }

    /**
     * Method that return a message received on the port specified at the construct time.<br>
     * Every UDPSocket.receive() corresponds to UDPSocket.send(message).<br>
     * <p>
     * Flow:<br>
     * &nbsp;   receive [handshake_id][nrPackets] - send [handshake_id][nrPackets]<br>
     * &nbsp;   foreach packet: receive [data_id][packet]<br>
     * <p>
     *
     * @return byte[] - Message
     * @throws UDPSocketException - in case of error
     */
    public byte[] receive()
            throws UDPSocketException
    {
        byte[] message = null;

        if (! this.isClosed() && this.isBound())
        {
            /* the id is used to check if a packet is from the expected message or another */
            byte packetCode               = 0;
            int  messageId                = 0;
            int  packetIndex              = 0;
            int  packetDataSize           = 0;
            int  numberOfTotalDataPackets = 0;

            ByteBuffer     packetContent = null; /* All data contained in a packet */
            DatagramPacket infoPacket    = null; /* Used to send/receive handshake packets */
            DatagramPacket dataPacket    = null; /* Used to receive data packets */

            infoPacket = new DatagramPacket(new byte[1 + 2 * Integer.BYTES], 1 + 2 * Integer.BYTES);
            dataPacket = new DatagramPacket(new byte[UDPSocket.MAX_PACKET_SIZE], UDPSocket.MAX_PACKET_SIZE);

            try
            {
                /* First of all wait for an handshake packet from any source
                 * [handshake_id][message_id][message_to_be_sent_size] */
                this.udpSocket.setSoTimeout(UDPSocket.MAX_TIMEOUT);
                this.udpSocket.receive(infoPacket);

                /* Check if is an acknowledge packet */
                packetContent = ByteBuffer.wrap(infoPacket.getData());
                packetCode    = packetContent.get();
                messageId     = packetContent.getInt();

                if (UDPSocket.HANDSHAKE_ID != packetCode)
                {
                    /* An unexpected packet have been received. Abort operation */
                    throw new UDPSocketException("Received packet code: " + packetCode
                                                 + ", but expected: " + UDPSocket.HANDSHAKE_ID, false);
                }

                /* save the last ip and port received */
                this.lastIpReceived   = infoPacket.getAddress().getHostAddress();
                this.lastPortReceived = infoPacket.getPort();

                /* Set a pseudo "connection" between this socket and the sender's socket */
                this.udpSocket.connect(new InetSocketAddress(this.lastIpReceived, this.lastPortReceived));

                /* Allocate buffer for message */
                message                  = new byte[packetContent.getInt()];
                numberOfTotalDataPackets = (int)Math.ceil((double)message.length / UDPSocket.MAX_DATA_SIZE);

                /* Send back what was received as acknowledge. */
                this.udpSocket.send(infoPacket);
                /* From now on, a time out is set for receive */
                this.udpSocket.setSoTimeout(this.packetTimeout);

                /* Wait for every expected packet */
                for (int i = 0; i < numberOfTotalDataPackets; ++ i)
                {
                    /* Receive data packet:
                     * [data_id][message_id][packet_index][data_size][data] */
                    this.udpSocket.receive(dataPacket);
                    packetContent = ByteBuffer.wrap(dataPacket.getData());

                    if ((UDPSocket.DATA_ID != packetContent.get()) || (messageId != packetContent.getInt()))
                    {
                        /* An unexpected packet have been received. Maybe is a foster packet, ignore it */
                        -- i;
                    }
                    else
                    {
                        packetIndex    = packetContent.getInt();
                        packetDataSize = packetContent.getInt();

                        /* Check if the packet index and size are valid */
                        if ((packetIndex * UDPSocket.MAX_DATA_SIZE + packetDataSize) > message.length)
                        {
                            /* The packed received is not in the expected bounds */
                            throw new UDPSocketException("The received packet is not in the expected bounds", false);
                        }

                        packetContent.get(message, packetIndex * UDPSocket.MAX_DATA_SIZE, packetDataSize);
                    }
                }
            }
            catch (SocketTimeoutException ex)
            {
                /* SocketTimeoutException caught. Abort operation */
                throw new UDPSocketException(ex, false);
            }
            catch (IOException ex)
            {
                /* Now, catch every other exception and throw it as a critical UDPSocketException */
                throw new UDPSocketException(ex, true);
            }
            finally
            {
                /* End the connection with the sender */
                this.udpSocket.disconnect();
            }
        }
        else
        {
            throw new UDPSocketException("Socket not bound.", true);
        }

        return message;
    }


    /**
     * Set timeout for waiting a data packet.
     * By default the timeout is 500ms and it can be changed in order to fit better
     * in a poor/good connection environment.
     *
     * @param timeOut wanted timeout
     */
    public void setDataTimeOut(int timeOut)
    {
        this.packetTimeout = timeOut;
    }

    /**
     * Get the last port received
     *
     * @return Last port received
     */
    public int getLastPort()
    {
        return this.lastPortReceived;
    }

    /**
     * Get the last ip received
     *
     * @return Last ip received
     */
    public String getLastIp()
    {
        return this.lastIpReceived;
    }

    /**
     * Returns the binding state of the socket.
     *
     * @return true if the socket successfully bound to an address
     */
    public boolean isBound()
    {
        return this.udpSocket.isBound();
    }

    /**
     * Returns whether the socket is closed or not.
     *
     * @return true if the socket has been closed
     */
    public boolean isClosed()
    {
        return this.udpSocket.isClosed();
    }

    /**
     * Closes the datagram socket associated with this socket.
     */
    public void close()
    {
        this.udpSocket.close();
    }
}
