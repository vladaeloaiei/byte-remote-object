package byteremote.client;

public interface ByteClient
{
    /**
     * Send a request to server to be processed.
     *
     * @param responseType - Type of the expected response (e.g. String, int, etc.. or null if no response is expected)
     * @param parameters   - The request parameters
     * @return response - The response or null if no response is expected
     * @throws Exception - In case the socket creation fails
     */
    public Object sendRequest(Class<?> responseType, Object... parameters)
            throws Exception;

    /**
     * Disconnect from server.
     * Close the tcp client socket
     *
     * @throws Exception - In case of the socket close fails
     */
    public void disconnect()
            throws Exception;
}
