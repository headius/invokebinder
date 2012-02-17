package com.headius.invokebinder;

/**
 * Exception raised when a Binder transform is not valid for the current
 * signature.
 */
public class InvalidTransformException extends RuntimeException {
    public InvalidTransformException(String message) {
        super(message);
    }

    public InvalidTransformException(Exception e) {
        super(e);
    }
}
