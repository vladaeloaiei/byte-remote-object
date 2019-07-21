package byteremote.client.udp;

import byteremote.client.ByteClient;
import byteremote.common.serialization.Serializer;
import byteremote.common.socket.udp.UDPSocket;
import byteremote.common.socket.udp.UDPSocketException;

import java.nio.ByteBuffer;

public class ByteUDPClient implements ByteClient
{
    /**
     * The UDP socket assigned to this client.
     * It will be used to connect to the server.
     */
    private UDPSocket udpSocket  = null;
    private String    serverIP   = null;
    private int       serverPort = - 1;

    /**
     * Create a new ByteUDPClient
     *
     * @param clientPort The port of the local client
     * @param serverIp   The ip of the server
     * @param serverPort The port of the server
     * @throws UDPSocketException - In case the socket creation fails
     */
    public ByteUDPClient(String serverIp, int serverPort, int clientPort)
            throws UDPSocketException
    {
        this.udpSocket  = new UDPSocket(clientPort);
        this.serverIP   = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Send a request to server to be processed.
     *
     * @param responseType Type of the expected response (e.g. String, int, etc.. or null if no response is expected)
     * @param parameters   The request parameters
     * @return response The response or null if no response is expected
     * @throws UDPSocketException In case the socket creation fails
     */
    @Override
    public Object sendRequest(Class<?> responseType, Object... parameters)
            throws Exception
    {
        Object response           = null;
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
            this.udpSocket.send(serializedRequest, this.serverIP, this.serverPort);

            /* The function is not void type,
             * so a return object is expected */
            if (null != responseType)
            {
                serializedResponse = this.udpSocket.receive();
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
     * Set timeout for waiting a data packet.
     * By default the timeout is 1000ms and it can be changed in order to fit better
     * in a poor/good connection.
     *
     * @param timeOut wanted timeout
     */
    public void setDataTimeOut(int timeOut)
    {
        this.udpSocket.setDataTimeOut(timeOut);
    }

    /**
     * Disconnect from server.
     * Close the tcp client socket
     *
     * @throws Exception - In case of the socket close fails
     */
    @Override
    public void disconnect()
            throws Exception
    {
        /* DatagramSocket.close() already checks
         * if the socket is closed or not */
        this.udpSocket.close();
    }
}
