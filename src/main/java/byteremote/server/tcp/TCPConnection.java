package byteremote.server.tcp;

import byteremote.common.serialization.DeserializationException;
import byteremote.common.serialization.SerializationException;
import byteremote.common.serialization.Serializer;
import byteremote.common.socket.tcp.TCPSocket;
import byteremote.common.socket.tcp.TCPSocketException;
import byteremote.server.handler.Request;
import byteremote.server.handler.Response;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * This class is used to handle a connection with a client using TCP protocol
 * Each connected client has assigned an unique socket
 */
class TCPConnection
{
    /**
     * In this thread-safe container the server will store all the incoming TCPConnections.
     * When a client will disconnect from the server, the connection will be
     * removed by itself from the container.
     * When the server is stopped, all the current active TCPConnections
     * will be forcibly stopped.
     */
    private Set<TCPConnection> tcpConnections   = null;
    private Object             exposedObject    = null;
    private TCPSocket          tcpClientSocket  = null;
    private Thread             connectionThread = null;
    private boolean            isLogEnabled     = false;


    /**
     * Create a new TCP Connection. A new thread will be created
     * which will wait for incoming requests from the provided tcp socket
     *
     * @param exposedObject  the exposed object
     * @param clientSocket   the tcp client socket
     * @param TCPConnections the tcp connections list
     * @param isLogEnabled   true if console log is enabled or false otherwise
     */
    TCPConnection(Object exposedObject,
                  TCPSocket clientSocket,
                  Set<TCPConnection> TCPConnections,
                  boolean isLogEnabled)
    {
        this.tcpConnections   = TCPConnections;
        this.exposedObject    = exposedObject;
        this.tcpClientSocket  = clientSocket;
        this.isLogEnabled     = isLogEnabled;
        this.connectionThread = new Thread(this::connectionTask);
        this.connectionThread.start();
    }

    /**
     * Close the connection and wait for the thread to finish
     *
     * @throws TCPSocketException   In case of a tcp socket error
     * @throws InterruptedException In case that the calling thread was interrupted
     */
    void close()
            throws TCPSocketException, InterruptedException
    {
        /* Socket.close() already checks
         * if the socket is closed or not */
        if (null != this.tcpClientSocket)
        {
            this.tcpClientSocket.close();
        }

        /* Thread.join() already checks
         * if the thread is alive or not */
        this.connectionThread.join();
    }

    /**
     * The task of the connection
     */
    private void connectionTask()
    {
        boolean  isActive = true;
        Response response = null;

        if ((null != this.exposedObject) && (null != this.tcpClientSocket))
        {
            while (isActive)
            {
                /* if something is not good with the received bytes
                 * don't close the server */
                try
                {
                    response = Request.process(this.exposedObject, this.tcpClientSocket.receive());

                    if (Void.TYPE != response.getType())
                    {
                        this.tcpClientSocket.send(Serializer.serialize(response.getValue()).array());
                    }
                }
                catch (TCPSocketException ex)
                {
                    /* A socket related exception was caught. Close the connection  */
                    isActive = false;

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
                     * Keep the connection alive */
                    if (this.isLogEnabled)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        }

        /* This connection is no longer active. The client or the server closed it.
         * Close the socket client if is opened and remove this connection from the
         * server's TCPConnections list */
        try
        {
            /* If the socket is already closed, it means that
             * the server closed this connection and is not needed to
             * remove it from the TCPConnections list */
            if ((null != this.tcpClientSocket) && ! this.tcpClientSocket.isClosed())
            {
                this.tcpClientSocket.close();

                /* Remove this connection from the TCP Connections list */
                if (null != this.tcpConnections)
                {
                    this.tcpConnections.remove(this);
                }
            }
        }
        catch (TCPSocketException ex)
        {
            if (this.isLogEnabled)
            {
                ex.printStackTrace();
            }
        }
    }
}
