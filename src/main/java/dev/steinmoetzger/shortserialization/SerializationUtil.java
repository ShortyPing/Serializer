/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization;

import dev.steinmoetzger.shortserialization.annotation.SerializableClass;
import dev.steinmoetzger.shortserialization.annotation.SerializableField;
import dev.steinmoetzger.shortserialization.deserialization.DeserializationData;
import dev.steinmoetzger.shortserialization.deserialization.DeserializationType;
import dev.steinmoetzger.shortserialization.exception.DeserializeException;
import dev.steinmoetzger.shortserialization.exception.SerializeException;
import org.reflections.Reflections;

import java.io.*;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;

public class SerializationUtil {

    private static ArrayList<File> lockedFiles = new ArrayList<>();

    enum DirectTypes {

        STRING(String.class, "STR"),
        INTEGER(Integer.class, "INT"),
        DOUBLE(Double.class, "DOUB"),
        FLOAT(Float.class, "FLT"),
        BYTE(Byte.class, "BYTE"),
        CHAR(Character.class, "CHAR"),
        SHORT(Short.class, "SHRT");

        Class<?> clazz;
        String prefix;

        DirectTypes(Class<?> clazz, String prefix) {
            this.prefix = prefix;
            this.clazz = clazz;
        }


        public static DirectTypes fromType(Class<?> clazz) {
            return Arrays.stream(values()).filter(value -> value.clazz == MethodType.methodType(clazz).wrap().returnType()).findFirst().orElse(null);
        }

        public static DirectTypes fromPrefix(String prefix) {
            return Arrays.stream(values()).filter(t -> t.prefix.equals(prefix)).findFirst().orElse(null);
        }
    }


    public static void serialize(Object object, File file) throws IOException, SerializeException, IllegalAccessException {

        serialize(object, file, false, "ROOT", new HashMap<>());
    }

    private static void serialize(Object object, File file, boolean append, String suffix, HashMap<Integer, String> serializeTracker) throws IOException, SerializeException, IllegalAccessException {


        if (!file.exists())
            file.createNewFile();


        if (append)
            while (lockedFiles.contains(file)) {
            }

        lockedFiles.add(file);

        if (!serializeTracker.containsKey(object.hashCode()))
            serializeTracker.put(object.hashCode(), suffix);

        if (!object.getClass().isAnnotationPresent(SerializableClass.class))
            throw new SerializeException("SerializableClass annotation is missing");

        Field[] fields = object.getClass().getDeclaredFields();

        String className = object.getClass().getAnnotation(SerializableClass.class).name();


        StringBuilder builder = new StringBuilder("\n# Beginning of a serialized object from ShortSerializer.\n# WARNING: If the file is changed, the functionality might not be fully granted\n")
                .append("::BEGIN CLASS: ")
                .append(className)
                .append(suffix != null ? "#" + suffix : "")
                .append("\n");


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
                            .append("\n");
                } else {
                    if (val == null) {
                        builder.append("::DAT [<REF>] ")
                                .append(name)
                                .append("=").append("<NULL>").append("\n");
                    } else {
                        if (!val.getClass().isAnnotationPresent(SerializableClass.class))
                            throw new SerializeException("Any child class of serializable object must have SerializableClass annotation");

                        // handling circular references
                        if (serializeTracker.containsKey(val.hashCode())) {
                            builder.append("::DAT [<REF>] ")
                                    .append(name)
                                    .append("=").append(val.getClass().getAnnotation(SerializableClass.class).name()).append("#").append(serializeTracker.get(val.hashCode())).append("\n");

                        } else {
                            UUID uuid = UUID.randomUUID();

                            serializeTracker.put(val.hashCode(), uuid.toString());
                            builder.append("::DAT [<REF>] ")
                                    .append(name)
                                    .append("=").append(val.getClass().getAnnotation(SerializableClass.class).name()).append("#").append(uuid).append("\n");

                            Object finalVal = val;


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
        }

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

    public static void setField(Object object, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(name);

        field.setAccessible(true);

        field.set(object, value);
    }

    public static Object deserializeClass(DeserializationData.DeserializationClass deserializationClass, String classpath) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, DeserializeException, NoSuchFieldException {
        Reflections reflections = new Reflections(classpath);
        Set<Class<?>> clazzez = reflections.getTypesAnnotatedWith(SerializableClass.class);
        Object obj = null;
        for (Class<?> clazz : clazzez) {
            if (clazz.isAnnotationPresent(SerializableClass.class) && clazz.getAnnotation(SerializableClass.class).name().equals(deserializationClass.getFieldName())) {
                obj = clazz.getDeclaredConstructor().newInstance();
            }
        }

        if (obj == null)
            throw new DeserializeException("Did not find class with name SerializableClass annotation and name argument: " + deserializationClass.getFieldName());

        for (Map.Entry<String, DeserializationData.DeserializationObject> entry : deserializationClass.getVariables().entrySet()) {
            String varName = entry.getKey();
            DeserializationData.DeserializationObject variable = entry.getValue();


            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(SerializableField.class) && (field.getAnnotation(SerializableField.class).name().equals(varName) || field.getAnnotation(SerializableField.class).name().equals(""))) {
                    Object value = null;
                    if (variable.getType() == DeserializationType.STRING) {
                        value = MethodType.methodType(variable.getType().clazz).wrap().returnType().getDeclaredMethod("valueOf", Object.class).invoke(null, variable.getValue());
                    } else if (variable.getType() == DeserializationType.REFERENCE) {
                        value = deserializeClass((DeserializationData.DeserializationClass) variable.getValue(), classpath);
                    } else {
                        if(((String) variable.getValue()).equals("<NULL>"))
                            value = null;
                        value = MethodType.methodType(variable.getType().clazz).wrap().returnType().getDeclaredMethod("valueOf", String.class).invoke(null, variable.getValue());
                    }



                    setField(obj, field.getName(), value);
                }
            }


        }

        return obj;

    }

