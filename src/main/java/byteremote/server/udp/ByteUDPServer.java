package byteremote.server.udp;

import byteremote.common.serialization.DeserializationException;
import byteremote.common.serialization.SerializationException;
import byteremote.common.serialization.Serializer;
import byteremote.common.socket.udp.UDPSocket;
import byteremote.common.socket.udp.UDPSocketException;
import byteremote.server.ByteServer;
import byteremote.server.handler.Request;
import byteremote.server.handler.Response;

import java.lang.reflect.InvocationTargetException;

/**
 * This class implements server side for UDP Connections
 */
public class ByteUDPServer implements ByteServer
{
    private boolean   isLogEnabled  = false;
    private Object    exposedObject = null;
    private Thread    serverThread  = null;
    private UDPSocket udpSocket     = null;
    private int       timeout       = - 1;

    /**
     * Create a new udp server
     *
     * @param exposedObject the object wanted to be exposed
     * @param isLogEnabled  true if console log is enabled or false otherwise
     */
    public ByteUDPServer(Object exposedObject, boolean isLogEnabled)
    {
        this.exposedObject = exposedObject;
        this.isLogEnabled  = isLogEnabled;
    }

    /**
     * The task of the server
     */
    private void serverTask()
    {
        boolean  isActive = true;
        Response response = null;

        if ((null != this.exposedObject) && (null != this.udpSocket))
        {
            while (isActive)
            {
                /* If something is not good with the server socket
                 * close the server */
                try
                {
                    response = Request.process(this.exposedObject, this.udpSocket.receive());

                    if (Void.TYPE != response.getType())
                    {
                        /* Send the response to the source from which we received request */
                        this.udpSocket.send(Serializer.serialize(response.getValue()).array(),
                                this.udpSocket.getLastIp(), this.udpSocket.getLastPort());
                    }
                }
                catch (UDPSocketException ex)
                {
                    /* A socket related exception was caught
                     * Close the server if the error is critical */
                    if (ex.isCritical())
                    {
                        isActive = false;
                    }

                    if (this.isLogEnabled)
                    {
                        ex.printStackTrace();
                    }
                }
                catch (DeserializationException |
                               NoSuchMethodException |
                               IllegalAccessException |
                               InvocationTargetException |
                               SerializationException ex)
                {
                    /* An exception was caught from the Request.process
                     * Keep the server alive */
                    if (this.isLogEnabled)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Start the TCP server thread
     */
    @Override
    public void start(int port)
            throws Exception
    {
        this.udpSocket = new UDPSocket(port);

        if (- 1 != this.timeout)
        {
            this.udpSocket.setDataTimeOut(this.timeout);
        }

        this.serverThread = new Thread(this::serverTask);
        this.serverThread.start();
    }

    /**
     * Stop the UDP server thread.
     * Note: After stopping the server, it cannot be started again.
     */
    @Override
    public void stop()
            throws Exception
    {
        /* Close the UDPSocket. In the server thread will be thrown a
         * UDPSocketException from UDPSocket.receive() */
        if (null != this.udpSocket)
        {
            this.udpSocket.close();
        }

        try
        {
            /* Wait for the main server thread */
            if (null != this.serverThread)
            {
                this.serverThread.join();
            }
        }
        catch (InterruptedException ex)
        {
            if (this.isLogEnabled)
            {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Set timeout for waiting a data packet.
     * By default the timeout is 1000ms and it can be changed in order to fit better
     * in a poor/good connection.
     *
     * @param timeOut wanted timeout
     */
    public void setDataTimeOut(int timeOut)
    {
        this.timeout = timeOut;
    }
}
