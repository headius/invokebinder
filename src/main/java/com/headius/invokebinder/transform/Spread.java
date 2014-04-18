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
import java.lang.invoke.MethodType;

/**
 * An array-spreading transform.
 * <p/>
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
        return target.asSpreader(source.parameterType(source.parameterCount() - 1), spreadTypes.length);
    }

    public MethodType down(MethodType type) {
        int last = source.parameterCount() - 1;
        if (!source.parameterArray()[last].isArray()) {
            throw new InvalidTransformException("trailing argument is not []: " + source);
        }

        type = type.dropParameterTypes(last, last + 1);
        return type.appendParameterTypes(spreadTypes);
    }

    public String toString() {
        return "spread " + source + " to " + down(source);
    }
}
