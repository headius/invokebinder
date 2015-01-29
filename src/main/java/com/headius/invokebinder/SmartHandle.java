/*
 * Copyright 2013-2014 headius.
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
package com.headius.invokebinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * A tuple of a Signature and a java.lang.invoke.MethodHandle, providing
 * features of both plus a number of MethodHandles.* methods in a simpler form.
 *
 * SmartHandle is provided as a way to couple a given MethodHandle to a
 * Signature, allowing future adaptation of the MethodHandle to proceed using
 * Signature's various shortcuts and conveniences.
 *
 * Example:
 *
 * <code>
 * // A signature that only wants the "context" and "args" arguments
 * public static final Signature ARG_COUNT_CHECK_FOLD = Signature
 * .returning(void.class)
 * .appendArg("args", Object[].class);
 *
 * // The actual target signature for arg count checking, with min and max ints
 * public static final Signature ARG_COUNT_CHECK_SIGNATURE = Signature
 * .returning(int.class)
 * .appendArg("args", Object[].class)
 * .appendArg("min", int.class)
 * .appendArg("max", int.class);
 *
 * // A SmartHandle for the arity-checking method, using the fold and signature
 * // from above and inserting 1, 3 for min, max
 * SmartHandle arityCheck = SmartBinder
 * .from(ARITY_CHECK_FOLD)
 * .append("min", 1)
 * .append("max", 3)
 * .cast(ARITY_CHECK_SIGNATURE)
 * .invokeStaticQuiet(LOOKUP, ArgCountChecker.class, "checkArgumentCount");
 *
 * // The variable-arity call contaings other arguments plus the Object[] args.
 * // Here, we can just fold with our arityCheck SmartHandle, which drops args
 * // we are not interested in, passes along the args array, and ignores the
 * // return value.
 * variableCall = SmartBinder
 * .from(VARIABLE_ARITY_SIGNATURE)
 * .foldVoid(arityCheck)
 * .invoke(directCall);
 * </code>
 *
 * @author headius
 */
public class SmartHandle {
    /**
     * The signature associated with this smart handle
     */
    private final Signature signature;
    /**
     * The MethodHandle associated with this smart handle
     */
    private final MethodHandle handle;

    SmartHandle(Signature signature, MethodHandle handle) {
        this.signature = signature;
        this.handle = handle;
    }

    /**
     * Create a new SmartHandle from the given Signature and MethodHandle.
     *
     * @param signature the signature for the new smart handle
     * @param handle    the method handle for the new smart handle
     * @return a new SmartHandle
     */
    public static SmartHandle from(Signature signature, MethodHandle handle) {
        return new SmartHandle(signature, handle);
    }

