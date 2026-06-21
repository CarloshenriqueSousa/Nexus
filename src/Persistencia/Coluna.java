package Persistencia;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Coluna {
    String nome();
    String tipo();
    boolean unico() default false;
    boolean naoNulo() default false;
    String padrao() default "";
}
