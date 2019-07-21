package byteremote.common.socket.tcp;

public class TCPSocketException extends Exception
{
    public TCPSocketException()
    {
        super();
    }

    public TCPSocketException(String message)
    {
        super(message);
    }

    public TCPSocketException(Throwable cause)
    {
        super(cause);
    }
}
