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

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Maintains both a Binder, for building a series of transformations, and a
 * current Signature that maps symbolic names to arguments. Transformations
 * normally performed with Binder using argument indices can be done instead
 * using argument names and wildcards.
 *
 * TODO: Examples, or links to wiki examples.
 *
 * @author headius
 */
public class SmartBinder {
    private final Signature start;
    private final List<Signature> signatures = new ArrayList<>();
    private final Binder binder;

    private SmartBinder(Signature start, Binder binder) {
        this.start = start;
        this.signatures.add(start);
        this.binder = binder;
    }

    private SmartBinder(SmartBinder original, Signature next, Binder binder) {
        this.start = original.start;
        this.signatures.add(0, next);
        this.signatures.addAll(original.signatures);
        this.binder = binder;
    }

    /**
     * Get the current Signature for this binder.
     *
     * @return this binder's current Signature
     */
    public Signature signature() {
        return signatures.get(0);
    }

    /**
     * Get the signature this binder started with.
     *
     * @return the signature this binder started with.
     */
    public Signature baseSignature() {
        return signatures.get(signatures.size() - 1);
    }

    /**
     * Get the Binder instance associated with this SmartBinder.
     *
     * @return this SmartBinder's Binder instance
     */
    public Binder binder() {
        return binder;
    }

    /**
     * Create a new SmartBinder from the given Signature.
     *
     * @param inbound the Signature to start from
     * @return a new SmartBinder
     */
    public static SmartBinder from(Signature inbound) {
        return new SmartBinder(inbound, Binder.from(inbound.type()));
    }

    /**
     * Create a new SmartBinder from the given types and argument names.
     *
     * @param retType the type of the return value to start with
     * @param names   the names of arguments
     * @param types   the argument types
     * @return a new SmartBinder
     */
    public static SmartBinder from(Class<?> retType, String[] names, Class<?>... types) {
        return from(Signature.returning(retType).appendArgs(names, types));
    }

    /**
     * Create a new SmartBinder with from the given types and argument name.
     *
     * @param retType the type of the return value to start with
     * @param name    the name of the sole argument
     * @param type    the sole argument's type
     * @return a new SmartBinder
     */
    public static SmartBinder from(Class<?> retType, String name, Class<?> type) {
        return from(Signature.returning(retType).appendArg(name, type));
    }

    /**
     * Create a new SmartBinder from the given Signature, using the given
     * Lookup for any handle lookups.
     *
     * @param lookup  the Lookup to use for handle lookups
     * @param inbound the Signature to start from
     * @return a new SmartBinder
     */
    public static SmartBinder from(Lookup lookup, Signature inbound) {
        return new SmartBinder(inbound, Binder.from(lookup, inbound.type()));
    }

    /**
     * Create a new SmartBinder from the given types and argument names,
     * using the given Lookup for any handle lookups.
     *
     * @param lookup  the Lookup to use for handle lookups
     * @param retType the type of the return value to start with
     * @param names   the names of arguments
     * @param types   the argument types
     * @return a new SmartBinder
     */
    public static SmartBinder from(Lookup lookup, Class<?> retType, String[] names, Class<?>... types) {
        return from(lookup, Signature.returning(retType).appendArgs(names, types));
    }

    /**
     * Create a new SmartBinder from the given types and argument name,
     * using the given Lookup for any handle lookups.
     *
     * @param lookup  the Lookup to use for handle lookups
     * @param retType the type of the return value to start with
     * @param name    the name of  the sole arguments
     * @param type    the sole argument's type
     * @return a new SmartBinder
     */
    public static SmartBinder from(Lookup lookup, Class<?> retType, String name, Class<?> type) {
        return from(lookup, Signature.returning(retType).appendArg(name, type));
    }

    ///////////////////////////////////////////////////////////////////////////
    // FOLDS, based on MethodHandles.foldArguments.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Pass all arguments to the given function and insert the resulting value
     * as newName into the argument list.
     *
     * @param newName  the name of the new first argument where the fold
     *                 function's result will be passed
     * @param function a function which will receive all arguments and have its
     *                 return value inserted into the call chain
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder fold(String newName, MethodHandle function) {
        return new SmartBinder(this, signature().prependArg(newName, function.type().returnType()), binder.fold(function));
    }

