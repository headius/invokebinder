package com.headius.invokebinder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * An argument-boxing transform.
 *
 * Equivalent call: MethodHandle.asVarargsCollector(Class)
*/
public class Varargs extends Transform {

    private final MethodType source;
    private int index;
    private final Class arrayType;

    public Varargs(MethodType source, int index, Class arrayType) {
        this.source = source;
        this.index = index;
        this.arrayType = arrayType;
    }

    public MethodHandle up(MethodHandle target) {
        return target.asVarargsCollector(arrayType).asType(source);
    }

    public MethodType down(MethodType type) {
        assertTypesAreCompatible();

        return type
                .dropParameterTypes(index, source.parameterCount())
                .appendParameterTypes(arrayType);
    }

    private void assertTypesAreCompatible() {
        Class componentType = arrayType.getComponentType();
        for (int i = index; i < source.parameterCount(); i++) {
            Class in = source.parameterType(i);
            assert componentType.isAssignableFrom(in)
                    : "incoming type " + in.getName() + " not compatible with " + componentType.getName() + "[]";
        }
    }

    public String toString() {
        return "varargs at " + index + " into " + arrayType.getName();
    }
}
