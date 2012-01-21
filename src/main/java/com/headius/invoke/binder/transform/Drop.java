package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * An argument drop transform.
 *
 * Equivalent call: MethodHandles.dropArguments(MethodHandle, int, MethodType).
*/
public class Drop extends Transform {

    private final int position;
    private final Class[] types;

    public Drop(int position, Class... types) {
        this.position = position;
        this.types = types;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.dropArguments(target, position, types);
    }

    public MethodType down(MethodType type) {
        return type.dropParameterTypes(position, types.length);
    }

    public String toString() {
        return "drop " + Arrays.toString(types) + " at " + position;
    }
}
