package byteremote.server.handler;

import byteremote.common.serialization.DeserializationException;
import byteremote.common.serialization.Serializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Class used to handle a request
 */
public class Request
{
    /**
     * This method is used to process a request received from a client.
     * This will try to deserialize the message and call the method
     * from the exposed object
     *
     * @param exposedObject exposed object from which the method will be called
     * @param message       the message
     * @return a response which contains the method return type and the returned value
     * if exists or null otherwise
     * @throws DeserializationException  in case that the message can not be deserialized.
     * @throws NoSuchMethodException     in case that the object does not contain the required method.
     * @throws IllegalAccessException    in case that the method can not be accessed.
     * @throws InvocationTargetException in case that the underlying method throws an exception.
     */
    public static Response process(Object exposedObject, byte[] message)
            throws DeserializationException, NoSuchMethodException,
                   IllegalAccessException, InvocationTargetException
    {
        Method     wantedMethod   = null;
        ByteBuffer inputBytes     = ByteBuffer.wrap(message);
        String     methodName     = (String)(Serializer.deserialize(inputBytes, String.class)[0]);
        Method[]   exposedMethods = exposedObject.getClass().getMethods();

        for (Method method : exposedMethods)
        {
            if (method.getName().equals(methodName))
            {
                /* Info: This protocol does not support
                 * Method overloads */
                wantedMethod = method;
                break;
            }
        }

        if (wantedMethod != null)
        {
            Class<?>[] types  = wantedMethod.getParameterTypes();
            Object[]   params = Serializer.deserialize(inputBytes, types);

            return new Response(wantedMethod.invoke(exposedObject, params),
                    wantedMethod.getReturnType());
        }
        else
        {
            throw new NoSuchMethodException("Server do not have method: " + methodName + " " + "exposed.");
        }
    }
}
