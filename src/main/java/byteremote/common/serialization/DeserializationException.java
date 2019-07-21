package byteremote.common.serialization;

public class DeserializationException extends Exception
{
    private static final long serialVersionUID = 5842479381722991849L;

    public DeserializationException()
    {
        super();
    }

    public DeserializationException(String message)
    {
        super(message);
    }

    public DeserializationException(Exception exception)
    {
        super(exception);
    }
}