    public static DeserializationData deserialize(File file) throws IOException, DeserializeException {

        if (!file.exists())
            throw new FileNotFoundException();

        BufferedReader reader = new BufferedReader(new FileReader(file));

        DeserializationData deserializationData = new DeserializationData();
        DeserializationData.DeserializationClass currentClass = null;

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("::"))
                continue;

            if (line.startsWith("::BEGIN CLASS")) {
                if (currentClass != null)
                    throw new DeserializeException("Syntax Error: Cannot begin class without closed previous...");

                String[] args = line.split(" ");
                String s = args[2];
                String fieldName = s.split("#")[0];
                String uuid = s.split("#")[1];
                currentClass = new DeserializationData.DeserializationClass(uuid, fieldName);

            }

            if (currentClass == null)
                throw new DeserializeException("Syntax Error: No class started");

            if (line.startsWith("::DAT")) {
                DeserializationType type;
                String[] args = line.split(" ");
                if (!line.contains("<")) {
                    type = DeserializationType.valueOf(
                            DirectTypes.fromPrefix(args[1].replace("[", "").replace("]", ""))
                                    .toString());

                    ArrayList<String> argsList = new ArrayList<>(Arrays.stream(args).toList());
                    argsList.remove(0);
                    argsList.remove(0);

                    StringBuilder builder = new StringBuilder();
                    argsList.forEach(builder::append);

                    String name = builder.toString().split("=")[0];
                    Object value = null;
                    try {
                        if (type != DeserializationType.STRING) {
                            value = MethodType.methodType(type.clazz).wrap().returnType().getDeclaredMethod("valueOf", String.class).invoke(null, builder.toString().split("=")[1]);
                        } else {
                            value = MethodType.methodType(type.clazz).wrap().returnType().getDeclaredMethod("valueOf", Object.class).invoke(null, builder.toString().split("=")[1]);

                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                        throw new DeserializeException("Field valueOf not found for " + type.clazz.getName());
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new DeserializeException(e);
                    }

                    currentClass.getVariables().put(name, new DeserializationData.DeserializationObject(type, name, value));
                } else {
                    type = DeserializationType.REFERENCE;

                    ArrayList<String> argsList = new ArrayList<>(Arrays.stream(args).toList());
                    argsList.remove(0);
                    argsList.remove(0);

                    StringBuilder builder = new StringBuilder();
                    argsList.forEach(builder::append);

                    String name = builder.toString().split("=")[0];
                    String refSignature = builder.toString().split("=")[1];

                    System.out.println(refSignature);

                    if (refSignature.equalsIgnoreCase("<NULL>")) {
                        currentClass.getVariables().put(name, new DeserializationData.DeserializationObject(type, name, null));
                        continue;
                    }
                    String refName = refSignature.split("#")[0];
                    String refUuid = refSignature.split("#")[1];

                    DeserializationData.DeserializationClass finalCurrentClass = currentClass;
                    new Thread(() -> {
                        while (!deserializationData.getClasses().containsKey(refUuid)) {
                        }

                        finalCurrentClass.getVariables().put(name, new DeserializationData.DeserializationObject(type, name, deserializationData.getClasses().get(refUuid)));
                    }).start();


                }
            }

            if (line.startsWith("::END CLASS")) {
                deserializationData.getClasses().put(currentClass.getUuid(), currentClass);
                currentClass = null;
            }
        }

        return deserializationData;
    }

}