    /**
     * Pass all arguments to the given function and insert the resulting value
     * as newName into the argument list.
     *
     * @param newName  the name of the new first argument where the fold
     *                 function's result will be passed
     * @param function a function which will receive all arguments and have its
     *                 return value inserted into the call chain
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder fold(String newName, SmartHandle function) {
        if (Arrays.equals(signature().argNames(), function.signature().argNames())) {
            return fold(newName, function.handle());
        } else {
            return fold(newName, signature().changeReturn(function.signature().type().returnType()).permuteWith(function).handle());
        }
    }

    /**
     * Pass all arguments to the given function and drop any result.
     *
     * @param function a function which will receive all arguments and have its
     *                 return value inserted into the call chain
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldVoid(MethodHandle function) {
        return new SmartBinder(this, signature(), binder.foldVoid(function));
    }

    /**
     * Pass all arguments to the given function and drop any result.
     *
     * @param function a function which will receive all arguments and have its
     *                 return value inserted into the call chain
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldVoid(SmartHandle function) {
        if (Arrays.equals(signature().argNames(), function.signature().argNames())) {
            return foldVoid(function.handle());
        } else {
            return foldVoid(signature().asFold(void.class).permuteWith(function).handle());
        }
    }

    /**
     * Acquire a static folding function from the given target class, using the
     * given name and Lookup. Pass all arguments to that function and insert
     * the resulting value as newName into the argument list.
     *
     * @param newName the name of the new first argument where the fold
     *                function's result will be passed
     * @param lookup  the Lookup to use for acquiring a folding function
     * @param target  the class on which to find the folding function
     * @param method  the name of the method to become a folding function
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldStatic(String newName, Lookup lookup, Class<?> target, String method) {
        Binder newBinder = binder.foldStatic(lookup, target, method);
        return new SmartBinder(this, signature().prependArg(newName, newBinder.type().parameterType(0)), binder);
    }

    /**
     * Acquire a public static folding function from the given target class,
     * using the given name. Pass all arguments to that function and insert
     * the resulting value as newName into the argument list.
     *
     * @param newName the name of the new first argument where the fold
     *                function's result will be passed
     * @param target  the class on which to find the folding function
     * @param method  the name of the method to become a folding function
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldStatic(String newName, Class<?> target, String method) {
        Binder newBinder = binder.foldStatic(target, method);
        return new SmartBinder(this, signature().prependArg(newName, newBinder.type().parameterType(0)), newBinder);
    }

    /**
     * Acquire a virtual folding function from the first argument's class,
     * using the given name and Lookup. Pass all arguments to that function and
     * insert the resulting value as newName into the argument list.
     *
     * @param newName the name of the new first argument where the fold
     *                function's result will be passed
     * @param lookup  the Lookup to use for acquiring a folding function
     * @param method  the name of the method to become a folding function
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldVirtual(String newName, Lookup lookup, String method) {
        Binder newBinder = binder.foldVirtual(lookup, method);
        return new SmartBinder(this, signature().prependArg(newName, newBinder.type().parameterType(0)), newBinder);
    }

    /**
     * Acquire a public virtual folding function from the first argument's
     * class, using the given name and Lookup. Pass all arguments to that
     * function and insert the resulting value as newName into the argument
     * list.
     *
     * @param newName the name of the new first argument where the fold
     *                function's result will be passed
     * @param method  the name of the method to become a folding function
     * @return a new SmartBinder with the fold applied
     */
    public SmartBinder foldVirtual(String newName, String method) {
        Binder newBinder = binder.foldVirtual(method);
        return new SmartBinder(this, signature().prependArg(newName, newBinder.type().parameterType(0)), newBinder);
    }

