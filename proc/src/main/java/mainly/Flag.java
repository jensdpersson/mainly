package mainly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Flag {
    /** A list of names for this flag, e.g. [--port, -p] */
    String[] names() default {};
    
    /** Ordering for positional arguments. Lower order is assigned earlier.
    Ignored if names() is given*/
    int order() default 0;
    
    /** A help message*/
    String help() default "";  
}