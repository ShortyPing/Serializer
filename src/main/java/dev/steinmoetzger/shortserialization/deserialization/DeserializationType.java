/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization.deserialization;

public enum DeserializationType {

    STRING(String.class),
    INTEGER(Integer.class),
    DOUBLE(Double.class),
    FLOAT(Float.class),
    BYTE(Byte.class),
    CHAR(Character.class),
    SHRT(Short.class),
    REFERENCE(Object.class); // class attribute irrelevant for references, because determined with reflections in deserialization

    public Class<?> clazz;

    DeserializationType(Class<?> clazz) {
        this.clazz = clazz;
    }
}