    /**
     * Create a new SmartHandle by performing a lookup on the given target class
     * for the given method name with the given signature.
     *
     * @param lookup    the MethodHandles.Lookup object to use
     * @param target    the class where the method is located
     * @param name      the name of the method
     * @param signature the signature of the method
     * @return a new SmartHandle based on the signature and looked-up MethodHandle
     */
    public static SmartHandle findStaticQuiet(Lookup lookup, Class<?> target, String name, Signature signature) {
        try {
            return new SmartHandle(signature, lookup.findStatic(target, name, signature.type()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the Signature of this SmartHandle.
     *
     * @return the Signature of this SmartHandle
     */
    public Signature signature() {
        return signature;
    }

    /**
     * Get the MethodHandle of this SmartHandle.
     *
     * @return the MethodHandle of this SmartHandle
     */
    public MethodHandle handle() {
        return handle;
    }

    /**
     * Apply an argument into the handle at the given index, returning a new
     * SmartHandle. The new handle will use the given value for the argument at
     * the given index, accepting one fewer argument as a result. In other words,
     * fix that argument (partial application) into the given handle.
     *
     * @param index the index of the argument in the new SmartHandle's Signature
     * @param arg  the argument value
     * @return a new SmartHandle that already has applied the given argument
     */
    public SmartHandle apply(int index, Object arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }

    /**
     * Apply an argument into the handle at the given name, returning a new
     * SmartHandle. The new handle will use the given value for the argument at
     * the given index, accepting one fewer argument as a result. In other words,
     * fix that argument (partial application) into the given handle.
     *
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg  the argument value
     * @return a new SmartHandle that already has applied the given argument
     */
    public SmartHandle apply(String name, Object arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }

    /**
     * Apply an argument into the handle at the end, returning a new
     * SmartHandle. The new handle will use the given value for the last
     * argument, accepting one fewer argument as a result. In other words,
     * fix that argument (partial application) into the given handle.
     *
     * @param arg  the argument value
     * @return a new SmartHandle that already has applied the given argument
     */
    public SmartHandle applyLast(Object arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }

    /**
     * Drop an argument name and type from the handle at the given index, returning a new
     * SmartHandle.
     *
     * @param beforeName name before which the dropped argument goes
     * @param newName name of the argument
     * @param type type of the argument
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle drop(String beforeName, String newName, Class<?> type) {
        return new SmartHandle(signature.insertArg(beforeName, newName, type), MethodHandles.dropArguments(handle, signature.argOffset(beforeName), type));
    }

    /**
     * Drop an argument from the handle at the given index, returning a new
     * SmartHandle.
     *
     * @param index index before which the dropped argument goes
     * @param newName name of the argument
     * @param type type of the argument
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle drop(int index, String newName, Class<?> type) {
        return new SmartHandle(signature.insertArg(index, newName, type), MethodHandles.dropArguments(handle, index, type));
    }

    /**
     * Drop an argument from the handle at the end, returning a new
     * SmartHandle.
     *
     * @param newName name of the argument
     * @param type type of the argument
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle dropLast(String newName, Class<?> type) {
        return new SmartHandle(signature.appendArg(newName, type), MethodHandles.dropArguments(handle, signature.argOffset(newName), type));
    }

    /**
     * Use this SmartHandle as a test to guard target and fallback handles.
     *
     * @param target   the "true" path for this handle's test
     * @param fallback the "false" path for this handle's test
     * @return a MethodHandle that performs the test and branch
     */
    public MethodHandle guard(MethodHandle target, MethodHandle fallback) {
        return MethodHandles.guardWithTest(handle, target, fallback);
    }

    /**
     * Use this SmartHandle as a test to guard target and fallback handles.
     *
     * @param target   the "true" path for this handle's test
     * @param fallback the "false" path for this handle's test
     * @return a new SmartHandle that performs the test and branch
     */
    public SmartHandle guard(SmartHandle target, SmartHandle fallback) {
        return new SmartHandle(target.signature, MethodHandles.guardWithTest(handle, target.handle, fallback.handle));
    }

    /**
     * Bind the first argument of this SmartHandle to the given object,
     * returning a new adapted handle.
     *
     * @param obj the object to which to bind this handle's first argument
     * @return a new SmartHandle with the first argument dropped in favor of obj
     */
    public SmartHandle bindTo(Object obj) {
        return new SmartHandle(signature.dropFirst(), handle.bindTo(obj));
    }

    /**
     * Create a new SmartHandle that converts arguments from the given type to
     * the current signature's type, using the same argument names. This conversion
     * is equivalent to MethodHandle#asType.
     *
     * @param incoming the target MethodType from which arguments will be converted
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle convert(MethodType incoming) {
        return new SmartHandle(new Signature(incoming, signature.argNames()), handle.asType(incoming));
    }

    /**
     * Create a new SmartHandle that converts arguments from the given return
     * type and argument types to the current signature's type, using the same
     * argument names. This conversion is equivalent to MethodHandle#asType.
     *
     * @param returnType the return type of the new handle
     * @param argTypes   the argument types of the new handle
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle convert(Class<?> returnType, Class<?>... argTypes) {
        return convert(MethodType.methodType(returnType, argTypes));
    }

    /**
     * Create a new SmartHandle that converts arguments from the given signature to
     * the current signature's type with the new argument names. This conversion
     * is equivalent to MethodHandle#asType.
     *
     * @param incoming the target MethodType from which arguments will be converted
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle convert(Signature incoming) {
        return new SmartHandle(incoming, handle.asType(incoming.type()));
    }

    /**
     * Create a new SmartHandle that casts arguments from the given type to
     * the current signature's type, using the same argument names. This casting
     * is equivalent to MethodHandles#explicitCastArguments.
     *
     * @param incoming the target MethodType from which arguments will be converted
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle cast(MethodType incoming) {
        return new SmartHandle(new Signature(incoming, signature.argNames()), MethodHandles.explicitCastArguments(handle, incoming));
    }

    /**
     * Create a new SmartHandle that casts arguments from the given signature to
     * the current signature's type with the new argument names. This casting
     * is equivalent to MethodHandle#asType.
     *
     * @param incoming the target MethodType from which arguments will be converted
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle cast(Signature incoming) {
        return new SmartHandle(incoming, MethodHandles.explicitCastArguments(handle, incoming.type()));
    }

    /**
     * Create a new SmartHandle that casts arguments from the given return
     * type and argument types to the current signature's type, using the same
     * argument names. This casting is equivalent to MethodHandle#asType.
     *
     * @param returnType the return type of the new handle
     * @param argTypes   the argument types of the new handle
     * @return a new SmartHandle that accepts the given argument types
     */
    public SmartHandle cast(Class<?> returnType, Class<?>... argTypes) {
        return cast(MethodType.methodType(returnType, argTypes));
    }

    /**
     * Replace the return value with the given value, performing no other
     * processing of the original value.
     *
     * @param type the type for the new return value
     * @param value the new value to return
     * @return a new SmartHandle that returns the given value
     */
    public SmartHandle returnValue(Class<?> type, Object value) {
        return new SmartHandle(signature.changeReturn(type), MethodHandles.filterReturnValue(handle, MethodHandles.constant(type, value)));
    }

    /**
     * A human-readable String representation of this SamrtHandle.
     *
     * @return a String representation of this handle
     */
    @Override
    public String toString() {
        return signature.toString() + "=>" + handle;
    }

}
