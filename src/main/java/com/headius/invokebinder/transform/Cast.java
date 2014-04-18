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
 * A casting transform.
 * <p/>
 * Equivalent call: MethodHandles.explicitCastArguments(MethodHandle, MethodType).
 */
public class Cast extends Transform {

    private final MethodType type;

    public Cast(MethodType type) {
        this.type = type;
    }

    public MethodHandle up(MethodHandle target) {
        // If target's return type is void, it is replaced with (Object)null.
        // If incoming signature expects something else, additional cast is required.
        // TODO: Is this a bug in JDK?
        if (target.type().returnType() == void.class) {
            target = MethodHandles.explicitCastArguments(target, type);
            return MethodHandles.explicitCastArguments(target, type);
        } else {
            return MethodHandles.explicitCastArguments(target, type);
        }
    }

    public MethodType down(MethodType type) {
        for (int i = 0; i < type.parameterCount(); i++) {
            type = type.changeParameterType(i, type.parameterArray()[i]);
        }
        return type;
    }

    public String toString() {
        return "cast args to " + type;
    }
}
