package com.headius.invoke.binder.transform;

import com.headius.invoke.binder.InvalidTransformException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An array-spreading transform.
 *
 * Equivalent call: MethodHandles.spreadInvoker(MethodType, int).bindTo(MethodHandle)
*/
public class Spread extends Transform {

    private final MethodType source;
    private final Class[] spreadTypes;

    public Spread(MethodType source, Class... spreadTypes) {
        this.source = source;
        this.spreadTypes = spreadTypes;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles
                .spreadInvoker(target.type(), target.type().parameterCount() - spreadTypes.length)
                .bindTo(target);
    }

    public MethodType down(MethodType type) {
        int last = source.parameterCount() - 1;
        if (source.parameterArray()[last] != Object[].class) {
            throw new InvalidTransformException("trailing argument is not Object[]: " + source);
        }

        type = type.dropParameterTypes(last, last + 1);
        return type.appendParameterTypes(spreadTypes);
    }

    public String toString() {
        return "spread " + source + " to " + down(source);
    }
}
