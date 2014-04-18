/*
 * Copyright 2012-2014 headius.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.headius.invokebinder.transform;

import com.headius.invokebinder.InvalidTransformException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * A permutation transform.
 * <p/>
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
