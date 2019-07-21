package byteremote.common.socket.udp;

public class UDPSocketException extends Exception
{
    /**
     * A critical exception appears when
     * the socket is in a nonfunctional state
     */
    private boolean critical = false;

    public UDPSocketException(boolean critical)
    {
        super();
        this.critical = critical;
    }

    public UDPSocketException(String message, boolean critical)
    {
        super(message);
        this.critical = critical;
    }

    public UDPSocketException(Throwable cause, boolean critical)
    {
        super(cause);
        this.critical = critical;
    }

    public boolean isCritical()
    {
        return this.critical;
    }
}
