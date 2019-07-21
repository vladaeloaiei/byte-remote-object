package byteremote.generation.processor;

/**
 * Class used to generate a class
 */
class GenerateClass
{
    /* SYSTEM.LINE_BREAK */
    static final String LINE_BREAK  = System.getProperty("line.separator");
    /* INDENTATION */
    static final String TAB         = "    ";
    /* CLIENT NAME */
    static final String CLIENT_NAME = "client";

    private String        className = null;
    private StringBuilder builder   = null;

    GenerateClass()
    {
        builder = new StringBuilder();
    }

    /**
     * Initialize the class infos
     *
     * @param packageName The package of the class
     * @return this
     */
    GenerateClass initializeClass(String packageName)
    {
        builder.append("package ")
               .append(packageName)
               .append(";")
               .append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.LINE_BREAK);

        return this;
    }

    /**
     * Add imports for the generate class
     *
     * @param lib Library name
     * @return this
     */
    GenerateClass imports(String lib)
    {
        builder.append("import ")
               .append(lib)
               .append(";")
               .append(GenerateClass.LINE_BREAK);
        return this;
    }

    /**
     * Define the class signature
     *
     * @param name       Name of the class
     * @param interfaces Interface that implements
     * @return this
     */
    GenerateClass define(String name, String interfaces)
    {
        className = name;

        builder.append(GenerateClass.LINE_BREAK)
               .append("public class ")
               .append(name)
               .append(" ");

        if (null != interfaces && ! interfaces.isEmpty())
            builder.append("implements ")
                   .append(interfaces);

        builder.append(GenerateClass.LINE_BREAK)
               .append("{")
               .append(GenerateClass.LINE_BREAK);

        return this;
    }

    /**
     * Add client private member
     *
     * @param clientType The type of the client
     * @return this
     */
    GenerateClass addMemberClient(String clientType)
    {
        builder.append(GenerateClass.TAB)
               .append("private ")
               .append(clientType)
               .append(" ")
               .append(GenerateClass.CLIENT_NAME)
               .append(" = null;")
               .append(GenerateClass.LINE_BREAK);

        return this;
    }

    /**
     * Add constructor
     *
     * @param clientType Type of the client that this class is using
     * @return this
     */
    GenerateClass addConstructor(String clientType)
    {
        builder.append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.TAB)
               .append("public ")
               .append(this.className)
               .append("(")
               .append(clientType)
               .append(" ")
               .append(GenerateClass.CLIENT_NAME)
               .append(")")
               .append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.TAB)
               .append("{")
               .append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.TAB)
               .append(GenerateClass.TAB)
               .append("this.")
               .append(GenerateClass.CLIENT_NAME)
               .append(" = ")
               .append(GenerateClass.CLIENT_NAME)
               .append(";")
               .append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.TAB)
               .append("}")
               .append(GenerateClass.LINE_BREAK)
               .append(GenerateClass.LINE_BREAK);

        return this;
    }

    /**
     * Add method
     *
     * @param m The method wanted to be added
     * @return this
     */
    GenerateClass addMethod(GenerateMethod m)
    {
        builder.append(m.end())
               .append(GenerateClass.LINE_BREAK);
        return this;
    }

    /**
     * Finish the class and convert it into String
     *
     * @return the finished class
     */
    String end()
    {
        builder.append("}")
               .append(GenerateClass.LINE_BREAK);
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }
}
