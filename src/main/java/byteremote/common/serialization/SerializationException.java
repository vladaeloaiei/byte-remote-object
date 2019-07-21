package byteremote.common.serialization;

public class SerializationException extends Exception
{
    private static final long serialVersionUID = -182123637584484710L;

    public SerializationException()
    {
        super();
    }

    public SerializationException(String message)
    {
        super(message);
    }

    public SerializationException(Exception exception)
    {
        super(exception);
    }
}