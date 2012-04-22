package com.headius.invokebinder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * An argument insertion transform.
 *
 * Equivalent call: MethodHandles.insertArguments(MethodHandle, int, Object...).
*/
public class Insert extends Transform {

    private final int position;
    private final Class[] types;
    private final Object[] values;

    public Insert(int position, Object... values) {
        this.position = position;
        this.values = values;
        Class[] types = new Class[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i].getClass();
        }
        this.types = types;
    }

    public Insert(int position, Class[] types, Object... values) {
        this.position = position;
        this.values = values;
        this.types = types;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.insertArguments(target, position, values);
    }

    public MethodType down(MethodType type) {
        return type.insertParameterTypes(position, types);
    }

    public String toString() {
        return "insert " + Arrays.toString(types()) + " at " + position;
    }

    private Class[] types() {
        Class[] types = new Class[values.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = values[i].getClass();
        }
        return types;
    }
}
