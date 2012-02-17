package com.headius.invokebinder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An argument conversion transform.
 *
 * Equivalent call: MethodHandles.asType(MethodHandle, MethodType).
*/
public class Convert extends Transform {

    private final MethodType type;

    public Convert(MethodType type) {
        this.type = type;
    }

    public MethodHandle up(MethodHandle target) {
        // If target's return type is void, it is replaced with (Object)null.
        // If incoming signature expects something else, additional cast is required.
        // TODO: Is this a bug in JDK?
        if (target.type().returnType() == void.class) {
            return MethodHandles.explicitCastArguments(target.asType(type), type);
        } else {
            return target.asType(type);
        }
    }

    public MethodType down(MethodType type) {
        for (int i = 0; i < type.parameterCount(); i++) {
            type = type.changeParameterType(i, type.parameterArray()[i]);
        }
        return type;
    }

    public String toString() {
        return "convert args to " + type;
    }
}
