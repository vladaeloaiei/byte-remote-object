package byteremote.server.handler;

/**
 * Class used to keep an response
 */
public class Response
{
    private Object value = null;
    private Class  type  = null;

    public Response(Object value, Class type)
    {
        this.value = value;
        this.type  = type;
    }

    public Object getValue()
    {
        return this.value;
    }

    public Class getType()
    {
        return this.type;
    }
}
