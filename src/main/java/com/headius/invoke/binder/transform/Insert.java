package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An argument insertion transform.
 *
 * Equivalent call: MethodHandles.insertArguments(MethodHandle, int, Object...).
*/
public class Insert extends Transform {

    private final int position;
    private final Object[] value;

    public Insert(int position, Object... value) {
        this.position = position;
        this.value = value;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.insertArguments(target, position, value);
    }

    public MethodType down(MethodType type) {
        return type.insertParameterTypes(position, value.getClass());
    }

    public String toString() {
        return "insert " + value.getClass() + " at " + position;
    }
}
