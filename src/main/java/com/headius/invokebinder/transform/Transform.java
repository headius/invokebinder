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
 * Abstract reprensentation of some handle transform.
 */
public abstract class Transform {
    /**
     * Apply this transform upward from the given MethodHandle, producing
     * a new handle.
     *
     * @param target the target handle
     * @return the adapted handle
     */
    public abstract MethodHandle up(MethodHandle target);

    /**
     * Apply this transform downward from an incoming MethodType, producing
     * a new type.
     *
     * @param source the source type
     * @return the new type
     */
    public abstract MethodType down(MethodType source);

    /**
     * Return a string representation of this transform.
     *
     * @return a string representation of this transform
     */
    public abstract String toString();

    /**
     * Return a Java code representation of this transform.
     *
     * @return a Java code representation of this transform.
     */
    public abstract String toJava(MethodType incoming);

    /**
     * Build a list of argument type classes suitable for inserting into Java code.
     *
     * This will be an argument list of the form "pkg.Cls1.class, pkg.Cls2[].class, primtype.class, ..."
     *
     * @param builder the builder in which to build the argument list
     * @param types the classes from which to create the argument list
     */
    protected static void buildClassArguments(StringBuilder builder, Class<?>[] types) {
        boolean second = false;
        for (Class cls : types) {
            if (second) builder.append(", ");
            second = true;
            buildClassArgument(builder, cls);
        }
    }

    /**
     * Build Java code to represent a single .class reference.
     *
     * This will be an argument of the form "pkg.Cls1.class" or "pkg.Cls2[].class" or "primtype.class"
     *
     * @param builder the builder in which to build the argument
     * @param cls the type for the argument
     */
    protected static void buildClassArgument(StringBuilder builder, Class cls) {
        buildClass(builder, cls);
        builder.append(".class");
    }

    /**
     * Build Java code to represent a cast to the given type.
     *
     * This will be an argument of the form "(pkg.Cls1)" or "(pkg.Cls2[])" or "(primtype)"
     *
     * @param builder the builder in which to build the argument
     * @param cls the type for the argument
     */
    protected static void buildClassCast(StringBuilder builder, Class cls) {
        builder.append('(');
        buildClass(builder, cls);
        builder.append(')');
    }

    /**
     * Build Java code to represent a literal primitive.
     *
     * This will append L or F as appropriate for long and float primitives.
     *
     * @param builder the builder in which to generate the code
     * @param value the primitive value to generate from
     */
    protected static void buildPrimitiveJava(StringBuilder builder, Object value) {
        builder.append(value.toString());
        if (value.getClass() == Float.class) builder.append('F');
        if (value.getClass() == Long.class) builder.append('L');
    }

    /**
     * Build Java code to represent a type reference to the given class.
     *
     * This will be of the form "pkg.Cls1" or "pkc.Cls2[]" or "primtype".
     *
     * @param builder the builder in which to build the type reference
     * @param cls the type for the reference
     */
    private static void buildClass(StringBuilder builder, Class cls) {
        int arrayDims = 0;
        Class tmp = cls;
        while (tmp.isArray()) {
            arrayDims++;
            tmp = tmp.getComponentType();
        }
        builder.append(tmp.getName());
        if (arrayDims > 0) {
            for (; arrayDims > 0 ; arrayDims--) {
                builder.append("[]");
            }
        }
    }

    /**
     * Build Java code appropriate for standing up the given MethodType.
     *
     * @param source the MethodType for which to build Java code
     * @return Java code suitable for building the given MethodType
     */
    public static String generateMethodType(MethodType source) {
        StringBuilder builder = new StringBuilder("MethodType.methodType(");
        buildClassArgument(builder, source.returnType());
        if (source.parameterCount() > 0) {
            builder.append(", ");
            buildClassArguments(builder, source.parameterArray());
        }
        builder.append(")");
        return builder.toString();
    }
}
