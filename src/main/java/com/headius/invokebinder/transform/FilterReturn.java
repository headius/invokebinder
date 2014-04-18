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

/**
 * An return-filtering transform.
 * <p/>
 * Equivalent call: MethodHandles.filterReturn(MethodHandle, MethodHandle).
 */
public class FilterReturn extends Transform {

    private final MethodHandle function;

    public FilterReturn(MethodHandle function) {
        this.function = function;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.filterReturnValue(target, function);
    }

    public MethodType down(MethodType type) {
        int count = function.type().parameterCount();
        switch (count) {
            case 0:
                return type.changeReturnType(void.class);
            case 1:
                return type.changeReturnType(function.type().parameterType(0));
            default:
                throw new InvalidTransformException("return filter " + function + " does not accept zero or one argument");
        }
    }

    public String toString() {
        return "filter return with " + function;
    }
}
