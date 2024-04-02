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

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A filter that takes multiple arguments and replaces them with zero or one argument of a new type.
 *
 * Equivalent call: MethodHandle.asCollector(Class, int) or MethodHandles.collectArguments
 */
public class Collect extends Transform {

    private final MethodType source;
    private final int index;
    private final int count;
    private final Class<?> resultType;
    private final MethodHandle collector;

    public Collect(MethodType source, int index, Class<?> resultType) {
        this.source = source;
        this.index = index;
        this.count = source.parameterCount() - index;
        this.resultType = resultType;
        this.collector = null;
    }

    public Collect(MethodType source, int index, Class<?> resultType, MethodHandle collector) {
        this.source = source;
        this.index = index;
        this.count = source.parameterCount() - index;
        this.resultType = resultType;
        this.collector = collector;
    }

    public Collect(MethodType source, int index, int count, Class<?> resultType) {
        this.source = source;
        this.index = index;
        this.count = count;
        this.resultType = resultType;
        this.collector = null;
    }

    public Collect(MethodType source, int index, int count, Class<?> resultType, MethodHandle collector) {
        this.source = source;
        this.index = index;
        this.count = count;
        this.resultType = resultType;
        this.collector = collector;
    }

    public MethodHandle up(MethodHandle target) {
        if (collector == null) {
            if (Util.isJava9()) {
                // Java 9 can collect a subset of non-tail arguments
                return target.asCollector(index, resultType, count);
            } else {
                if (onlyTail()) {
                    // tail arguments can be array-collected on all Java versions
                    return target.asCollector(resultType, count);
                } else {
                    // non-tail arguments must be permuted prior to Java 9
                    Permutes permutes = buildPermutes(source, target.type());

                    Binder binder = preparePermuteBinder(permutes);
                    return binder.invoke(target);
                }
            }
        } else {
            // custom collector always collects only as many args as it accepts
            return MethodHandles.collectArguments(target, index, collector);
        }
    }

    private Binder preparePermuteBinder(Permutes permutes) {
        return Binder.from(source)
                .permute(permutes.movePermute)
                .collect(source.parameterCount() - count, resultType)
                .permute(permutes.moveBackPermute);
    }

    public MethodType down(MethodType type) {
        assertTypesAreCompatible();

        return type
                .dropParameterTypes(index, index + count)
                .insertParameterTypes(index, resultType);
    }

    private void assertTypesAreCompatible() {
        if (collector == null) {
            // default array collector
            assert resultType.isArray() : "no collector provided but target type is not array";
            Class<?> componentType = resultType.getComponentType();
            for (int i = index; i < index + count; i++) {
                Class<?> in = source.parameterType(i);
                assert in.isAssignableFrom(componentType)
                        : "incoming type " + in.getName() + " not compatible with " + componentType.getName() + "[]";
            }
        } else {
            for (int i = 0; i < count; i++) {
                Class<?> in = source.parameterType(index + i);
                Class<?> out = collector.type().parameterType(i);
                assert in.isAssignableFrom(out) : "incoming type " + in.getName() + " not compatible with " + out;
            }
            assert collector.type().returnType().isAssignableFrom(resultType);
        }
    }

    public String toString() {
        return "collect at " + index + " into " + resultType.getName();
    }

    public String toJava(MethodType incoming) {
        StringBuilder builder = new StringBuilder();
        if (onlyTail()) {
            if (collector == null) {
                builder.append("handle = handle.asCollector(");
                buildClassArgument(builder, resultType);
                builder
                        .append(", ")
                        .append(count)
                        .append(");");
            } else {
                builder.append("handle = MethodHandles.collectArguments(");

                builder
                        .append("handle, ")
                        .append(count)
                        .append(", ");

                buildClassArgument(builder, resultType);

                builder.append(");");
            }
        } else {
            Permutes permutes = buildPermutes(source, incoming);

            Binder binder = preparePermuteBinder(permutes);
            return binder.toJava(incoming);
        }
        return builder.toString();
    }

    private boolean onlyTail() {
        return index + count == source.parameterCount();
    }

    private static class Permutes {
        private final int[] movePermute;
        private final int[] moveBackPermute;

        private Permutes(MethodType source, MethodType target, int index, int count) {
            movePermute = new int[source.parameterCount()];
            moveBackPermute = new int[target.parameterCount()];
            // pre
            for (int i = 0; i < index; i++) {
                movePermute[i] = i;
                moveBackPermute[i] = i;
            }

            // post
            int shifted = 0;
            for (int i = index; i + count < movePermute.length; i++, shifted++) movePermute[i] = i + count;
            for (int i = index; i + 1 < moveBackPermute.length; i++) moveBackPermute[i + 1] = i;

            // collected args
            for (int i = index + shifted; i < movePermute.length; i++) movePermute[i] = i - shifted;
            moveBackPermute[index] = moveBackPermute.length - 1;
        }
    }

    private Permutes buildPermutes(MethodType source, MethodType target) {
        return new Permutes(source, target, index, count);
    }
}
