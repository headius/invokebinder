package com.headius.invokebinder.transform;

import com.headius.invokebinder.InvalidTransformException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * A permutation transform.
 *
 * Equivalent call: MethodHandles.permuteArguments(MethodHandle, MethodType, int...)
*/
public class Permute extends Transform {

    private final MethodType source;
    private final int[] reorder;

    public Permute(MethodType source, int... reorder) {
        this.source = source;
        this.reorder = reorder;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.permuteArguments(target, source, reorder);
    }

    public MethodType down(MethodType type) {
        Class[] types = new Class[reorder.length];
        for (int i = 0; i < reorder.length; i++) {
            int typeIndex = reorder[i];
            if (typeIndex < 0 || typeIndex >= type.parameterCount()) {
                throw new InvalidTransformException("one or more permute indices (" + Arrays.toString(reorder) + ") out of bounds for " + source);
            }

            types[i] = type.parameterType(reorder[i]);
        }
        return MethodType.methodType(type.returnType(), types);
    }

    public String toString() {
        return "permute " + source + " with " + Arrays.toString(reorder);
    }
}
