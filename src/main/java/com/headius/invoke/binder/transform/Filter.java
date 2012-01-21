package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * An argument-filtering transform.
 *
 * Equivalent call: MethodHandles.filterArguments(MethodHandle, int, MethodHandle...).
 */
public class Filter extends Transform {

    private final int index;
    private final MethodHandle[] functions;

    public Filter(int index, MethodHandle... functions) {
        this.index = index;
        this.functions = functions;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.filterArguments(target, index, functions);
    }

    public MethodType down(MethodType type) {
        for (int i = 0; i < functions.length; i++) {
            type = type.changeParameterType(index + i, functions[i].type().returnType());
        }
        return type;
    }

    public String toString() {
        return "fold args from " + index + " with " + Arrays.toString(functions);
    }
}
