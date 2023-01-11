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

    public Car() {

    }
    public Car(String color, int maxSpeed) {
        this.color = color;
        this.maxSpeed = maxSpeed;

    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Person getPerson() {
        return person;
    }
}
