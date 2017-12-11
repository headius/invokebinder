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
package com.headius.invokebinder;

import com.headius.invokebinder.transform.Cast;
import com.headius.invokebinder.transform.Catch;
import com.headius.invokebinder.transform.Collect;
import com.headius.invokebinder.transform.Convert;
import com.headius.invokebinder.transform.Drop;
import com.headius.invokebinder.transform.Filter;
import com.headius.invokebinder.transform.FilterReturn;
import com.headius.invokebinder.transform.Fold;
import com.headius.invokebinder.transform.Insert;
import com.headius.invokebinder.transform.Permute;
import com.headius.invokebinder.transform.Spread;
import com.headius.invokebinder.transform.Transform;
import com.headius.invokebinder.transform.TryFinally;
import com.headius.invokebinder.transform.Varargs;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * The Binder class provides a DSL for building a chain of MethodHandles using
 * various of the adaptations provided by java.lang.invoke.MethodHandles. The
 * transformations are pushed into a stack, allowing the DSL to operate forward
 * from an incoming signature rather than backward from a target handle. This
 * is often conceptually easier to understand, and certainly easier to read.
 *
 * The transformations are also applied simultaneously to the starting
 * java.lang.invoke.MethodType, allowing Binder to check at each step whether
 * the adaptation is valid.
 *
 * Here's a typical use, starting with a signature that takes two Strings and
 * returns a String, dropping and inserting arguments, casting to a target
 * signature, and finally calling a target handle with that signature.
 *
 * <pre>
 * MethodHandle mh = Binder
 *     .from(String.class, String.class, String.class) // String w(String, String)
 *     .drop(1, String.class) // String x(String)
 *     .insert(0, 'hello') // String y(String, String)
 *     .cast(String.class, CharSequence.class, Object.class) // String z(CharSequence, Object)String
 *     .invoke(someTargetHandle);
 * </pre>
 */
public class Binder {

    private final Logger logger = Logger.getLogger("Invoke Binder");
    private final List<Transform> transforms = new LinkedList<>();
    private final List<MethodType> types = new LinkedList<>();
    private final MethodType start;
    private final MethodHandles.Lookup lookup;

    /**
     * Construct a new Binder, starting from a given MethodType. Use a public
     * java.lang.invoke.MethodHandles.Lookup for retrieving direct handles.
     *
     * @param start the starting MethodType, for calls entering the eventual chain
     */
    public Binder(MethodType start) {
        this.start = start;
        this.types.add(0, start);
        this.lookup = MethodHandles.publicLookup();
    }

    /**
     * Construct a new Binder, starting from a given Lookup and MethodType.
     *
     * @param lookup the Lookup context to use for direct handles
     * @param start  the starting MethodType, for calls entering the eventual chain
     */
    public Binder(MethodHandles.Lookup lookup, MethodType start) {
        this.start = start;
        this.types.add(0, start);
        this.lookup = lookup;
    }

    /**
     * Construct a new Binder using the given invokebinder.
     *
     * @param source a Binder to duplicate
     */
    public Binder(Binder source) {
        this.start = source.start;
        this.types.addAll(source.types);
        this.transforms.addAll(source.transforms);
        this.lookup = source.lookup;
    }

    /**
     * Construct a new Binder using the given Lookup and invokebinder.
     *
     * @param lookup the Lookup context to use for direct handles
     * @param source the source Binder
     */
    public Binder(MethodHandles.Lookup lookup, Binder source) {
        this.start = source.start;
        this.types.addAll(source.types);
        this.transforms.addAll(source.transforms);
        this.lookup = lookup;
    }

    /**
     * Construct a new Binder using the given invokebinder plus an additional transform
     *
     * @param source    the source Binder
     * @param transform the additional Transform
     */
    public Binder(Binder source, Transform transform) {
        this.start = source.start;
        this.types.addAll(source.types);
        this.transforms.addAll(source.transforms);
        add(transform);
        this.lookup = source.lookup;
    }

    /**
     * Construct a new Binder using the given invokebinder plus an additional transform and current type
     *
     * @param source    the source Binder
     * @param transform the additional Transform
     * @param type      the new current type resulting from the transform
     */
    public Binder(Binder source, Transform transform, MethodType type) {
        this.start = source.start;
        this.types.addAll(source.types);
        this.transforms.addAll(source.transforms);
        add(transform, type);
        this.lookup = source.lookup;
    }

    /**
     * Construct a new Binder, starting from a given MethodType.
     *
     * @param start the starting MethodType, for calls entering the eventual chain
     * @return the Binder object
     */
    public static Binder from(MethodType start) {
        return new Binder(start);
    }