    ///////////////////////////////////////////////////////////////////////////
    // PERMUTES, based on MethodHandles.permuteArguments
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Using the argument names and order in the target Signature, permute the
     * arguments in this SmartBinder. Arguments may be duplicated or omitted
     * in the target Signature, but all arguments in the target must be defined
     * in this SmartBinder .
     *
     * @param target the Signature from which to derive a new argument list
     * @return a new SmartBinder with the permute applied
     */
    public SmartBinder permute(Signature target) {
        return new SmartBinder(this, target, binder.permute(signature().to(target)));
    }

    /**
     * Using the argument names and order in the given targetNames, permute the
     * arguments in this SmartBinder. Arguments may be duplicated or omitted
     * in the targetNames array, but all arguments in the target must be
     * defined in this SmartBinder.
     *
     * @param targetNames the array of names from which to derive a new argument
     *                    list
     * @return a new SmartBinder with the permute applied
     */
    public SmartBinder permute(String... targetNames) {
        return permute(signature().permute(targetNames));
    }

    /**
     * Permute all parameters except the names given. Blacklisting to #permute's
     * whitelisting.
     *
     * @param excludeNames parameter patterns to exclude
     * @return a new SmartBinder with the exclude applied
     */
    public SmartBinder exclude(String... excludeNames) {
        return permute(signature().exclude(excludeNames));
    }

    ///////////////////////////////////////////////////////////////////////////
    // SPREADS, based on MethodHandle#asSpreader.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Spread a trailing array into the specified argument types.
     *
     * @param spreadNames the names for the spread out arguments
     * @param spreadTypes the types as which to spread the incoming array
     * @return a new SmartBinder with the spread applied
     */
    public SmartBinder spread(String[] spreadNames, Class<?>... spreadTypes) {
        return new SmartBinder(this, signature().spread(spreadNames, spreadTypes), binder.spread(spreadTypes));
    }

    /**
     * Spread a trailing array into count number of arguments, using the
     * natural component type for the array. Build names for the arguments
     * using the given baseName plus the argument's index.
     *
     * Example:
     * Current binder has a signature of (int, String[])void. We want
     * to spread the strings into five arguments named "str".
     *
     * <code>binder = binder.spread("str", 5)</code>
     *
     * The resulting signature will have five trailing arguments named
     * "arg0" through "arg4".
     *
     * @param baseName the base name from which to create the new argument names
     * @param count    the count of arguments to spread
     * @return a new SmartBinder with the spread applied
     */
    public SmartBinder spread(String baseName, int count) {
        return new SmartBinder(this, signature().spread(baseName, count), binder.spread(count));
    }

