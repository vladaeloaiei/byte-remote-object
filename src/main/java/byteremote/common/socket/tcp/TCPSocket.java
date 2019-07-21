package byteremote.common.socket.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * This class is a wrapper for java.net.Socket
 * Every single TCPSocket.receive() corresponds to a single TCPSocket.send()
 */
public class TCPSocket
{
    private static final int CONNECTION_TIMEOUT = 2000;

    /* PRIVATE MEMBERS */
    private Socket       tcpSocket       = null;
    private OutputStream tcpSocketWriter = null;
    private InputStream  tcpSocketReader = null;

    /**
     * Parameterized constructor.
     *
     * @param ip   - Host name
     * @param port - Port number
     * @throws TCPSocketException - in case of error
     */
    public TCPSocket(String ip, int port)
            throws TCPSocketException
    {
        try
        {
            InetSocketAddress endpoint = new InetSocketAddress(ip, port);

            this.tcpSocket = new Socket();
            this.tcpSocket.connect(endpoint, TCPSocket.CONNECTION_TIMEOUT);

            this.tcpSocketWriter = this.tcpSocket.getOutputStream();
            this.tcpSocketReader = this.tcpSocket.getInputStream();
        }
        catch (IOException e)
        {
            /* catch the IOException and throw it as a TCPSocketException */
            throw new TCPSocketException(e);
        }
    }

    /**
     * Parameterized constructor.
     * Wrap an existing Socket
     *
     * @param socket - Socket to wrap
     * @throws TCPSocketException - in case of error
     */
    public TCPSocket(Socket socket)
            throws TCPSocketException
    {
        try
        {
            this.tcpSocket       = socket;
            this.tcpSocketWriter = this.tcpSocket.getOutputStream();
            this.tcpSocketReader = this.tcpSocket.getInputStream();
        }
        catch (IOException e)
        {
            /* catch the IOException and throw it as a TCPSocketException */
            throw new TCPSocketException(e);
        }
    }

    /**
     * Method used send a message to the connected socket via TCP
     * <p> Note: Can not be sent a message bigger than Int.MAX_VALUE </p>
     *
     * @param message The message to be sent
     * @throws TCPSocketException If any error occurs
     */
    public void send(byte[] message)
            throws TCPSocketException
    {
        if (this.isClosed() || (! this.isConnected()))
        {
            throw new TCPSocketException("Socket not connected");
        }
        else
        {
            try
            {
                /* [size_of_message][message] */
                this.tcpSocketWriter.write(ByteBuffer.allocate(Integer.BYTES).putInt(message.length).array());
                this.tcpSocketWriter.write(message, 0, message.length);
                /* Immediately send the byte[] tcp via socket */
                this.tcpSocketWriter.flush();
            }
            catch (IOException e)
            {
                /* catch the IOException and throw it as a TCPSocketException */
                throw new TCPSocketException(e);
            }
        }
    }

    /**
     * Method used to receive a message from the connected socket via TCP
     *
     * @return the received message
     * @throws TCPSocketException If any error occurs
     */
    public byte[] receive()
            throws TCPSocketException
    {
        if (! this.isClosed() && this.isConnected())
        {
            try
            {
                int    numberOfBytesRead = 0;
                byte[] message           = new byte[Integer.BYTES];

                /* read [size_of_message] */
                numberOfBytesRead = tcpSocketReader.read(message, 0, Integer.BYTES);

                if (Integer.BYTES == numberOfBytesRead)
                {
                    int numberOfTotalBytesRead = 0;
                    int numberOfRemainingBytes = 0;

                    numberOfRemainingBytes = ByteBuffer.wrap(message).getInt();
                    message                = new byte[numberOfRemainingBytes];

                    /* read [message] */
                    while (0 != numberOfRemainingBytes)
                    {
                        numberOfBytesRead = tcpSocketReader.read(message, numberOfTotalBytesRead,
                                numberOfRemainingBytes);
                        numberOfRemainingBytes -= numberOfBytesRead;
                        numberOfTotalBytesRead += numberOfBytesRead;
                    }

                    return message;
                }
                else
                {
                    throw new TCPSocketException("Not enough bytes read: Received " + numberOfBytesRead
                                                 + " expected " + Integer.BYTES);
                }
            }
            catch (IOException ex)
            {
                /* catch the IOException and throw it as a TCPSocketException */
                throw new TCPSocketException(ex);
            }
        }
        else
        {
            throw new TCPSocketException("Socket not connected");
        }
    }

    /**
     * Returns the connection state of the wrapped socket
     *
     * @return true if the socket is connected, or false otherwise
     */
    public boolean isConnected()
    {
        return this.tcpSocket.isConnected();
    }

    /**
     * Returns the closed state of the wrapped socket
     *
     * @return true if the socket is closed, or false otherwise
     */
    public boolean isClosed()
    {
        return this.tcpSocket.isClosed();
    }

    /**
     * Close the wrapped socket
     *
     * @throws TCPSocketException If any error occurs
     */
    public void close()
            throws TCPSocketException
    {
        try
        {
            this.tcpSocketReader.close();
            this.tcpSocketWriter.close();
            this.tcpSocket.close();
        }
        catch (IOException e)
        {
            /* catch the IOException and throw it as a TCPSocketException */
            throw new TCPSocketException(e);
        }
    }
}