    /**
     * Construct a new Binder, starting from a given MethodType.
     *
     * @param lookup the Lookup context to use for direct handles
     * @param start  the starting MethodType, for calls entering the eventual chain
     * @return the Binder object
     */
    public static Binder from(MethodHandles.Lookup lookup, MethodType start) {
        return new Binder(lookup, start);
    }

    /**
     * Construct a new Binder using a return type.
     *
     * @param returnType the return type of the incoming signature
     * @return the Binder object
     */
    public static Binder from(Class<?> returnType) {
        return from(MethodType.methodType(returnType));
    }

    /**
     * Construct a new Binder using a return type.
     *
     * @param lookup     the Lookup context to use for direct handles
     * @param returnType the return type of the incoming signature
     * @return the Binder object
     */
    public static Binder from(MethodHandles.Lookup lookup, Class<?> returnType) {
        return from(lookup, MethodType.methodType(returnType));
    }

    /**
     * Construct a new Binder using a return type and argument types.
     *
     * @param returnType the return type of the incoming signature
     * @param argTypes   the argument types of the incoming signature
     * @return the Binder object
     */
    public static Binder from(Class<?> returnType, Class<?>[] argTypes) {
        return from(MethodType.methodType(returnType, argTypes));
    }

    /**
     * Construct a new Binder using a return type and argument types.
     *
     * @param lookup     the Lookup context to use for direct handles
     * @param returnType the return type of the incoming signature
     * @param argTypes   the argument types of the incoming signature
     * @return the Binder object
     */
    public static Binder from(MethodHandles.Lookup lookup, Class<?> returnType, Class<?>[] argTypes) {
        return from(lookup, MethodType.methodType(returnType, argTypes));
    }

    /**
     * Construct a new Binder using a return type and argument types.
     *
     * @param returnType the return type of the incoming signature
     * @param argType0   the first argument type of the incoming signature
     * @param argTypes   the remaining argument types of the incoming signature
     * @return the Binder object
     */
    public static Binder from(Class<?> returnType, Class<?> argType0, Class<?>... argTypes) {
        return from(MethodType.methodType(returnType, argType0, argTypes));
    }

    /**
     * Construct a new Binder using a return type and argument types.
     *
     * @param lookup     the Lookup context to use for direct handles
     * @param returnType the return type of the incoming signature
     * @param argType0   the first argument type of the incoming signature
     * @param argTypes   the remaining argument types of the incoming signature
     * @return the Binder object
     */
    public static Binder from(MethodHandles.Lookup lookup, Class<?> returnType, Class<?> argType0, Class<?>... argTypes) {
        return from(lookup, MethodType.methodType(returnType, argType0, argTypes));
    }

    /**
     * Construct a new Binder, starting from a given invokebinder.
     *
     * @param start the starting invokebinder; the new one will start with the current endpoint type
     *              of the given invokebinder
     * @return the Binder object
     */
    public static Binder from(Binder start) {
        return new Binder(start);
    }

    /**
     * Construct a new Binder, starting from a given invokebinder.
     *
     * @param lookup the Lookup context to use for direct handles
     * @param start  the starting invokebinder; the new one will start with the current endpoint type
     *               of the given invokebinder
     * @return the Binder object
     */
    public static Binder from(MethodHandles.Lookup lookup, Binder start) {
        return new Binder(lookup, start);
    }

    /**
     * Join this binder to an existing one by applying its transformations after
     * this one.
     *
     * @param other the Binder containing the set of transformations to append
     * @return a new Binder combining this Binder with the target Binder
     */
    public Binder to(Binder other) {
        assert type().equals(other.start);

        Binder newBinder = new Binder(this);

        for (ListIterator<Transform> iter = other.transforms.listIterator(other.transforms.size()); iter.hasPrevious(); ) {
            Transform t = iter.previous();
            newBinder.add(t);
        }

        return newBinder;
    }

    /**
     * Use an alternate java.lang.invoke.MethodHandles.Lookup as the default for
     * any direct handles created.
     *
     * @param lookup the new Lookup context to use
     * @return a new Binder with the given lookup
     */
    public Binder withLookup(MethodHandles.Lookup lookup) {
        return from(lookup, this);
    }

    /**
     * Add a Transform to the chain.
     *
     * @param transform
     */
    private void add(Transform transform) {
        add(transform, transform.down(types.get(0)));
    }

    /**
     * Add a Transform with an associated MethodType target to the chain.
     *
     * @param transform
     * @param target
     */
    private void add(Transform transform, MethodType target) {
        types.add(0, target);
        transforms.add(0, transform);
    }

    /**
     * The current MethodType, were the handle chain to terminate at this point.
     *
     * @return the current MethodType
     */
    public MethodType type() {
        return types.get(0);
    }

