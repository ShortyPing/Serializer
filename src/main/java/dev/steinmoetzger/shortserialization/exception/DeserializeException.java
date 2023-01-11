/*
Copyright (c) 2015 - 2023 Michael Steinmötzger
All rights are reserved for this project, unless otherwise
stated in a license file.
*/

package dev.steinmoetzger.shortserialization.exception;

public class DeserializeException extends Exception{

    public DeserializeException() {
    }

    public DeserializeException(String message) {
        super(message);
    }

    public DeserializeException(Throwable cause) {
        super(cause);
    }
}
