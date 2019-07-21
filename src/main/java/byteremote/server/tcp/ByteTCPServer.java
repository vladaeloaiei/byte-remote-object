package byteremote.server.tcp;

import byteremote.common.socket.tcp.TCPSocket;
import byteremote.common.socket.tcp.TCPSocketException;
import byteremote.server.ByteServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This Class implements server side for TCP Connections
 */
public class ByteTCPServer implements ByteServer
{
    /**
     * In this thread-safe container the server will store all the incoming TCPConnections.
     * When a client will disconnect from the server, the connection will be
     * removed by itself from the container.
     * When the server is stopped, all the current active TCPConnections
     * will be forcibly stopped.
     */
    private final Set<TCPConnection> tcpConnections = Collections.synchronizedSet(new HashSet<>());

    private boolean      isLogEnabled    = false;
    private Object       exposedObject   = null;
    private ServerSocket tcpServerSocket = null;
    private Thread       serverThread    = null;


    /**
     * Create a new tcp server
     *
     * @param exposedObject the object wanted to be exposed
     * @param isLogEnabled  true if console log is enabled or false otherwise
     */
    public ByteTCPServer(Object exposedObject, boolean isLogEnabled)
    {
        this.isLogEnabled  = isLogEnabled;
        this.exposedObject = exposedObject;
    }

    /**
     * The task of the server
     */
    private void serverTask()
    {
        boolean isActive = true;

        while (isActive)
        {
            /* if something is not good with the server socket
             * stop the server */
            try
            {
                /* Wait for a client */
                TCPSocket tcpSocket = new TCPSocket(this.tcpServerSocket.accept());
                /* Create a new TCPConnection for this client
                 * and add it into tcpConnections list */
                this.tcpConnections.add(new TCPConnection(this.exposedObject,
                        tcpSocket, this.tcpConnections, this.isLogEnabled));
            }
            catch (TCPSocketException | IOException ex)
            {
                /* Caught exception. This means that was an error underlying protocol.
                 * Stop the server */
                isActive = false;

                if (this.isLogEnabled)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Start the TCP server thread
     *
     * @param port the tcp server port on which the server will wait connections
     */
    @Override
    public void start(int port)
            throws Exception
    {
        try
        {
            this.tcpServerSocket  = new ServerSocket(port);
            this.serverThread = new Thread(this::serverTask);
            this.serverThread.start();
        }
        catch (IOException ex)
        {
            /* catch the IOException from ServerSocket(port)
             * and throw it as a TCPSocketException */
            throw new TCPSocketException(ex);
        }
    }

    /**
     * Stop the TCP server thread.
     * Note: After stopping the server, it cannot be started again.
     */
    @Override
    public void stop()
            throws Exception
    {
        try
        {
            /* Close the ServerSocket. In the server thread will be thrown a
             * SocketException from ServerSocket.accept() */
            if (null != this.tcpServerSocket)
            {
                this.tcpServerSocket.close();
            }

            /* Wait for the main server thread */
            if (null != this.serverThread)
            {
                this.serverThread.join();
            }

            /* Close all the active tcpConnections */
            synchronized (this.tcpConnections)
            {
                for (TCPConnection tcpConnection : this.tcpConnections)
                {
                    try
                    {
                        tcpConnection.close();
                    }
                    catch (InterruptedException ex)
                    {
                        if (this.isLogEnabled)
                        {
                            ex.printStackTrace();
                        }
                    }
                }

                /* Clear the tcpConnections list */
                this.tcpConnections.clear();
            }
        }
        catch (IOException ex)
        {
            throw new TCPSocketException(ex);
        }
        catch (InterruptedException ex)
        {
            if (this.isLogEnabled)
            {
                ex.printStackTrace();
            }
        }
    }
}