    /**
     * Println the current MethodType to the given stream.
     *
     * @param ps a PrintStream to which to println the current MethodType
     * @return this Binding
     */
    public Binder printType(PrintStream ps) {
        ps.println(types.get(0));
        return this;
    }

    /**
     * Println the current MethodType to stdout.
     *
     * @return this Binding
     */
    public Binder printType() {
        return printType(System.out);
    }

    /**
     * Log the current MethodType as info.
     *
     * @return this Binding
     */
    public Binder logType() {
        logger.info(types.get(0).toString());
        return this;
    }

    /**
     * Insert at the given index the given boolean value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, boolean value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given byte value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, byte value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given short value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, short value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given char value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, char value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given int value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, int value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given long value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, long value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given float value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, float value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given double value.
     *
     * @param index the index at which to insert the argument value
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, double value) {
        return new Binder(this, new Insert(index, value));
    }

    /**
     * Insert at the given index the given argument value(s).
     *
     * @param index  the index at which to insert the argument value
     * @param values the value(s) to insert
     * @return a new Binder
     */
    public Binder insert(int index, Object... values) {
        return new Binder(this, new Insert(index, values));
    }

    /**
     * Insert at the given index the given argument value.
     *
     * @param index the index at which to insert the argument value
     * @param type  the actual type to use, rather than getClass
     * @param value the value to insert
     * @return a new Binder
     */
    public Binder insert(int index, Class<?> type, Object value) {
        return new Binder(this, new Insert(index, new Class[]{type}, value));
    }

    /**
     * Insert at the given index the given argument value(s).
     *
     * @param index  the index at which to insert the argument value
     * @param types  the actual types to use, rather than getClass
     * @param values the value(s) to insert
     * @return a new Binder
     */
    public Binder insert(int index, Class<?>[] types, Object... values) {
        return new Binder(this, new Insert(index, types, values));
    }

    /**
     * Append to the argument list the given boolean value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(boolean value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given byte value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(byte value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given short value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(short value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given char value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(char value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given int value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(int value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given long value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(long value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given float value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(float value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given double value.
     *
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(double value) {
        return new Binder(this, new Insert(type().parameterCount(), value));
    }

    /**
     * Append to the argument list the given argument value(s).
     *
     * @param values the value(s) to append
     * @return a new Binder
     */
    public Binder append(Object... values) {
        return new Binder(this, new Insert(type().parameterCount(), values));
    }

    /**
     * Prepend to the argument list the given boolean value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(boolean value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given byte value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(byte value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given short value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(short value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given char value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(char value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given int value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(int value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given long value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(long value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given float value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(float value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given double value.
     *
     * @param value the value to prepend
     * @return a new Binder
     */
    public Binder prepend(double value) {
        return new Binder(this, new Insert(0, value));
    }

    /**
     * Prepend to the argument list the given argument value(s).
     *
     * @param values the value(s) to prepend
     * @return a new Binder
     */
    public Binder prepend(Object... values) {
        return new Binder(this, new Insert(0, values));
    }

    /**
     * Append to the argument list the given argument value with the specified type.
     *
     * @param type  the actual type to use, rather than getClass
     * @param value the value to append
     * @return a new Binder
     */
    public Binder append(Class<?> type, Object value) {
        return new Binder(this, new Insert(type().parameterCount(), new Class[]{type}, value));
    }

    /**
     * Append to the argument list the given argument values with the specified types.
     *
     * @param types  the actual types to use, rather than getClass
     * @param values the value(s) to append
     * @return a new Binder
     */
    public Binder append(Class<?>[] types, Object... values) {
        return new Binder(this, new Insert(type().parameterCount(), types, values));
    }

    /**
     * Prepend to the argument list the given argument value with the specified type
     *
     * @param type  the actual type to use, rather than getClass
     * @param value the value(s) to prepend
     * @return a new Binder
     */
    public Binder prepend(Class<?> type, Object value) {
        return new Binder(this, new Insert(0, new Class[]{type}, value));
    }

    /**
     * Prepend to the argument list the given argument values with the specified types.
     *
     * @param types  the actual types to use, rather than getClass
     * @param values the value(s) to prepend
     * @return a new Binder
     */
    public Binder prepend(Class<?>[] types, Object... values) {
        return new Binder(this, new Insert(0, types, values));
    }

    /**
     * Drop a single argument at the given index.
     *
     * @param index the index at which to drop an argument
     * @return a new Binder
     */
    public Binder drop(int index) {
        return drop(index, 1);
    }

