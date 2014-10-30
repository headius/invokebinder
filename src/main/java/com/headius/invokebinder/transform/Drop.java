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
        return type.dropParameterTypes(position, position + types.length);
    }

    public String toString() {
        return "drop " + Arrays.toString(types) + " at " + position;
    }
}
