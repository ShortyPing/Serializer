/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization;

import dev.steinmoetzger.shortserialization.annotation.SerializableClass;
import dev.steinmoetzger.shortserialization.annotation.SerializableField;
import dev.steinmoetzger.shortserialization.exception.SerializeException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class SerializationUtil {

    private static ArrayList<File> lockedFiles = new ArrayList<>();

    enum DirectTypes {

        STRING(String.class, "STR"),
        INTEGER(Integer.class, "INT"),
        DOUBLE(Double.class, "DOUB"),
        FLOAT(Float.class, "FLT"),
        BYTE(Byte.class, "BYTE"),
        CHAR(Character.class, "CHAR");

        Class<?> clazz;
        String prefix;

        DirectTypes(Class<?> clazz, String prefix) {
            this.prefix = prefix;
            this.clazz = clazz;
        }


        public static DirectTypes fromType(Class<?> clazz) {
            return Arrays.stream(values()).filter(value -> value.clazz == MethodType.methodType(clazz).wrap().returnType()).findFirst().orElse(null);
        }
    }


    public static void serialize(Object object, File file, boolean append) throws IOException, SerializeException, IllegalAccessException {
        serialize(object, file, append, UUID.randomUUID().toString(), new HashMap<>());
    }

    private static void serialize(Object object, File file, boolean append, String suffix, HashMap<Object, String> serializeTracker) throws IOException, SerializeException, IllegalAccessException {


        if (!file.exists())
            file.createNewFile();


        if (append)
            while (lockedFiles.contains(file)) {
            }

        lockedFiles.add(file);

        serializeTracker.put(object, suffix);


        if (!object.getClass().isAnnotationPresent(SerializableClass.class))
            throw new SerializeException("SerializableClass annotation is missing");

        Field[] fields = object.getClass().getDeclaredFields();

        String className = object.getClass().getAnnotation(SerializableClass.class).name();
        char delim = object.getClass().getAnnotation(SerializableClass.class).delim();


        StringBuilder builder = new StringBuilder("\n# Beginning of a serialized object from ShortSerializer.\n# WARNING: If the file is changed, the functionality might not be fully granted\n")
                .append("::BEGIN CLASS: ")
                .append(className)
                .append(suffix != null ? "#" + suffix : "")
                .append("\n")
                .append(delim != '\n' ? "::DELIMITER: " + "'" + delim + "'\n" : "");


        if (fields.length < 1)
            throw new SerializeException("This object does not have any serializable fields");

        for (Field field : fields) {
            if (field.isAnnotationPresent(SerializableField.class)) {
                field.trySetAccessible();


                String name = field.getDeclaredAnnotation(SerializableField.class).name();
                if (name.isEmpty()) {
                    name = field.getName();
                }


                DirectTypes type = DirectTypes.fromType(field.getType());
                Object val = null;
                try {
                    val = field.get(object);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (type != null) {
                    builder.append("::DAT [")
                            .append(type.prefix)
                            .append("] ")
                            .append(name)
                            .append("=")
                            .append(val == null ? "<NULL>" : val.toString())
                            .append(delim);
                } else {

                    if (val == null) {
                        builder.append("::DAT [<REF>] ")
                                .append(name)
                                .append("=").append("<NULL>").append(delim);
                    } else {
                        if (!val.getClass().isAnnotationPresent(SerializableClass.class))
                            throw new SerializeException("Any child class of serializable object must have SerializableClass annotation");
                        UUID uuid = UUID.randomUUID();
                        builder.append("::DAT [<REF>] ")
                                .append(name)
                                .append("=").append(val.getClass().getAnnotation(SerializableClass.class).name()).append("#").append(uuid).append(delim);

                        Object finalVal = val;

                        for (Field f : val.getClass().getDeclaredFields()) {
                            f.setAccessible(true);
                            if (f.isAnnotationPresent(SerializableField.class)) {
                                if (serializeTracker.containsKey(f.get(val))) {

                                }
                            }

                        }

                        new Thread(() -> {
                            try {
                                serialize(finalVal, file, true, uuid.toString(), serializeTracker);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (SerializeException e) {
                                throw new RuntimeException(e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                }

            }
        }

        if (delim != '\n')
            builder.append("\n");
        builder.append("::END CLASS: ").append(className).append(suffix != null ? "#" + suffix : "");

        if (!append) {
            Files.write(file.toPath(), builder.toString().getBytes());

            lockedFiles.remove(file);
            return;
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
        writer.append("\n");
        writer.append(builder.toString());
        writer.close();
        lockedFiles.remove(file);

    }

}
