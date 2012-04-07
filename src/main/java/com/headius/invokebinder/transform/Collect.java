package com.headius.invokebinder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * An argument-boxing transform with a fixed incoming size.
 *
 * Equivalent call: MethodHandle.asCollector(Class, int)
*/
public class Collect extends Transform {

    private final MethodType source;
    private int index;
    private final Class arrayType;

    public Collect(MethodType source, int index, Class arrayType) {
        this.source = source;
        this.index = index;
        this.arrayType = arrayType;
    }

    public MethodHandle up(MethodHandle target) {
        return target.asCollector(arrayType, source.parameterCount() - index);
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
            assert in.isAssignableFrom(componentType)
                    : "incoming type " + in.getName() + " not compatible with " + componentType.getName() + "[]";
        }
    }

    public String toString() {
        return "collect at " + index + " into " + arrayType.getName();
    }
}
