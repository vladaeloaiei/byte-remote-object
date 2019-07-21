package byteremote.common.serialization;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Serializer
{
    /**
     * The maximum depth allowed for an object.
     * This is used to avoid the StackOverflowError
     * due to possibility of recurse too deeply.
     */
    private static final int MAX_DEPTH = 20;

    /**
     * Method used to serialize a variable length list of objects into a ByteBuffer
     * The primitives, primitives wrappers and String will be
     * serialized as native. The others will be opened in a recurrent way.
     *
     * <p> Important: Do not use this function to serialize circular objects or Java Collections.
     * e.g. Circular Linked Lists.
     * </p>
     *
     * @param objects Variable object list to be serialized
     * @return Objects serialized in a ByteBuffer
     * @throws SerializationException In case that an error occurs
     */
    public static ByteBuffer serialize(Object... objects)
            throws SerializationException
    {
        if (null == objects)
        {
            throw new SerializationException("Null input parameter: objects.");
        }

        ByteArrayOutputStream serializedParameters = new ByteArrayOutputStream();
        DataOutputStream      outputStream         = new DataOutputStream(serializedParameters);

        try
        {
            for (Object object : objects)
            {
                Serializer.serializeObject(outputStream, object, 0);
            }

            outputStream.close();
        }
        catch (IOException ex)
        {
            /* Catch IOException and throw it as SerializationException */
            throw new SerializationException(ex);
        }

        return ByteBuffer.wrap(serializedParameters.toByteArray());
    }

    /**
     * This method is used to serialize an object.
     * The primitives, primitives wrappers and String will be
     * serialized as native. The others will be opened in a recurrent way.
     *
     * <p> Important: Do not use this function to serialize circular objects or Java Collections.
     * e.g. Circular Linked Lists.
     * </p>
     *
     * @param outputStream DataOutputStream in which the serialized object is written
     * @param inputObject  Abstract input object
     * @param currentDepth The current depth in the call stack
     * @throws IOException            Input/output exception
     * @throws SerializationException Serialization exception
     */
    private static void serializeObject(DataOutputStream outputStream, Object inputObject, int currentDepth)
            throws IOException, SerializationException
    {
        if (null == outputStream)
        {
            throw new SerializationException("Null input parameter: outputStream.");
        }

        if (Serializer.MAX_DEPTH < currentDepth)
        {
            throw new SerializationException("This function recurs too deeply.");
        }

        /* If the object is null, mark it as null with 1*/
        if (null == inputObject)
        {
            /* byte[] = [1] */
            outputStream.write(1);
        }
        else if (inputObject.getClass().isArray())
        {
            Serializer.serializeArray(outputStream, inputObject, currentDepth);
        }
        else
        {
            /* byte[] = [0][object : ?bytes] */
            outputStream.write(0);

            switch (inputObject.getClass().getTypeName())
            {
                case "boolean":
                case "java.lang.Boolean":
                {
                    /* byte[] = [1/0 : 1byte] */
                    outputStream.writeBoolean((Boolean)inputObject);
                    break;
                }
                case "byte":
                case "java.lang.Byte":
                {
                    /* byte[] = [byte : 1byte] */
                    outputStream.writeByte((Byte)inputObject);
                    break;
                }
                case "char":
                case "java.lang.Character":
                {
                    /* byte[] = [char : 2bytes] */
                    outputStream.writeChar((Character)inputObject);
                    break;
                }
                case "short":
                case "java.lang.Short":
                {
                    /* byte[] = [short : 2bytes] */
                    outputStream.writeShort((Short)inputObject);
                    break;
                }
                case "int":
                case "java.lang.Integer":
                {
                    /* byte[] = [integer : 4bytes] */
                    outputStream.writeInt((Integer)inputObject);
                    break;
                }
                case "long":
                case "java.lang.Long":
                {
                    /* byte[] = [long : 8bytes] */
                    outputStream.writeLong((Long)inputObject);
                    break;
                }
                case "float":
                case "java.lang.Float":
                {
                    /* byte[] = [float : 4bytes] */
                    outputStream.writeFloat((Float)inputObject);
                    break;
                }

                case "double":
                case "java.lang.Double":
                {
                    /* byte[] = [double : 8bytes] */
                    outputStream.writeDouble((Double)inputObject);
                    break;
                }
                case "java.lang.String":
                {
                    /* byte[] = [UTF8_string_size : 4bytes][message : UTF8_string bytes] */
                    byte[] serializedString = ((String)inputObject).getBytes(StandardCharsets.UTF_8);
                    outputStream.writeInt(serializedString.length);
                    outputStream.write(serializedString);
                    break;
                }
                default:
                {
                    /* Unknown object type. This will be deserialized via reflection */
                    /* byte[] = [object : ?bytes] */
                    try
                    {
                        Field[] inputObjectMembers = inputObject.getClass().getDeclaredFields();

                        for (Field member : inputObjectMembers)
                        {
                            if (! Modifier.isFinal(member.getModifiers()))
                            {
                                member.setAccessible(true);
                                Serializer.serializeObject(outputStream, member.get(inputObject), currentDepth + 1);
                            }
                        }
                    }
                    catch (SecurityException | IllegalAccessException ex)
                    {
                        throw new SerializationException(ex);
                    }
                }
            }
        }
    }

    /**
     * Method used to serialize an array
     * If it is an array of primitives it will be serialized as a single object,
     * if it is an array of objects it will be serialized element by element
     * (it will call serializeObject(element))
     * <p> Note: Can not be serialized arrays bigger than 1^31-1 elements </p>
     *
     * @param outputStream DataOutputStream in which the serialized object is written
     * @param inputArray   Abstract input array
     * @param currentDepth The current depth in the call stack
     * @throws IOException            Input/output exception
     * @throws SerializationException Serialization exception
     */
    private static void serializeArray(DataOutputStream outputStream, Object inputArray, int currentDepth)
            throws IOException, SerializationException
    {
        if (null == outputStream)
        {
            throw new SerializationException("Null input parameter: outputStream.");
        }

        if (Serializer.MAX_DEPTH < currentDepth)
        {
            throw new SerializationException("This function recurs too deep.");
        }

        /* If the object is null, mark it as null with 1 */
        if (null == inputArray)
        {
            /* byte[] = [1] */
            outputStream.write(1);
        }
        else if (! inputArray.getClass().isArray())
        {
            throw new SerializationException("This object: " + inputArray.getClass().getTypeName() +
                                             " is not an array type.");
        }
        else
        {
            /* byte[] = [0][inputArray : ?bytes] */
            outputStream.write(0);

            switch (inputArray.getClass().getTypeName())
            {
                case "byte[]":
                {
                    /* byte[] = [byte_array size : 4bytes][message : byte_array bytes] */
                    outputStream.writeInt(((byte[])inputArray).length);
                    outputStream.write((byte[])inputArray);
                    break;
                }
                case "boolean[]":
                {
                    /* byte[] = [boolean_array size : 4bytes][message : boolean_array bytes] */
                    int arrayLength = ((boolean[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [1/0 : 1byte] */
                        outputStream.writeBoolean(((boolean[])inputArray)[i]);
                    }
                    break;
                }
                case "char[]":
                {
                    /* byte[] = [char_array size : 4bytes][message : char_array bytes] */
                    int arrayLength = ((char[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [char : 2bytes] */
                        outputStream.writeChar(((char[])inputArray)[i]);
                    }

                    break;
                }
                case "short[]":
                {
                    /* byte[] = [short_array size : 4bytes][message : short_array bytes] */
                    int arrayLength = ((short[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [short : 2bytes] */
                        outputStream.writeShort(((short[])inputArray)[i]);
                    }

                    break;
                }
                case "int[]":
                {
                    /* byte[] = [int_array size : 4bytes][message : int_array bytes] */
                    int arrayLength = ((int[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [integer : 4bytes] */
                        outputStream.writeInt(((int[])inputArray)[i]);
                    }

                    break;
                }
                case "long[]":
                {
                    /* byte[] = [long_array size : 4bytes][message : long_array bytes] */
                    int arrayLength = ((long[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [long : 8bytes] */
                        outputStream.writeLong(((long[])inputArray)[i]);
                    }

                    break;
                }
                case "float[]":
                {
                    /* byte[] = [float_array size : 4bytes][message : float_array bytes] */
                    int arrayLength = ((float[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [float : 4bytes] */
                        outputStream.writeFloat(((float[])inputArray)[i]);
                    }

                    break;
                }
                case "double[]":
                {
                    /* byte[] = [double_array size : 4bytes][message : double_array bytes] */
                    int arrayLength = ((double[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [double : 8bytes] */
                        outputStream.writeDouble(((double[])inputArray)[i]);
                    }

                    break;
                }
                default:
                {
                    /* This is not an array of primitives. It will be serialized element by element */
                    /* byte[] = [object_array size : 4bytes][message : object_array bytes] */
                    int arrayLength = ((Object[])inputArray).length;

                    outputStream.writeInt(arrayLength);

                    for (int i = 0; i < arrayLength; ++ i)
                    {
                        /* [object : ?bytes] */
                        Serializer.serializeObject(outputStream, ((Object[])inputArray)[i], currentDepth + 1);
                    }
                }
            }
        }
    }

    /**
     * The method deserialize the inputBuffer into Object[] described by parameters.
     * <p>If there are not enough bytes in the input buffer, this method will throw an BufferUnderflowException</p>
     *
     * @param inputBuffer input Buffer
     * @param parameters  expected objects class
     * @return deserialized objects
     * @throws DeserializationException in case of error
     */
    public static Object[] deserialize(ByteBuffer inputBuffer, Class<?>... parameters)
            throws DeserializationException
    {
        if (null == inputBuffer)
        {
            throw new DeserializationException("Null input parameter: inputBuffer.");
        }

        if (null == parameters)
        {
            throw new DeserializationException("Null input parameter: parameters.");
        }

        Object[] objectArray = new Object[parameters.length];

        for (int i = 0; i < parameters.length; ++ i)
        {
            objectArray[i] = Serializer.deserializeObject(inputBuffer, parameters[i], 0);
        }

        return objectArray;
    }

    /**
     * This method will reads the necessary amount of bytes and will reconstruct the Object.
     *
     * @param inputBytes   ByteArrayInputStream that contains the object (or more) serialized in bytes.
     *                     The size of the array can be bigger that expected, but only the first bytes needed will be
     *                     used.
     *                     (e.g. only 4 bytes read from buffer if Integer is expected)
     * @param objectClass  Class of the expected object (e.g. java.lang.String)
     * @param currentDepth The current depth in the call stack
     * @return requested object or null in case of exception
     * @throws DeserializationException If inputBytes does not contain enough information to deserialize
     *                                  objects defined in objectClass
     */
    private static Object deserializeObject(ByteBuffer inputBytes, Class<?> objectClass, int currentDepth)
            throws DeserializationException
    {
        Object deserializedObject = null;

        if (null == objectClass)
        {
            throw new DeserializationException("Null input parameter: inputBytes.");
        }

        if (null == inputBytes)
        {
            throw new DeserializationException("Null input parameter: inputBytes.");
        }

        if (MAX_DEPTH < currentDepth)
        {
            throw new DeserializationException("This function recurs too deeply.");
        }

        if (objectClass.isArray())
        {
            deserializedObject = Serializer.deserializeArray(inputBytes, objectClass, currentDepth);
        }
        /* INFO: If the expected object is a primitive and null serialized is received,
         * this function call will throw a NullPointerException */
        else if (0 == inputBytes.get())
        {
            switch (objectClass.getTypeName())
            {
                case "boolean":
                case "java.lang.Boolean":
                {
                    /* byte[] = [1/0 : 1byte] */
                    deserializedObject = inputBytes.get() == 1;
                    break;
                }
                case "byte":
                case "java.lang.Byte":
                {
                    /* byte[] = [byte : 1byte] */
                    deserializedObject = inputBytes.get();
                    break;
                }
                case "char":
                case "java.lang.Character":
                {
                    /* byte[] = [char : 2bytes] */
                    deserializedObject = inputBytes.getChar();
                    break;
                }
                case "short":
                case "java.lang.Short":
                {
                    /* byte[] = [short : 2bytes] */
                    deserializedObject = inputBytes.getShort();
                    break;
                }
                case "int":
                case "java.lang.Integer":
                {
                    /* byte[] = [int : 4bytes] */
                    deserializedObject = inputBytes.getInt();
                    break;
                }
                case "long":
                case "java.lang.Long":
                {
                    /* byte[] = [long : 8bytes] */
                    deserializedObject = inputBytes.getLong();
                    break;
                }
                case "float":
                case "java.lang.Float":
                {
                    /* byte[] = [float : 4bytes] */
                    deserializedObject = inputBytes.getFloat();
                    break;
                }
                case "double":
                case "java.lang.Double":
                {
                    /* byte[] = [double : 8bytes] */
                    deserializedObject = inputBytes.getDouble();
                    break;
                }
                case "java.lang.String":
                {
                    /* byte[] = [string_size : 4bytes][message : size bytes] */
                    /* Read string size */
                    int stringSize = inputBytes.getInt();
                    /* Read string */
                    byte[] stringBytes = new byte[stringSize];
                    inputBytes.get(stringBytes, 0, stringSize);
                    deserializedObject = new String(stringBytes, StandardCharsets.UTF_8);
                    break;
                }
                default:
                {
                    /* Unknown object type. This will be deserialized via reflection */
                    try
                    {
                        Field[]        unknownObjectMembers = objectClass.getDeclaredFields();
                        Constructor<?> constructor          = objectClass.getDeclaredConstructor();

                        constructor.setAccessible(true);
                        deserializedObject = constructor.newInstance();

                        for (Field member : unknownObjectMembers)
                        {
                            if (! Modifier.isFinal(member.getModifiers()))
                            {
                                member.setAccessible(true);
                                member.set(deserializedObject, Serializer.deserializeObject(inputBytes,
                                        member.getType(), currentDepth + 1));
                            }
                        }
                    }
                    catch (NoSuchMethodException |
                                   InstantiationException |
                                   IllegalAccessException |
                                   InvocationTargetException ex)
                    {
                        /* Catch these exceptions and throw as DeserializationException */
                        throw new DeserializationException(ex);
                    }

                    break;
                }
            }
        }

        return deserializedObject;
    }

    /**
     * This method will reads the necessary amount of bytes and will reconstruct the Object.
     *
     * @param inputBytes   ByteArrayInputStream that contains the object (or more) serialized in bytes.
     *                     The size of the array can be bigger that expected, but only the first bytes needed will be
     *                     used.
     *                     (e.g. only 4 bytes read from buffer if Integer is expected)
     * @param arrayClass   Class of the expected object (e.g. java.lang.int[])
     * @param currentDepth The current depth in the call stack
     * @return requested object or null in case of exception
     * @throws DeserializationException If inputBytes does not contain enough information to deserialize
     *                                  objects defined in objectClass
     * @throws NullPointerException     If the inputBytes or objectClass is null.
     */
    private static Object deserializeArray(ByteBuffer inputBytes, Class<?> arrayClass, int currentDepth)
            throws DeserializationException
    {
        Object deserializedObject = null;

        if (null == arrayClass)
        {
            throw new DeserializationException("Null input parameter: arrayClass.");
        }

        if (null == inputBytes)
        {
            throw new DeserializationException("Null input parameter: inputBytes.");
        }

        if (Serializer.MAX_DEPTH < currentDepth)
        {
            throw new DeserializationException("This function recurs too deeply.");
        }

        /* If the expected array is not null, fill it with elements*/
        if (0 == inputBytes.get())
        {
            if (! arrayClass.isArray())
            {
                throw new DeserializationException("This object: " + arrayClass.getTypeName() +
                                                   " is not an array type.");
            }
            else
            {
                int arrayLength = inputBytes.getInt();

                if (0 > arrayLength)
                {
                    throw new DeserializationException("Invalid array length: " + arrayLength);
                }

                switch (arrayClass.getTypeName())
                {
                    case "byte[]":
                    {
                        /* byte[] = [byte_array size : 4bytes][message : byte_array bytes] */
                        deserializedObject = new byte[arrayLength];
                        inputBytes.get((byte[])deserializedObject, 0, arrayLength);

                        break;
                    }
                    case "boolean[]":
                    {
                        /* byte[] = [boolean_array size : 4bytes][message : boolean_array bytes] */
                        deserializedObject = new boolean[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [1/0 : 1byte] */
                            ((boolean[])deserializedObject)[i] = (inputBytes.get() != 0);
                        }

                        break;
                    }
                    case "char[]":
                    {
                        /* byte[] = [char_array size : 4bytes][message : char_array bytes] */
                        deserializedObject = new char[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [char : 2bytes] */
                            ((char[])deserializedObject)[i] = inputBytes.getChar();
                        }

                        break;
                    }
                    case "short[]":
                    {
                        /* byte[] = [short_array size : 4bytes][message : short_array bytes] */
                        deserializedObject = new short[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [short : 2bytes] */
                            ((short[])deserializedObject)[i] = inputBytes.getShort();
                        }

                        break;
                    }
                    case "int[]":
                    {
                        /* byte[] = [int_array size : 4bytes][message : int_array bytes] */
                        deserializedObject = new int[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [int : 4bytes] */
                            ((int[])deserializedObject)[i] = inputBytes.getInt();
                        }

                        break;
                    }
                    case "long[]":
                    {
                        /* byte[] = [long_array size : 4bytes][message : long_array bytes] */
                        deserializedObject = new long[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [long : 8bytes] */
                            ((long[])deserializedObject)[i] = inputBytes.getLong();
                        }

                        break;
                    }
                    case "float[]":
                    {
                        /* byte[] = [float_array size : 4bytes][message : float_array bytes] */
                        deserializedObject = new float[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [float : 4bytes] */
                            ((float[])deserializedObject)[i] = inputBytes.getFloat();
                        }

                        break;
                    }
                    case "double[]":
                    {
                        /* byte[] = [double_array size : 4bytes][message : double_array bytes] */
                        deserializedObject = new double[arrayLength];

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            /* byte[] = [double : 8bytes] */
                            ((double[])deserializedObject)[i] = inputBytes.getDouble();
                        }

                        break;
                    }
                    default:
                    {
                        /* This is not an array of primitives. It will be deserialized element by element */

                        deserializedObject = Array.newInstance(arrayClass.getComponentType(), arrayLength);

                        for (int i = 0; i < arrayLength; ++ i)
                        {
                            ((Object[])deserializedObject)[i] = Serializer.deserializeObject(inputBytes,
                                    arrayClass.getComponentType(), currentDepth + 1);
                        }
                    }
                }
            }
        }

        return deserializedObject;
    }
}