    ///////////////////////////////////////////////////////////////////////////
    // INSERTS, based on MethodHandles.insertArguments.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, Object value) {
        return new SmartBinder(this, signature().insertArg(index, name, value.getClass()), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, boolean value) {
        return new SmartBinder(this, signature().insertArg(index, name, boolean.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, byte value) {
        return new SmartBinder(this, signature().insertArg(index, name, byte.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, short value) {
        return new SmartBinder(this, signature().insertArg(index, name, short.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, char value) {
        return new SmartBinder(this, signature().insertArg(index, name, char.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, int value) {
        return new SmartBinder(this, signature().insertArg(index, name, int.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, long value) {
        return new SmartBinder(this, signature().insertArg(index, name, long.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, float value) {
        return new SmartBinder(this, signature().insertArg(index, name, float.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, double value) {
        return new SmartBinder(this, signature().insertArg(index, name, double.class), binder.insert(index, value));
    }

    /**
     * Insert an argument into the argument list at the given index with the
     * given name and value.
     *
     * @param index the index at which to insert the argument
     * @param name  the name of the new argument
     * @param type  the type to use in the new signature
     * @param value the value of the new argument
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String name, Class<?> type, Object value) {
        return new SmartBinder(this, signature().insertArg(index, name, type), binder.insert(index, type, value));
    }

    /**
     * Insert arguments into the argument list at the given index with the
     * given names and values.
     *
     * @param index  the index at which to insert the arguments
     * @param names  the names of the new arguments
     * @param types  the types of the new arguments
     * @param values the values of the new arguments
     * @return a new SmartBinder with the insert applied
     */
    public SmartBinder insert(int index, String[] names, Class<?>[] types, Object... values) {
        return new SmartBinder(this, signature().insertArgs(index, names, types), binder.insert(index, types, values));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, Object value) {
        return new SmartBinder(this, signature().appendArg(name, value.getClass()), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, boolean value) {
        return new SmartBinder(this, signature().appendArg(name, boolean.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, byte value) {
        return new SmartBinder(this, signature().appendArg(name, byte.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, short value) {
        return new SmartBinder(this, signature().appendArg(name, short.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, char value) {
        return new SmartBinder(this, signature().appendArg(name, char.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, int value) {
        return new SmartBinder(this, signature().appendArg(name, int.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, long value) {
        return new SmartBinder(this, signature().appendArg(name, long.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, float value) {
        return new SmartBinder(this, signature().appendArg(name, float.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, double value) {
        return new SmartBinder(this, signature().appendArg(name, double.class), binder.append(value));
    }

    /**
     * Append the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param type  the type to use in the new signature
     * @param value the value of the new argument
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String name, Class<?> type, Object value) {
        return new SmartBinder(this, signature().appendArg(name, type), binder.append(new Class<?>[]{type}, value));
    }

    /**
     * Append the given arguments to the argument list, assigning them the
     * given names.
     *
     * @param names  the names of the new arguments
     * @param types  the types to use in the new signature
     * @param values the values of the new arguments
     * @return a new SmartBinder with the append applied
     */
    public SmartBinder append(String[] names, Class<?>[] types, Object... values) {
        return new SmartBinder(this, signature().appendArgs(names, types), binder.append(types, values));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, Object value) {
        return new SmartBinder(this, signature().prependArg(name, value.getClass()), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, boolean value) {
        return new SmartBinder(this, signature().prependArg(name, boolean.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, byte value) {
        return new SmartBinder(this, signature().prependArg(name, byte.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, short value) {
        return new SmartBinder(this, signature().prependArg(name, short.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, char value) {
        return new SmartBinder(this, signature().prependArg(name, char.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, int value) {
        return new SmartBinder(this, signature().prependArg(name, int.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, long value) {
        return new SmartBinder(this, signature().prependArg(name, long.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, float value) {
        return new SmartBinder(this, signature().prependArg(name, float.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, double value) {
        return new SmartBinder(this, signature().prependArg(name, double.class), binder.prepend(value));
    }

    /**
     * Prepend the given argument to the argument list, assigning it the
     * given name.
     *
     * @param name  the name of the new argument
     * @param type  the type to use in the new signature
     * @param value the value of the new argument
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String name, Class<?> type, Object value) {
        return new SmartBinder(this, signature().prependArg(name, type), binder.prepend(type, value));
    }

    /**
     * Prepend the given arguments to the argument list, assigning them the
     * given name.
     *
     * @param names  the names of the new arguments
     * @param types  the types to use in the new signature
     * @param values the values of the new arguments
     * @return a new SmartBinder with the prepend applied
     */
    public SmartBinder prepend(String[] names, Class<?>[] types, Object... values) {
        return new SmartBinder(this, signature().prependArgs(names, types), binder.prepend(types, values));
    }

    ///////////////////////////////////////////////////////////////////////////
    // DROPS, based on MethodHandles.dropArguments.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Drop the argument with the given name.
     *
     * @param name the name of the argument to drop
     * @return a new SmartBinder with the drop applied
     */
    public SmartBinder drop(String name) {
        int index = signature().argOffset(name);
        return new SmartBinder(this, signature().dropArg(index), binder.drop(index));
    }

    /**
     * Drop the last argument.
     *
     * @return a new SmartBinder with the drop applied
     */
    public SmartBinder dropLast() {
        return dropLast(1);
    }

    /**
     * Drop the last N arguments.
     *
     * @param count the count of arguments to drop
     * @return a new SmartBinder with the drop applied
     */
    public SmartBinder dropLast(int count) {
        return new SmartBinder(this, signature().dropLast(count), binder.dropLast(count));
    }

    /**
     * Drop the first argument.
     *
     * @return a new SmartBinder with the drop applied
     */
    public SmartBinder dropFirst() {
        return dropFirst(1);
    }

    /**
     * Drop the first N arguments.
     *
     * @param count the count of arguments to drop
     * @return a new SmartBinder with the drop applied
     */
    public SmartBinder dropFirst(int count) {
        return new SmartBinder(this, signature().dropFirst(count), binder.dropFirst(count));
    }

    ///////////////////////////////////////////////////////////////////////////
    // COLLECTS, based on MethodHandle#asCollector
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Collect arguments matching namePattern into an trailing array argument
     * named outName.
     *
     * The namePattern is a standard regular expression.
     *
     * @param outName     the name of the new array argument
     * @param namePattern a pattern with which to match arguments for collecting
     * @return a new SmartBinder with the collect applied
     */
    public SmartBinder collect(String outName, String namePattern) {
        int index = signature().argOffsets(namePattern);

        assert index >= 0 : "no arguments matching " + namePattern + " found in signature " + signature();

        Signature newSignature = signature().collect(outName, namePattern);

        return new SmartBinder(this, newSignature, binder.collect(index, signature().argCount() - (newSignature.argCount() - 1), Array.newInstance(signature().argType(index), 0).getClass()));
    }

    ///////////////////////////////////////////////////////////////////////////
    // CASTS, based on MethodHandles.explicitCastArguments and MethodHandle.asType.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Cast the incoming arguments to the types in the given signature. The
     * argument count must match, but the names in the target signature are
     * ignored.
     *
     * @param target the Signature to which arguments should be cast
     * @return a new SmartBinder with the cast applied
     */
    public SmartBinder cast(Signature target) {
        return new SmartBinder(target, binder.cast(target.type()));
    }

    /**
     * Cast the incoming arguments to the return and argument types given. The
     * argument count must match.
     *
     * @param returnType the return type for the casted signature
     * @param argTypes   the types of the arguments for the casted signature
     * @return a new SmartBinder with the cast applied
     */
    public SmartBinder cast(Class<?> returnType, Class<?>... argTypes) {
        return new SmartBinder(this, new Signature(returnType, argTypes, signature().argNames()), binder.cast(returnType, argTypes));
    }

    /**
     * Cast the incoming arguments to the return, first argument type, and
     * remaining argument types. Provide for convenience when dealing with
     * virtual method argument lists, which frequently omit the target
     * object.
     *
     * @param returnType the return type for the casted signature
     * @param firstArg   the type of the first argument for the casted signature
     * @param restArgs   the types of the remaining arguments for the casted signature
     * @return a new SmartBinder with the cast applied.
     */
    public SmartBinder castVirtual(Class<?> returnType, Class<?> firstArg, Class<?>... restArgs) {
        return new SmartBinder(this, new Signature(returnType, firstArg, restArgs, signature().argNames()), binder.castVirtual(returnType, firstArg, restArgs));
    }

    /**
     * Cast the named argument to the given type.
     *
     * @param name the name of the argument to cast
     * @param type the type to which that argument will be cast
     * @return a new SmartBinder with the cast applied
     */
    public SmartBinder castArg(String name, Class<?> type) {
        Signature newSig = signature().replaceArg(name, name, type);
        return new SmartBinder(this, newSig, binder.cast(newSig.type()));
    }

    /**
     * Cast the return value to the given type.
     *
     * Example: Our current signature is (String)String but the method this
     * handle will eventually call returns CharSequence.
     *
     * <code>binder = binder.castReturn(CharSequence.class);</code>
     *
     * Our handle will now successfully find and call the target method and
     * propagate the returned CharSequence as a String.
     *
     * @param type the new type for the return value
     * @return a new SmartBinder
     */
    public SmartBinder castReturn(Class<?> type) {
        return new SmartBinder(this, signature().changeReturn(type), binder.cast(type, binder.type().parameterArray()));
    }

    ///////////////////////////////////////////////////////////////////////////
    // FILTERS, based on MethodHandles.filterReturnValue and filterArguments.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Use the given filter function to transform the return value at this
     * point in the binder. The filter will be inserted into the handle, and
     * return values will pass through it before continuing.
     *
     * The filter's argument must match the expected return value downstream
     * from this point in the binder, and the return value must match the
     * return value at this point in the binder.
     *
     * @param filter the function to use to transform the return value at this point
     * @return a new SmartBinder with the filter applied
     */
    public SmartBinder filterReturn(MethodHandle filter) {
        return new SmartBinder(this, signature().changeReturn(filter.type().returnType()), binder.filterReturn(filter));
    }

    /**
     * Use the given filter function to transform the return value at this
     * point in the binder. The filter will be inserted into the handle, and
     * return values will pass through it before continuing.
     *
     * The filter's argument must match the expected return value downstream
     * from this point in the binder, and the return value must match the
     * return value at this point in the binder.
     *
     * @param filter the function to use to transform the return value at this point
     * @return a new SmartBinder with the filter applied
     */
    public SmartBinder filterReturn(SmartHandle filter) {
        return new SmartBinder(this, signature().changeReturn(filter.signature().type().returnType()), binder.filterReturn(filter.handle()));
    }

    ///////////////////////////////////////////////////////////////////////////
    // INVOKES, terminating the handle chain at a concrete target
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Terminate this binder by looking up the named virtual method on the
     * first argument's type. Perform the actual method lookup using the given
     * Lookup object.
     *
     * @param lookup the Lookup to use for handle lookups
     * @param name   the name of the target virtual method
     * @return a SmartHandle with this binder's starting signature, bound
     * to the target method
     * @throws NoSuchMethodException  if the named method with current signature's types does not exist
     * @throws IllegalAccessException if the named method is not accessible to the given Lookup
     */
    public SmartHandle invokeVirtual(Lookup lookup, String name) throws NoSuchMethodException, IllegalAccessException {
        return new SmartHandle(start, binder.invokeVirtual(lookup, name));
    }

    /**
     * Terminate this binder by looking up the named virtual method on the
     * first argument's type. Perform the actual method lookup using the given
     * Lookup object. If the lookup fails, a RuntimeException will be raised,
     * containing the actual reason. This method is for convenience in (for
     * example) field declarations, where checked exceptions noise up code
     * that can't recover anyway.
     *
     * Use this in situations where you would not expect your library to be
     * usable if the target method can't be acquired.
     *
     * @param lookup the Lookup to use for handle lookups
     * @param name   the name of the target virtual method
     * @return a SmartHandle with this binder's starting signature, bound
     * to the target method
     */
    public SmartHandle invokeVirtualQuiet(Lookup lookup, String name) {
        return new SmartHandle(start, binder.invokeVirtualQuiet(lookup, name));
    }

    /**
     * Terminate this binder by looking up the named static method on the
     * given target type. Perform the actual method lookup using the given
     * Lookup object.
     *
     * @param lookup the Lookup to use for handle lookups
     * @param target the type on which to find the static method
     * @param name   the name of the target static method
     * @return a SmartHandle with this binder's starting signature, bound
     * to the target method
     * @throws NoSuchMethodException  if the named method with current signature's types does not exist
     * @throws IllegalAccessException if the named method is not accessible to the given Lookup
     */
    public SmartHandle invokeStatic(Lookup lookup, Class<?> target, String name) throws NoSuchMethodException, IllegalAccessException {
        return new SmartHandle(start, binder.invokeStatic(lookup, target, name));
    }

    /**
     * Terminate this binder by looking up the named static method on the
     * given target type. Perform the actual method lookup using the given
     * Lookup object. If the lookup fails, a RuntimeException will be raised,
     * containing the actual reason. This method is for convenience in (for
     * example) field declarations, where checked exceptions noise up code
     * that can't recover anyway.
     *
     * Use this in situations where you would not expect your library to be
     * usable if the target method can't be acquired.
     *
     * @param lookup the Lookup to use for handle lookups
     * @param target the type on which to find the static method
     * @param name   the name of the target static method
     * @return a SmartHandle with this binder's starting signature, bound
     * to the target method
     */
    public SmartHandle invokeStaticQuiet(Lookup lookup, Class<?> target, String name) {
        return new SmartHandle(start, binder.invokeStaticQuiet(lookup, target, name));
    }

    /**
     * Terminate this binder by invoking the given target handle. The signature
     * of this binder is not compared to the signature of the given
     * SmartHandle.
     *
     * @param target the handle to invoke
     * @return a new SmartHandle with this binder's starting signature, bound
     * through to the given handle
     */
    public SmartHandle invoke(SmartHandle target) {
        return new SmartHandle(start, binder.invoke(target.handle()));
    }

    /**
     * Terminate this binder by invoking the given target handle.
     *
     * @param target the handle to invoke
     * @return a new SmartHandle with this binder's starting signature, bound
     * through to the given handle
     */
    public SmartHandle invoke(MethodHandle target) {
        return new SmartHandle(start, binder.invoke(target));
    }

    /**
     * Terminate this binder by setting an array element based on the current
     * signature. The signature should have the array as its first argument,
     * an integer index as its second, and an appropriately-typed value as its
     * third. It should have a void return value.
     *
     * Invoking the resulting handle will (eventually) perform the array
     * assignment.
     *
     * @return a new SmartHandle with this binder's starting signature, bound
     * to an array assignment operation
     */
    public SmartHandle arrayGet() {
        return new SmartHandle(start, binder.arrayGet());
    }

    /**
     * Terminate this binder by getting an array element based on the current
     * signature. The signature should have the array as its first argument and
     * an integer index as its second, and an appropriately-typed return value.
     *
     * Invoking the resulting handle will (eventually) perform the array
     * assignment.
     *
     * @return a new SmartHandle with this binder's starting signature, bound
     * to an array dereference operation
     */
    public SmartHandle arraySet() {
        return new SmartHandle(start, binder.arraySet());
    }

    /**
     * Terminate this binder by returning its sole remaining argument. The
     * signature must take only one argument whose type matches the return
     * type.
     *
     * Invoking the resulting handle will (eventually) return the argument
     * passed in at this point.
     *
     * @return a new SmartHandle with this binder's starting signature that
     * simply returns its sole received argument
     */
    public SmartHandle identity() {
        return new SmartHandle(start, binder.identity());
    }

    ///////////////////////////////////////////////////////////////////////////
    // OTHER UTILITIES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Print this binder's current signature to System.out.
     *
     * @return this SmartBinder
     */
    public SmartBinder printSignature() {
        return printSignature(System.out);
    }

    /**
     * Print this binder's current signature to the give PrintStream.
     *
     * @return this SmartBinder
     */
    public SmartBinder printSignature(PrintStream ps) {
        ps.println(signature().toString());
        return this;
    }

    /**
     * Produce a SmartHandle from this binder that invokes a leading
     * MethodHandle argument with the remaining arguments.
     *
     * @return a SmartHandle that invokes its leading MethodHandle argument
     */
    public SmartHandle invoker() {
        return new SmartHandle(start, binder.invoker());
    }

    /**
     * Filter the arguments matching the given pattern using the given filter function.
     *
     * @param pattern the regular expression pattern to match arguments
     * @param filter the MethodHandle to use to filter the arguments
     * @return a new SmartBinder with the filter applied
     */
    public SmartBinder filter(String pattern, MethodHandle filter) {
        String[] argNames = signature().argNames();
        Pattern pat = Pattern.compile(pattern);

        Binder newBinder = binder();
        Signature newSig = signature();
        for (int i = 0; i < argNames.length; i++) {
            if (pat.matcher(argNames[i]).matches()) {
                newBinder = newBinder.filter(i, filter);
                newSig = newSig.argType(i, filter.type().returnType());
            }
        }

        return new SmartBinder(newSig, newBinder);
    }

    /**
     * @see Binder#tryFinally(MethodHandle)
     */
    public SmartBinder tryFinally(MethodHandle post) {
        return new SmartBinder(this, signature(), binder.tryFinally(post));
    }
}