    /**
     * Drop from the given index a number of arguments.
     *
     * @param index the index at which to start dropping
     * @param count the number of arguments to drop
     * @return a new Binder
     */
    public Binder drop(int index, int count) {
        return new Binder(this, new Drop(index, Arrays.copyOfRange(type().parameterArray(), index, index + count)));
    }

    /**
     * Drop a single argument at the end of the argument list.
     *
     * @return a new Binder
     */
    public Binder dropLast() {
        return dropLast(1);
    }

    /**
     * Drop from the end of the argument list a number of arguments.
     *
     * @param count the number of arguments to drop
     * @return a new Binder
     */
    public Binder dropLast(int count) {
        assert count <= type().parameterCount();

        return drop(type().parameterCount() - count, count);
    }

    /**
     * Drop a single argument at the beginning of the argument list.
     *
     * @return a new Binder
     */
    public Binder dropFirst() {
        return dropFirst(1);
    }

    /**
     * Drop from the end of the argument list a number of arguments.
     *
     * @param count the number of arguments to drop
     * @return a new Binder
     */
    public Binder dropFirst(int count) {
        assert count <= type().parameterCount();

        return drop(0, count);
    }

    /**
     * Drop all arguments from this handle chain
     *
     * @return a new Binder
     */
    public Binder dropAll() {
        return drop(0, type().parameterCount());
    }

    /**
     * Convert the incoming arguments to the given MethodType. The conversions
     * applied are equivalent to those in MethodHandle.asType(MethodType).
     *
     * @param target the target MethodType
     * @return a new Binder
     */
    public Binder convert(MethodType target) {
        return new Binder(this, new Convert(type()), target);
    }

    /**
     * Convert the incoming arguments to the given MethodType. The conversions
     * applied are equivalent to those in MethodHandle.asType(MethodType).
     *
     * @param returnType the target return type
     * @param argTypes   the target argument types
     * @return a new Binder
     */
    public Binder convert(Class<?> returnType, Class<?>... argTypes) {
        return new Binder(this, new Convert(type()), MethodType.methodType(returnType, argTypes));
    }

    /**
     * Cast the incoming arguments to the given MethodType. The casts
     * applied are equivalent to those in MethodHandles.explicitCastArguments(mh, MethodType).
     *
     * @param type the target MethodType
     * @return a new Binder
     */
    public Binder cast(MethodType type) {
        return new Binder(this, new Cast(type()), type);
    }

    /**
     * Cast the incoming arguments to the given MethodType. The casts
     * applied are equivalent to those in MethodHandle.explicitCastArguments(MethodType).
     *
     * @param returnType the target return type
     * @param argTypes   the target argument types
     * @return a new Binder
     */
    public Binder cast(Class<?> returnType, Class<?>... argTypes) {
        return new Binder(this, new Cast(type()), MethodType.methodType(returnType, argTypes));
    }

    /**
     * Cast the incoming arguments to the given MethodType. The casts
     * applied are equivalent to those in MethodHandle.explicitCastArguments(MethodType).
     *
     * @param returnType the target return type
     * @param firstType  the first argument type, usually a target type
     * @param restTypes  the remaining target argument types
     * @return a new Binder
     */
    public Binder castVirtual(Class<?> returnType, Class<?> firstType, Class<?>... restTypes) {
        return new Binder(this, new Cast(type()), MethodType.methodType(returnType, firstType, restTypes));
    }

    /**
     * Spread a trailing array argument into the specified argument types.
     *
     * @param spreadTypes the types into which to spread the incoming Object[]
     * @return a new Binder
     */
    public Binder spread(Class<?>... spreadTypes) {
        if (spreadTypes.length == 0) {
            return dropLast();
        }

        return new Binder(this, new Spread(type(), spreadTypes));
    }

    /**
     * Spread a trailing array argument into the given number of arguments of
     * the type of the array.
     *
     * @param count the new count of arguments to spread from the trailing array
     * @return a new Binder
     */
    public Binder spread(int count) {
        if (count == 0) {
            return dropLast();
        }

        Class<?> aryType = type().parameterType(type().parameterCount() - 1);

        assert aryType.isArray();

        Class<?>[] spreadTypes = new Class[count];
        Arrays.fill(spreadTypes, aryType.getComponentType());

        return spread(spreadTypes);
    }

    /**
     * Box all incoming arguments from the given position onward into the given array type.
     *
     * @param index the index from which to start boxing args
     * @param type  the array type into which the args will be boxed
     * @return a new Binder
     */
    public Binder collect(int index, Class<?> type) {
        return new Binder(this, new Collect(type(), index, type));
    }

    /**
     * Box a range of incoming arguments into the given array type.
     *
     * @param index the index from which to start boxing args
     * @param count the count of arguments to box
     * @param type  the array type into which the args will be boxed
     * @return a new Binder
     */
    public Binder collect(int index, int count, Class<?> type) {
        return new Binder(this, new Collect(type(), index, count, type));
    }

