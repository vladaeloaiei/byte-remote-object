package byteremote.generation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ByteRemote
{
    /**
     * name of the class that will be generated on client side
     *
     * @return The name
     */
    String name();
}
