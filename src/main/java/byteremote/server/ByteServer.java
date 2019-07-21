package byteremote.server;

public interface ByteServer
{
    /**
     * Start the server
     *
     * @param port The port on which the server listens
     * @throws Exception In case of error
     */
    public void start(int port)
            throws Exception;

    /**
     * Stop the server.
     * Note: After stopping the server, it cannot be started again.
     *
     * @throws Exception In case of error
     */
    public void stop()
            throws Exception;
}
