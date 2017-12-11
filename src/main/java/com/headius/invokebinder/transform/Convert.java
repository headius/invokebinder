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

/**
 * An argument conversion transform.
 *
 * Equivalent call: MethodHandles.asType(MethodHandle, MethodType).
 */
public class Convert extends Transform {

    private final MethodType type;

    public Convert(MethodType type) {
        this.type = type;
    }

    public MethodHandle up(MethodHandle target) {
        // If target's return type is void, it is replaced with (Object)null.
        // If incoming signature expects something else, additional cast is required.
        // TODO: Is this a bug in JDK?
        if (target.type().returnType() == void.class) {
            return MethodHandles.explicitCastArguments(target.asType(type), type);
        } else {
            return target.asType(type);
        }
    }

    public MethodType down(MethodType type) {
        for (int i = 0; i < type.parameterCount(); i++) {
            type = type.changeParameterType(i, type.parameterArray()[i]);
        }
        return type;
    }

    public String toString() {
        return "convert args to " + type;
    }

    public String toJava(MethodType incoming) {
        String methodTypeJava = generateMethodType(type);

        if (incoming.returnType() == void.class) {
            return "handle = MethodHandles.explicitCastArguments(handle.asType(" + methodTypeJava + ", " + methodTypeJava + ");";
        }

        return "handle = handle.asType(" + methodTypeJava + ");";
    }
}
