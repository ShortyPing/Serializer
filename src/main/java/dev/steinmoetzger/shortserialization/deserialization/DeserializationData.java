/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization.deserialization;

import dev.steinmoetzger.shortserialization.SerializationUtil;
import dev.steinmoetzger.shortserialization.annotation.SerializableClass;
import dev.steinmoetzger.shortserialization.annotation.SerializableField;
import dev.steinmoetzger.shortserialization.exception.DeserializeException;
import org.reflections.Reflections;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeserializationData {


    private HashMap<String, DeserializationClass> classes;

    public DeserializationData() {
        this.classes = new HashMap<>();
    }

    public HashMap<String, DeserializationClass> getClasses() {
        return classes;
    }


    public Object toObject(String classpath) throws DeserializeException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Reflections reflections = new Reflections(classpath);
        HashMap<String, Object> javaClasses = new HashMap<>();
        Set<Class<?>> clazzes = reflections.getTypesAnnotatedWith(SerializableClass.class);
        Object obj;
        for (Map.Entry<String, DeserializationClass> entry : getClasses().entrySet()) {
            String uuid = entry.getKey();
            DeserializationClass clazz = entry.getValue();
            javaClasses.put(uuid, SerializationUtil.deserializeClass(clazz, classpath));
        }

        return javaClasses.get("ROOT");

    }

    @Override
    public String toString() {
        return "DeserializationData{" +
                "classes=" + classes +
                '}';
    }

    public static class DeserializationClass {
        private String uuid;
        private String fieldName;
        private HashMap<String, DeserializationObject> variables;


        public DeserializationClass(String uuid, String fieldName) {
            this.uuid = uuid;
            this.fieldName = fieldName;
            this.variables = new HashMap<>();
        }

        public String getUuid() {
            return uuid;
        }

        public String getFieldName() {
            return fieldName;
        }


        public HashMap<String, DeserializationObject> getVariables() {
            return variables;
        }


        @Override
        public String toString() {
            return "DeserializationClass{" +
                    "uuid='" + uuid + '\'' +
                    ", fieldName='" + fieldName + '\'' +
                    ", variables=" + variables +
                    '}';
        }
    }

    public static class DeserializationObject {

        private DeserializationType type;
        private String name;
        private Object value;

        @Override
        public String toString() {
            return "DeserializationObject{" +
                    "type=" + type +
                    ", name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }

        public DeserializationObject(DeserializationType type, String name, Object value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public DeserializationType getType() {
            return type;
        }

        public void setType(DeserializationType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
