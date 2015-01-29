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
import java.lang.invoke.MethodType;

/**
 * An argument-boxing transform.
 *
 * Equivalent call: MethodHandle.asVarargsCollector(Class)
 */
public class Varargs extends Transform {

    private final MethodType source;
    private int index;
    private final Class<?> arrayType;

    public Varargs(MethodType source, int index, Class<?> arrayType) {
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
        Class<?> componentType = arrayType.getComponentType();
        for (int i = index; i < source.parameterCount(); i++) {
            Class<?> in = source.parameterType(i);
            assert in.isAssignableFrom(componentType)
                    : "incoming type " + in.getName() + " not compatible with " + componentType.getName() + "[]";
        }
    }

    public String toString() {
        return "varargs at " + index + " into " + arrayType.getName();
    }
}
