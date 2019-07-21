package byteremote.generation.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * Class used to generate a method
 */
class GenerateMethod
{
    private boolean       firstParam = false;
    private StringBuilder builder    = null;

    GenerateMethod(String ann)
    {
        firstParam = true;
        builder    = new StringBuilder();

        if (null != ann && ! ann.isEmpty())
        {
            builder.append(GenerateClass.TAB)
                   .append(ann)
                   .append(GenerateClass.LINE_BREAK);
        }

        builder.append(GenerateClass.TAB)
               .append("public ");
    }

    /**
     * Add the return type of the method
     *
     * @param returnType Return type
     * @return this
     */
    GenerateMethod type(String returnType)
    {
        builder.append(returnType)
               .append(" ");
        return this;
    }

    /**
     * Add name of the method
     *
     * @param name Name
     * @return this
     */
    GenerateMethod name(String name)
    {
        builder.append(name)
               .append("(");
        return this;
    }

    /**
     * Add parameter
     *
     * @param type       Type of the parameter
     * @param identifier Parameter name
     * @return this
     */
    GenerateMethod addParam(String type, String identifier)
    {
        if (! firstParam)
        {
            builder.append(", ")
                   .append(type)
                   .append(" ")
                   .append(identifier);
        }
        else
        {
            builder.append(type)
                   .append(" ")
                   .append(identifier);
            firstParam = false;
        }

        return this;
    }

    /**
     * Define the body of the method
     *
     * @param method    The called method signature
     * @param exception The thrown exception by this method
     * @return this
     */
    GenerateMethod defineBody(ExecutableElement method, String exception)
    {
        builder.append(")");

        if (null != exception && ! exception.isEmpty())
        {
            builder.append(" throws ")
                   .append(exception);
        }

        builder.append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.TAB)
               .append("{")
               .append(GenerateClass.LINE_BREAK)
               .append(generateClientMethodBody(method));

        return this;
    }

    /**
     * Finish the method and convert it to String
     *
     * @return The finished method
     */
    String end()
    {
        builder.append(GenerateClass.TAB)
               .append("}")
               .append(GenerateClass.LINE_BREAK);
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }

    /**
     * Generate the body of the method. This class calls the input method and it's parameters
     *
     * @param method The method that will be called
     * @return The result
     */
    private String generateClientMethodBody(ExecutableElement method)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(GenerateClass.TAB)
               .append(GenerateClass.TAB);

        if (! (method.getReturnType() instanceof javax.lang.model.type.NoType))
        {
            builder.append("return (")
                   .append(method.getReturnType())
                   .append(")");
        }

        builder.append("this.")
               .append(GenerateClass.CLIENT_NAME)
               .append(".sendRequest(");

        if ((method.getReturnType() instanceof javax.lang.model.type.NoType))
        {
            builder.append("null");
        }
        else
        {
            builder.append(method.getReturnType())
                   .append(".class");
        }

        builder.append(",")
               .append("\"").append(method.getSimpleName()).append("\"");

        for (VariableElement parameter : method.getParameters())
        {
            builder.append(", ")
                   .append(parameter.getSimpleName().toString());
        }

        builder.append(");")
               .append(GenerateClass.LINE_BREAK);

        return builder.toString();
    }
}