    /**
     * Box all incoming arguments from the given position onward into the given array type.
     * This version accepts a variable number of incoming arguments.
     *
     * @param index the index from which to start boxing args
     * @param type  the array type into which the args will be boxed
     * @return a new Binder
     */
    public Binder varargs(int index, Class<?> type) {
        return new Binder(this, new Varargs(type(), index, type));
    }

    /**
     * Permute the incoming arguments to a new sequence specified by the given values.
     *
     * Arguments may be duplicated or dropped in this sequence.
     *
     * @param reorder the int offsets of the incoming arguments in the desired permutation
     * @return a new Binder
     */
    public Binder permute(int... reorder) {
        return new Binder(this, new Permute(type(), reorder));
    }

    /**
     * Process the incoming arguments using the given handle, inserting the result
     * as the first argument.
     *
     * @param function the function that will process the incoming arguments. Its
     *                 signature must match the current signature's arguments exactly.
     * @return a new Binder
     */
    public Binder fold(MethodHandle function) {
        return new Binder(this, new Fold(function));
    }

    public Binder foldVoid(MethodHandle function) {
        if (type().returnType() == void.class) {
            return fold(function);
        } else {
            return fold(function.asType(function.type().changeReturnType(void.class)));
        }
    }

    /**
     * Process the incoming arguments by calling the given static method on the
     * given class, inserting the result as the first argument.
     *
     * @param lookup the java.lang.invoke.MethodHandles.Lookup to use
     * @param target the class on which the method is defined
     * @param method the method to invoke on the first argument
     * @return a new Binder
     */
    public Binder foldStatic(MethodHandles.Lookup lookup, Class<?> target, String method) {
        return fold(Binder.from(type()).invokeStaticQuiet(lookup, target, method));
    }

    /**
     * Process the incoming arguments by calling the given static method on the
     * given class, inserting the result as the first argument.
     *
     * @param target the class on which the method is defined
     * @param method the method to invoke on the first argument
     * @return a new Binder
     */
    public Binder foldStatic(Class<?> target, String method) {
        return foldStatic(lookup, target, method);
    }

    /**
     * Process the incoming arguments by calling the given method on the first
     * argument, inserting the result as the first argument.
     *
     * @param lookup the java.lang.invoke.MethodHandles.Lookup to use
     * @param method the method to invoke on the first argument
     * @return a new Binder
     */
    public Binder foldVirtual(MethodHandles.Lookup lookup, String method) {
        return fold(Binder.from(type()).invokeVirtualQuiet(lookup, method));
    }

    /**
     * Process the incoming arguments by calling the given method on the first
     * argument, inserting the result as the first argument.
     *
     * @param method the method to invoke on the first argument
     * @return a new Binder
     */
    public Binder foldVirtual(String method) {
        return foldVirtual(lookup, method);
    }

    /**
     * Filter incoming arguments, starting at the given index, replacing each with the
     * result of calling the associated function in the given list.
     *
     * @param index     the index of the first argument to filter
     * @param functions the array of functions to transform the arguments
     * @return a new Binder
     */
    public Binder filter(int index, MethodHandle... functions) {
        return new Binder(this, new Filter(index, functions));
    }

    /**
     * Filter return value, using a function that produces the current return type
     * from another type. The new endpoint will have the return value that the
     * filter function accepts as an argument.
     *
     * @param function the array of functions to transform the arguments
     * @return a new Binder
     */
    public Binder filterReturn(MethodHandle function) {
        return new Binder(this, new FilterReturn(function));
    }

    /**
     * Apply transforms to run the given handle's logic as a "finally" block.
     *
     * try {
     * some_code // your eventual endpoint
     * } finally {
     * finally_logic // the given handle
     * }
     *
     * The layering uses a combination of catch and fold to reuse the same target
     * handle for both exceptional and non-exceptional paths. In essence, the
     * result is equivalent to using the given post logic as both an exception
     * handler (using catchException) and a "post fold" that runs after the main
     * downstream handles have run.
     *
     * @param post the logic that would live inside the "finally" block
     * @return a new Binder
     */
    public Binder tryFinally(MethodHandle post) {
        return new Binder(this, new TryFinally(post));
    }

    /**
     * Catch the given exception type from the downstream chain and handle it with the
     * given function.
     *
     * @param throwable the exception type to catch
     * @param function  the function to use for handling the exception
     * @return a new Binder
     */
    public Binder catchException(Class<? extends Throwable> throwable, MethodHandle function) {
        return new Binder(this, new Catch(throwable, function));
    }

