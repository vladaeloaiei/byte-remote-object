package byteremote.generation.processor;

import byteremote.client.ByteClient;
import byteremote.generation.annotation.ByteRemote;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Class called during compilation to generate the class on client side
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("byteremote.generation.annotation.ByteRemote")
public class ByteClientAnnotationProcessor extends AbstractProcessor
{
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ByteRemote.class);

        for (Element e : elements)
        {
            try
            {
                ByteRemote     annotation     = e.getAnnotation(ByteRemote.class);
                JavaFileObject sourceFile     = null;
                String         className      = annotation.name();
                GenerateClass  implementClass = new GenerateClass();

                if (! isValidClassName(className))
                {
                    printError("Invalid class name: " + className, e);
                }

                implementClass.initializeClass(className.toLowerCase())
                              .imports(getPackageName(e) + "." + e.getSimpleName().toString())
                              .define(className, e.getSimpleName().toString())
                              .addMemberClient(ByteClient.class.getTypeName())
                              .addConstructor(ByteClient.class.getTypeName());

                for (ExecutableElement interfaceMethod : ElementFilter.methodsIn(e.getEnclosedElements()))
                {
                    if (ElementKind.METHOD == interfaceMethod.getKind())
                    {
                        if (interfaceMethod.getReturnType().getKind().isPrimitive())
                        {
                            printError("Methods with primitive return type are not allowed", e);
                            return true;
                        }

                        GenerateMethod generatedMethod = new GenerateMethod("@Override");
                        generatedMethod.type(interfaceMethod.getReturnType().toString())
                                       .name(interfaceMethod.getSimpleName().toString());

                        for (int i = 0; i < interfaceMethod.getParameters().size(); ++ i)
                        {
                            String paramType = interfaceMethod.getParameters().get(i).asType().toString();
                            String paramName = interfaceMethod.getParameters().get(i).getSimpleName().toString();
                            generatedMethod.addParam(paramType, paramName);
                        }

                        generatedMethod.defineBody(interfaceMethod, Exception.class.getName());
                        implementClass.addMethod(generatedMethod);
                    }
                }

                sourceFile = processingEnv.getFiler()
                                          .createSourceFile(className.toLowerCase() + "." + className);
                Writer writer = sourceFile.openWriter();
                writer.write(implementClass.end());
                writer.close();
            }
            catch (IOException ex)
            {
                printError(ex.getMessage(), e);
            }
        }

        return true;
    }

    /**
     * Get the name of the package of the provided element
     *
     * @param e The element
     * @return The package name
     */
    private String getPackageName(Element e)
    {
        String packageName = null;

        try
        {
            PackageElement packageOfElement = processingEnv.getElementUtils().getPackageOf(e);
            packageName = packageOfElement.getQualifiedName().toString();

            if (packageName.equals(""))
            {
                printError("The interface is not in a package!", e);
            }
        }
        catch (Exception ex)
        {
            printError("getPackageName: " + ex.getMessage(), e);
        }

        return packageName;
    }

    /**
     * Method used to print an error during compilation
     *
     * @param message The message wanted to be printed
     * @param e       The element on which the error appeared
     */
    private void printError(String message, Element e)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                e.getSimpleName() + ": " + message, e);
    }

    /**
     * Checks if a string is a valid class name
     *
     * @param className The name of the class
     * @return true for a valid class name, or false otherwise
     */
    private boolean isValidClassName(String className)
    {
        return SourceVersion.isIdentifier(className) && ! SourceVersion.isKeyword(className);
    }
}