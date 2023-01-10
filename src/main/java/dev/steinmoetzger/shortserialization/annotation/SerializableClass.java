/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableClass {

    String name();
    char delim() default '\n';
}