    /**
     * Apply all transforms to an endpoint that does absolutely nothing. Useful for
     * creating exception handlers in void methods that simply ignore the exception.
     *
     * @return a handle that has all transforms applied and does nothing at its endpoint
     */
    public MethodHandle nop() {
        if (type().returnType() != void.class) {
            throw new InvalidTransformException("must have void return type to nop: " + type());
        }
        return invoke(Binder
                .from(type())
                .drop(0, type().parameterCount())
                .cast(Object.class)
                .constant(null));
    }

    /**
     * Throw the current signature's sole Throwable argument. Return type
     * does not matter, since it will never return.
     *
     * @return a handle that has all transforms applied and which will eventually throw an exception
     */
    public MethodHandle throwException() {
        if (type().parameterCount() != 1 || !Throwable.class.isAssignableFrom(type().parameterType(0))) {
            throw new InvalidTransformException("incoming signature must have one Throwable type as its sole argument: " + type());
        }
        return invoke(MethodHandles.throwException(type().returnType(), type().parameterType(0).asSubclass(Throwable.class)));
    }

    /**
     * Apply the tranforms, binding them to a constant value that will
     * propagate back through the chain. The chain's expected return type
     * at that point must be compatible with the given value's type.
     *
     * @param value the constant value to put at the end of the chain
     * @return a handle that has all transforms applied in sequence up to the constant
     */
    public MethodHandle constant(Object value) {
        return invoke(MethodHandles.constant(type().returnType(), value));
    }

    /**
     * Apply the tranforms, binding them to a handle that will simply return its sole
     * argument as its return value. The endpoint signature must have a single argument
     * of the same type as its return type.
     *
     * @return a handle that has all transforms applied in sequence
     */
    public MethodHandle identity() {
        return invoke(MethodHandles.identity(type().parameterType(0)));
    }

