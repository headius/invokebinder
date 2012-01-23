package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An exception-handling transform.
 * <p/>
 * Equivalent call: MethodHandles.catchException(MethodHandle, Class, MethodHandle).
 */
public class Catch extends Transform {

    private final Class throwable;
    private final MethodHandle function;

    public Catch(Class throwable, MethodHandle function) {
        this.throwable = throwable;
        this.function = function;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.catchException(target, throwable, function);
    }

    public MethodType down(MethodType type) {
        return type;
    }

    public String toString() {
        return "catch exception type " + throwable + " using " + function;
    }
}
