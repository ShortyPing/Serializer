/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization;

import dev.steinmoetzger.shortserialization.annotation.SerializableClass;
import dev.steinmoetzger.shortserialization.annotation.SerializableField;

@SerializableClass(name = "Person", delim = ';')
public class Person {

    @SerializableField()
    public String name;
    @SerializableField()
    private float age;

    @SerializableField
    private Car car;

    public Person(String name, int age, Car car) {
        this.name = name;
        this.age = age;
        this.car = car;
    }

    public void setCar(Car car) {
        this.car = car;
    }
}
