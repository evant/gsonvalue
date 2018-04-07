package me.tatarka.gsonvalue.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GsonValue {

    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Creator {
    }
}

