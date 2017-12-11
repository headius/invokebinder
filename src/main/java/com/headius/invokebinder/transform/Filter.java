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

    public static final String FILTER_FUNCTIONS_JAVA = "<filter functions>";

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

    public String toJava(MethodType incoming) {
        return "handle = MethodHandles.filterArguments(handle, " + index + ", " + FILTER_FUNCTIONS_JAVA + ");";
    }
}