    /**
     * Apply the chain of transforms with the target method handle as the final
     * endpoint. Produces a handle that has the transforms in given sequence.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param target the endpoint handle to bind to
     * @return a handle that has all transforms applied in sequence up to endpoint
     */
    public MethodHandle invoke(MethodHandle target) {
        MethodHandle current = target;
        for (Transform t : transforms) {
            current = t.up(current);
        }

        // if resulting handle's type does not match start, attempt one more cast
        current = MethodHandles.explicitCastArguments(current, start);

        return current;
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and method. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to unreflect the method
     * @param method the Method to unreflect
     * @return the full handle chain, bound to the given method
     * @throws java.lang.IllegalAccessException if the method is not accessible
     */
    public MethodHandle invoke(MethodHandles.Lookup lookup, Method method) throws IllegalAccessException {
        return invoke(lookup.unreflect(method));
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and method. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to unreflect the method
     * @param method the Method to unreflect
     * @return the full handle chain, bound to the given method
     */
    public MethodHandle invokeQuiet(MethodHandles.Lookup lookup, Method method) {
        try {
            return invoke(lookup, method);
        } catch (IllegalAccessException iae) {
            throw new InvalidTransformException(iae);
        }
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to unreflect the method
     * @param target the class in which to find the method
     * @param name   the name of the method to invoke
     * @return the full handle chain, bound to the given method
     * @throws java.lang.NoSuchMethodException if the method does not exist
     * @throws java.lang.IllegalAccessException if the method is not accessible
     */
    public MethodHandle invokeStatic(MethodHandles.Lookup lookup, Class<?> target, String name) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findStatic(target, name, type()));
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the method
     * @param target the class in which to find the method
     * @param name   the name of the method to invoke
     * @return the full handle chain, bound to the given method
     */
    public MethodHandle invokeStaticQuiet(MethodHandles.Lookup lookup, Class<?> target, String name) {
        try {
            return invokeStatic(lookup, target, name);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to a virtual method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the method
     * @param name   the name of the method to invoke
     * @return the full handle chain, bound to the given method
     * @throws java.lang.NoSuchMethodException if the method does not exist
     * @throws java.lang.IllegalAccessException if the method is not accessible
     */
    public MethodHandle invokeVirtual(MethodHandles.Lookup lookup, String name) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findVirtual(type().parameterType(0), name, type().dropParameterTypes(0, 1)));
    }

    /**
     * Apply the chain of transforms and bind them to a virtual method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the method
     * @param name   the name of the method to invoke
     * @return the full handle chain, bound to the given method
     */
    public MethodHandle invokeVirtualQuiet(MethodHandles.Lookup lookup, String name) {
        try {
            return invokeVirtual(lookup, name);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to a special method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the method
     * @param name   the name of the method to invoke
     * @param caller the calling class
     * @return the full handle chain, bound to the given method
     * @throws java.lang.NoSuchMethodException if the method does not exist
     * @throws java.lang.IllegalAccessException if the method is not accessible
     */
    public MethodHandle invokeSpecial(MethodHandles.Lookup lookup, String name, Class<?> caller) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findSpecial(type().parameterType(0), name, type().dropParameterTypes(0, 1), caller));
    }

    /**
     * Apply the chain of transforms and bind them to a special method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the method
     * @param name   the name of the method to invoke
     * @param caller the calling class
     * @return the full handle chain, bound to the given method
     */
    public MethodHandle invokeSpecialQuiet(MethodHandles.Lookup lookup, String name, Class<?> caller) {
        try {
            return invokeSpecial(lookup, name, caller);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to a constructor specified
     * using the end signature plus the given class. The constructor will
     * be retrieved using the given Lookup and must match the end signature's
     * arguments exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the constructor
     * @param target the constructor's class
     * @return the full handle chain, bound to the given constructor
     * @throws java.lang.NoSuchMethodException if the constructor does not exist
     * @throws java.lang.IllegalAccessException if the constructor is not accessible
     */
    public MethodHandle invokeConstructor(MethodHandles.Lookup lookup, Class<?> target) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findConstructor(target, type().changeReturnType(void.class)));
    }

    /**
     * Apply the chain of transforms and bind them to a constructor specified
     * using the end signature plus the given class. The constructor will
     * be retrieved using the given Lookup and must match the end signature's
     * arguments exactly.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the constructor
     * @param target the constructor's class
     * @return the full handle chain, bound to the given constructor
     */
    public MethodHandle invokeConstructorQuiet(MethodHandles.Lookup lookup, Class<?> target) {
        try {
            return invokeConstructor(lookup, target);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to an object field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * the target class or a subclass as its only argument.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param name   the field's name
     * @return the full handle chain, bound to the given field access
     * @throws java.lang.NoSuchFieldException if the field does not exist
     * @throws java.lang.IllegalAccessException if the field is not accessible
     * @throws java.lang.NoSuchFieldException if the field does not exist
     * @throws java.lang.IllegalAccessException if the field is not accessible
     */
    public MethodHandle getField(MethodHandles.Lookup lookup, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findGetter(type().parameterType(0), name, type().returnType()));
    }

    /**
     * Apply the chain of transforms and bind them to an object field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * the target class or a subclass as its only argument.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param name   the field's name
     * @return the full handle chain, bound to the given field access
     */
    public MethodHandle getFieldQuiet(MethodHandles.Lookup lookup, String name) {
        try {
            return getField(lookup, name);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to a static field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * no arguments.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param target the class in which the field is defined
     * @param name   the field's name
     * @return the full handle chain, bound to the given field access
     * @throws java.lang.NoSuchFieldException if the field does not exist
     * @throws java.lang.IllegalAccessException if the field is not accessible or cannot be modified
     */
    public MethodHandle getStatic(MethodHandles.Lookup lookup, Class<?> target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findStaticGetter(target, name, type().returnType()));
    }

    /**
     * Apply the chain of transforms and bind them to a static field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * no arguments.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param target the class in which the field is defined
     * @param name   the field's name
     * @return the full handle chain, bound to the given field access
     */
    public MethodHandle getStaticQuiet(MethodHandles.Lookup lookup, Class<?> target, String name) {
        try {
            return getStatic(lookup, target, name);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param name   the field's name
     * @return the full handle chain, bound to the given field assignment
     * @throws java.lang.NoSuchFieldException if the field does not exist
     * @throws java.lang.IllegalAccessException if the field is not accessible or cannot be modified
     */
    public MethodHandle setField(MethodHandles.Lookup lookup, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findSetter(type().parameterType(0), name, type().parameterType(1)));
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param name   the field's name
     * @return the full handle chain, bound to the given field assignment
     */
    public MethodHandle setFieldQuiet(MethodHandles.Lookup lookup, String name) {
        try {
            return setField(lookup, name);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new InvalidTransformException(e);
        }
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param target the class in which the field is defined
     * @param name   the field's name
     * @return the full handle chain, bound to the given field assignment
     * @throws java.lang.NoSuchFieldException if the field does not exist
     * @throws java.lang.IllegalAccessException if the field is not accessible or cannot be modified
     */
    public MethodHandle setStatic(MethodHandles.Lookup lookup, Class<?> target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findStaticSetter(target, name, type().parameterType(0)));
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     *
     * If the final handle's type does not exactly match the initial type for
     * this Binder, an additional cast will be attempted.
     *
     * This version is "quiet" in that it throws an unchecked InvalidTransformException
     * if the target method does not exist or is inaccessible.
     *
     * @param lookup the MethodHandles.Lookup to use to look up the field
     * @param target the class in which the field is defined
     * @param name   the field's name
     * @return the full handle chain, bound to the given field assignment
     */
    public MethodHandle setStaticQuiet(MethodHandles.Lookup lookup, Class<?> target, String name) {
        try {
            return setStatic(lookup, target, name);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new InvalidTransformException(e);
        }
    }


    /**
     * Apply the chain of transforms and bind them to an array element set. The signature
     * at the endpoint must return void and receive the array type, int index, and array
     * element type.
     *
     * @return the full handle chain, bound to an array element set.
     */
    public MethodHandle arraySet() {
        return invoke(MethodHandles.arrayElementSetter(type().parameterType(0)));
    }

    /**
     * Apply the chain of transforms and bind them to a volatile array element set.
     *
     * @see Binder#arraySet()
     * @see VarHandle#setVolatile(Object...)
     */
    public MethodHandle arraySetVolatile() {
        return arrayAccess(VarHandle.AccessMode.SET_VOLATILE);
    }

    /**
     * Apply the chain of transforms and bind them to a release-fenced array element set.
     *
     * @see Binder#arraySet()
     * @see VarHandle#setRelease(Object...)
     */
    public MethodHandle arraySetAcquire() {
        return arrayAccess(VarHandle.AccessMode.SET_RELEASE);
    }

    /**
     * Apply the chain of transforms and bind them to an opaque (no ordering guarantee) array element set.
     *
     * @see Binder#arraySet()
     * @see VarHandle#setVolatile(Object...)
     */
    public MethodHandle arraySetOpaque() {
        return arrayAccess(VarHandle.AccessMode.SET_OPAQUE);
    }


    /**
     * Apply the chain of transforms and bind them to an array element get. The signature
     * at the endpoint must return the array element type and receive the array type and
     * int index.
     *
     * @return the full handle chain, bound to an array element get.
     */
    public MethodHandle arrayGet() {
        return invoke(MethodHandles.arrayElementGetter(type().parameterType(0)));
    }

    /**
     * Apply the chain of transforms and bind them to a volatile array element get.
     *
     * @see Binder#arrayGet()
     * @see VarHandle#getVolatile(Object...)
     */
    public MethodHandle arrayGetVolatile() {
        return arrayAccess(VarHandle.AccessMode.GET_VOLATILE);
    }

    /**
     * Apply the chain of transforms and bind them to an acquire-fenced array element get.
     *
     * @see Binder#arrayGet()
     * @see VarHandle#getAcquire(Object...)
     */
    public MethodHandle arrayGetAcquire() {
        return arrayAccess(VarHandle.AccessMode.GET_ACQUIRE);
    }

    /**
     * Apply the chain of transforms and bind them to an opaque (no ordering guarantee) array element get.
     *
     * @see Binder#arrayGet()
     * @see VarHandle#getVolatile(Object...)
     */
    public MethodHandle arrayGetOpaque() {
        return arrayAccess(VarHandle.AccessMode.GET_OPAQUE);
    }

    /**
     * Apply the chain of transforms and bind them to an array varhandle operation. The
     * signature at the endpoint must match the VarHandle access type passed in.
     */
    public MethodHandle arrayAccess(VarHandle.AccessMode mode) {
        return invoke(MethodHandles.arrayElementVarHandle(type().parameterType(0)).toMethodHandle(mode));
    }

    /**
     * Apply the chain of transforms and bind them to a boolean branch as from
     * java.lang.invoke.MethodHandles.guardWithTest. As with GWT, the current endpoint
     * signature must match the given target and fallback signatures.
     *
     * @param test      the test handle
     * @param truePath  the target handle
     * @param falsePath the fallback handle
     * @return the full handle chain bound to a branch
     */
    public MethodHandle branch(MethodHandle test, MethodHandle truePath, MethodHandle falsePath) {
        return invoke(MethodHandles.guardWithTest(test, truePath, falsePath));
    }

    /**
     * Produce a MethodHandle that invokes its leading MethodHandle argument
     * with the remaining arguments, returning the result.
     *
     * @return a new handle that invokes its leading MethodHandle argument
     */
    public MethodHandle invoker() {
        return invoke(MethodHandles.invoker(start));
    }

    /**
     * Produce Java code that would perform equivalent operations to this binder.
     *
     * @return Java code for the handle adaptations this Binder would produce.
     */
    public String toJava(MethodType incoming) {
        StringBuilder builder = new StringBuilder();
        boolean second = false;
        for (Transform transform : transforms) {
            if (second) builder.append('\n');
            second = true;
            builder.append(transform.toJava(incoming));
        }
        return builder.toString();
    }

}
