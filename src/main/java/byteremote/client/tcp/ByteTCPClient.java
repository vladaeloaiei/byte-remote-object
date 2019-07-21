package byteremote.client.tcp;

import byteremote.client.ByteClient;
import byteremote.common.serialization.Serializer;
import byteremote.common.socket.tcp.TCPSocket;
import byteremote.common.socket.tcp.TCPSocketException;

import java.nio.ByteBuffer;

public class ByteTCPClient implements ByteClient
{
    /**
     * The TCP socket assigned to this client.
     * It will be used to connect to the server.
     */
    private TCPSocket tcpSocket = null;

    /**
     * Connect to the ByteTCPServer
     *
     * @param serverIp   The ip of the server
     * @param serverPort The port of the server
     * @throws TCPSocketException In case the socket connection fails
     */
    public ByteTCPClient(String serverIp, int serverPort)
            throws TCPSocketException
    {
        this.tcpSocket = new TCPSocket(serverIp, serverPort);
    }

    /**
     * Send a request to server to be processed.
     *
     * @param responseType Type of the expected response (e.g. String, int, etc.. or null if no response is expected)
     * @param parameters   The request parameters
     * @return The response or null if no response is expected
     * @throws Exception In case the socket creation fails
     */
    public Object sendRequest(Class<?> responseType, Object... parameters)
            throws Exception
    {
        Object response = null;
        /* Serialize the parameters */
        byte[] serializedRequest  = Serializer.serialize(parameters).array();
        byte[] serializedResponse = null;

        /* If more than 2 threads call .send and .receive on the same
         * socket (from the same "this"), it could lead to an unwanted
         * behaviour. For example,
         *      - Thread 1 calls .send(request)
         *      - Thread 2 calls .send(request)
         *      - Thread 2 calls .receive(response)
         *      - Thread 1 calls .receive(response)
         * In the end, the Thread 2 will receive the response for the call
         * of the Thread 1 and vice-versa */
        synchronized (this)
        {
            this.tcpSocket.send(serializedRequest);

            /* The function is not void type,
             * so a return object is expected */
            if (null != responseType)
            {
                serializedResponse = this.tcpSocket.receive();
            }
        }

        /* Deserialize the response if exists */
        if (null != serializedResponse)
        {
            response = Serializer.deserialize(ByteBuffer.wrap(serializedResponse), responseType)[0];
        }

        return response;
    }

    /**
     * Disconnect from server.
     * Close the tcp client socket
     *
     * @throws Exception In case of the socket close fails
     */
    public void disconnect()
            throws Exception
    {
        /* Socket.close() already checks
         * if the socket is closed or not */
        this.tcpSocket.close();
    }
}
