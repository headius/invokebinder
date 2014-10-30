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
 * An argument-folding transform.
 *
 * Equivalent call: MethodHandles.foldArguments(MethodHandle, MethodHandle).
 */
public class Fold extends Transform {

    private final MethodHandle function;

    public Fold(MethodHandle function) {
        this.function = function;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.foldArguments(target, function);
    }

    public MethodType down(MethodType type) {
        if (function.type().returnType() == void.class) return type;
        return type.insertParameterTypes(0, function.type().returnType());
    }

    public String toString() {
        return "fold args with " + function;
    }
}
