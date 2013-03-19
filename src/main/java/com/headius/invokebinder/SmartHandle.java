/*
 * Copyright 2013 headius.
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
 *         .returning(void.class)
 *         .appendArg("args", Object[].class);
 * 
 * // The actual target signature for arg count checking, with min and max ints
 * public static final Signature ARG_COUNT_CHECK_SIGNATURE = Signature
 *         .returning(int.class)
 *         .appendArg("args", Object[].class)
 *         .appendArg("min", int.class)
 *         .appendArg("max", int.class);
 *
 * // A SmartHandle for the arity-checking method, using the fold and signature
 * // from above and inserting 1, 3 for min, max
 * SmartHandle arityCheck = SmartBinder
 *         .from(ARITY_CHECK_FOLD)
 *         .append("min", 1)
 *         .append("max", 3)
 *         .cast(ARITY_CHECK_SIGNATURE)
 *         .invokeStaticQuiet(LOOKUP, ArgCountChecker.class, "checkArgumentCount");
 *
 * // The variable-arity call contaings other arguments plus the Object[] args.
 * // Here, we can just fold with our arityCheck SmartHandle, which drops args
 * // we are not interested in, passes along the args array, and ignores the
 * // return value.
 * variableCall = SmartBinder
 *         .from(VARIABLE_ARITY_SIGNATURE)
 *         .foldVoid(arityCheck)
 *         .invoke(directCall);
 * </code>
 * 
 * @author headius
 */
public class SmartHandle {
    /** The signature associated with this smart handle */
    private final Signature signature;
    /** The MethodHandle associated with this smart handle */
    private final MethodHandle handle;

    SmartHandle(Signature signature, MethodHandle handle) {
        this.signature = signature;
        this.handle = handle;
    }
    
    /**
     * Create a new SmartHandle from the given Signature and MethodHandle.
     * 
     * @param signature the signature for the new smart handle
     * @param handle the method handle for the new smart handle
     * @return a new SmartHandle
     */
    public static SmartHandle from(Signature signature, MethodHandle handle) {
        return new SmartHandle(signature, handle);
    }

    /**
     * Create a new SmartHandle by performing a lookup on the given target class
     * for the given method name with the given signature.
     * 
     * @param lookup the MethodHandles.Lookup object to use
     * @param target the class where the method is located
     * @param name the name of the method
     * @param signature the signature of the method
     * @return a new SmartHandle based on the signature and looked-up MethodHandle
     */
    public static SmartHandle findStaticQuiet(Lookup lookup, Class target, String name, Signature signature) {
        try {
            return new SmartHandle(signature, lookup.findStatic(target, name, signature.type()));
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
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
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, Object arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, boolean arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, byte arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, short arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, char arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, int arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, long arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, float arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(int index, double arg) {
        return new SmartHandle(signature.dropArg(index), MethodHandles.insertArguments(handle, index, arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, Object arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, boolean arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, byte arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, short arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, char arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, int arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, long arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, float arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle insert(String name, double arg) {
        return new SmartHandle(signature.dropArg(name), MethodHandles.insertArguments(handle, signature.argOffset(name), arg));
    }
        
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(Object arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(boolean arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(byte arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(short arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(char arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(int arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(long arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(float arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Append an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle append(double arg) {
        return new SmartHandle(signature.dropLast(), MethodHandles.insertArguments(handle, signature.type().parameterCount(), arg));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle drop(String beforeName, String newName, Class type) {
        return new SmartHandle(signature.insertArg(beforeName, newName, type), MethodHandles.dropArguments(handle, signature.argOffset(beforeName), type));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle drop(int index, String newName, Class type) {
        return new SmartHandle(signature.insertArg(index, newName, type), MethodHandles.dropArguments(handle, index, type));
    }
    
    /**
     * Insert an argument into the handle at the given index, returning a new
     * SmartHandle.
     * 
     * @param name the name of the argument in the new SmartHandle's Signature
     * @param arg the argument value
     * @return a new SmartHandle with the additional argument
     */
    public SmartHandle dropLast(String newName, Class type) {
        return new SmartHandle(signature.appendArg(newName, type), MethodHandles.dropArguments(handle, signature.argOffset(newName), type));
    }
    
    /**
     * Use this SmartHandle as a test to guard target and fallback handles.
     * 
     * @param target the "true" path for this handle's test
     * @param fallback the "false" path for this handle's test
     * @return a MethodHandle that performs the test and branch
     */
    public MethodHandle guard(MethodHandle target, MethodHandle fallback) {
        return MethodHandles.guardWithTest(handle, target, fallback);
    }
    
    /**
     * Use this SmartHandle as a test to guard target and fallback handles.
     * 
     * @param target the "true" path for this handle's test
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
    
}
