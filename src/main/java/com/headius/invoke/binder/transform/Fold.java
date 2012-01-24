package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An argument-folding transform.
 * <p/>
 * Equivalent call: MethodHandles.foldArguments(MethodHandle, MethodHandle).
 */
public class Fold extends Transform {

    private final MethodHandle function;

    public Fold(MethodHandle function) {
        this.function = function;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.foldArguments(target, function);
    }

    public MethodType down(MethodType type) {
        if (function.type().returnType() == void.class) return type;
        return type.insertParameterTypes(0, function.type().returnType());
    }

    public String toString() {
        return "fold args with " + function;
    }
}
