/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization;

import dev.steinmoetzger.shortserialization.annotation.SerializableClass;
import dev.steinmoetzger.shortserialization.annotation.SerializableField;

@SerializableClass(name = "Car")
public class Car {

    @SerializableField
    private String color;
    @SerializableField
    private int maxSpeed;
    @SerializableField
    private Person person;

    public Car(String color, int maxSpeed) {
        this.color = color;
        this.maxSpeed = maxSpeed;

    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
