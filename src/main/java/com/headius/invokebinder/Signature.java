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
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Signature represents a series of method arguments plus their symbolic names.
 *
 * In order to make it easier to permute arguments, track their flow, and debug
 * cases where reordering or permuting fails to work properly, the Signature
 * class also tracks symbolic names for all arguments. This allows permuting
 * by name or by name pattern, avoiding the error-prone juggling of int[] for
 * the standard MethodHandles.permuteArguments call.
 *
 * A Signature is created starting using #thatReturns method, and expanded using
 * #withArgument for each named argument in sequence. Order is preserved.
 *
 * A Signature can be mutated into another by manipuating the argument list as
 * with java.lang.invoke.MethodType, but using argument names and name patterns
 * instead of integer offsets.
 *
 * Two signatures can be used to produce a permute array suitable for use in
 * java.lang.invoke.MethodHandles#permuteArguments using the #to methods. The
 * #to method can also accept a list of argument names, as a shortcut.
 *
 * @author headius
 */
public class Signature {
    private final MethodType methodType;
    private final String[] argNames;

    /**
     * Construct a new signature with the given return value.
     *
     * @param retval the return value for the new signature
     */
    Signature(Class retval) {
        this(MethodType.methodType(retval));
    }

    /**
     * Construct a new signature with the given return value, argument types,
     * and argument names.
     *
     * @param retval   the return value for the new signature
     * @param argTypes the argument types for the new signature
     * @param argNames the argument names for the new signature
     */
    Signature(Class retval, Class[] argTypes, String... argNames) {
        this(MethodType.methodType(retval, argTypes), argNames);
    }

    /**
     * Construct a new signature with the given return value, argument types,
     * and argument names.
     *
     * @param retval   the return value for the new signature
     * @param firstArg the first argument type, often the receiver of an instance method
     * @param restArgs the remaining argument types for the new signature
     * @param argNames the argument names for the new signature
     */
    Signature(Class retval, Class firstArg, Class[] restArgs, String... argNames) {
        this(MethodType.methodType(retval, firstArg, restArgs), argNames);
    }


    /**
     * Construct a new signature with the given method type and argument names.
     *
     * @param methodType the method type for the new signature
     * @param argNames   the argument names for the new signature
     */
    Signature(MethodType methodType, String... argNames) {
        assert methodType.parameterCount() == argNames.length : "arg name count " + argNames.length + " does not match parameter count " + methodType.parameterCount();
        this.methodType = methodType;
        this.argNames = argNames;
    }


    /**
     * Construct a new signature with the given method type and argument names.
     *
     * @param methodType the method type for the new signature
     * @param firstName  the first argument name for the new signature; for eventual instance methods, it can be "this"
     * @param restNames  the remaining argument names for the new signature
     */
    Signature(MethodType methodType, String firstName, String... restNames) {
        assert methodType.parameterCount() == (restNames.length + 1) : "arg name count " + (restNames.length + 1) + " does not match parameter count " + methodType.parameterCount();
        this.methodType = methodType;
        this.argNames = new String[restNames.length + 1];
        this.argNames[0] = firstName;
        System.arraycopy(restNames, 0, this.argNames, 1, restNames.length);
    }

    /**
     * Produce a human-readable representation of this signature. This
     * representation uses Class#getSimpleName to improve readability.
     *
     * @return a human-readable representation of the signature
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < argNames.length; i++) {
            sb.append(methodType.parameterType(i).getSimpleName()).append(' ').append(argNames[i]);
            if (i + 1 < argNames.length) {
                sb.append(", ");
            }
        }
        sb.append(")").append(methodType.returnType().getSimpleName());
        return sb.toString();
    }

    /**
     * Create a new signature returning the given type.
     *
     * @param retval the return type for the new signature
     * @return the new signature
     */
    public static Signature returning(Class retval) {
        Signature sig = new Signature(retval);
        return sig;
    }

    /**
     * Create a new signature based on the given return value, argument types, and argument names
     *
     * @param retval the type of the return value
     * @param argTypes the types of the arguments
     * @param argNames the names of the arguments
     * @return a new Signature
     */
    public static Signature from(Class retval, Class[] argTypes, String... argNames) {
        assert argTypes.length == argNames.length;

        return new Signature(retval, argTypes, argNames);
    }

    /**
     * Create a new signature based on this one with a different return type.
     *
     * @param retval the class for the new signature's return type
     * @return the new signature with modified return value
     */
    public Signature changeReturn(Class retval) {
        return new Signature(methodType.changeReturnType(retval), argNames);
    }

    /**
     * Produce a new signature based on this one with a different return type.
     *
     * @param retval the new return type for the new signature
     * @return a new signature with the added argument
     */
    public Signature asFold(Class retval) {
        return new Signature(methodType.changeReturnType(retval), argNames);
    }

    /**
     * Append an argument (name + type) to the signature.
     *
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature with the added arguments
     */
    public Signature appendArg(String name, Class type) {
        String[] newArgNames = new String[argNames.length + 1];
        System.arraycopy(argNames, 0, newArgNames, 0, argNames.length);
        newArgNames[argNames.length] = name;
        MethodType newMethodType = methodType.appendParameterTypes(type);
        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Append an argument (name + type) to the signature.
     *
     * @param names the names of the arguments
     * @param types the types of the argument
     * @return a new signature with the added arguments
     */
    public Signature appendArgs(String[] names, Class... types) {
        assert names.length == types.length : "names and types must be of the same length";

        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(argNames, 0, newArgNames, 0, argNames.length);
        System.arraycopy(names, 0, newArgNames, argNames.length, names.length);
        MethodType newMethodType = methodType.appendParameterTypes(types);
        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Prepend an argument (name + type) to the signature.
     *
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature with the added arguments
     */
    public Signature prependArg(String name, Class type) {
        String[] newArgNames = new String[argNames.length + 1];
        System.arraycopy(argNames, 0, newArgNames, 1, argNames.length);
        newArgNames[0] = name;
        MethodType newMethodType = methodType.insertParameterTypes(0, type);
        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Prepend arguments (names + types) to the signature.
     *
     * @param names the names of the arguments
     * @param types the types of the arguments
     * @return a new signature with the added arguments
     */
    public Signature prependArgs(String[] names, Class... types) {
        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(argNames, 0, newArgNames, names.length, argNames.length);
        System.arraycopy(names, 0, newArgNames, 0, names.length);
        MethodType newMethodType = methodType.insertParameterTypes(0, types);
        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Insert an argument (name + type) into the signature.
     *
     * @param index the index at which to insert
     * @param name  the name of the new argument
     * @param type  the type of the new argument
     * @return a new signature with the added arguments
     */
    public Signature insertArg(int index, String name, Class type) {
        return insertArgs(index, new String[]{name}, new Class[]{type});
    }

    /**
     * Insert an argument (name + type) into the signature before the argument
     * with the given name.
     *
     * @param beforeName the name of the argument before which to insert
     * @param name       the name of the new argument
     * @param type       the type of the new argument
     * @return a new signature with the added arguments
     */
    public Signature insertArg(String beforeName, String name, Class type) {
        return insertArgs(argOffset(beforeName), new String[]{name}, new Class[]{type});
    }

    /**
     * Insert arguments (names + types) into the signature.
     *
     * @param index the index at which to insert
     * @param names the names of the new arguments
     * @param types the types of the new arguments
     * @return a new signature with the added arguments
     */
    public Signature insertArgs(int index, String[] names, Class... types) {
        assert names.length == types.length : "names and types must be of the same length";

        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(names, 0, newArgNames, index, names.length);
        if (index != 0) System.arraycopy(argNames, 0, newArgNames, 0, index);
        if (argNames.length - index != 0)
            System.arraycopy(argNames, index, newArgNames, index + names.length, argNames.length - index);

        MethodType newMethodType = methodType.insertParameterTypes(index, types);

        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Insert arguments (names + types) into the signature before the argument
     * with the given name.
     *
     * @param beforeName the name of the argument before which to insert
     * @param names      the names of the new arguments
     * @param types      the types of the new arguments
     * @return a new Signature with the added arguments
     */
    public Signature insertArgs(String beforeName, String[] names, Class... types) {
        return insertArgs(argOffset(beforeName), names, types);
    }

    /**
     * Drops the first argument with the given name.
     *
     * @param name the name of the argument to drop
     * @return a new signature
     */
    public Signature dropArg(String name) {
        String[] newArgNames = new String[argNames.length - 1];
        MethodType newType = methodType;

        for (int i = 0, j = 0; i < argNames.length; i++) {
            if (argNames[i].equals(name)) {
                newType = newType.dropParameterTypes(j, j + 1);
                continue;
            }
            newArgNames[j++] = argNames[i];
        }

        if (newType == null) {
            // arg name not found; should we error?
            return this;
        }

        return new Signature(newType, newArgNames);
    }

    /**
     * Drops the argument at the given index.
     *
     * @param index the index of the argument to drop
     * @return a new signature
     */
    public Signature dropArg(int index) {
        assert index < argNames.length;

        String[] newArgNames = new String[argNames.length - 1];
        if (index > 0) System.arraycopy(argNames, 0, newArgNames, 0, index);
        if (index < argNames.length - 1)
            System.arraycopy(argNames, index + 1, newArgNames, index, argNames.length - (index + 1));

        MethodType newType = methodType.dropParameterTypes(index, index + 1);

        return new Signature(newType, newArgNames);
    }

    /**
     * Drop the last argument from this signature.
     *
     * @return a new signature
     */
    public Signature dropLast() {
        return dropLast(1);
    }

    /**
     * Drop the specified number of last arguments from this signature.
     *
     * @param n number of arguments to drop
     * @return a new signature
     */
    public Signature dropLast(int n) {
        return new Signature(
                methodType.dropParameterTypes(methodType.parameterCount() - n, methodType.parameterCount()),
                Arrays.copyOfRange(argNames, 0, argNames.length - n));
    }

    /**
     * Drop the first argument from this signature.
     *
     * @return a new signature
     */
    public Signature dropFirst() {
        return dropFirst(1);
    }

    /**
     * Drop the specified number of first arguments from this signature.
     *
     * @param n number of arguments to drop
     * @return a new signature
     */
    public Signature dropFirst(int n) {
        return new Signature(
                methodType.dropParameterTypes(0, n),
                Arrays.copyOfRange(argNames, n, argNames.length));
    }

    /**
     * Replace the named argument with a new name and type.
     *
     * @param oldName the old name of the argument
     * @param newName the new name of the argument; can be the same as old
     * @param newType the new type of the argument; can be the same as old
     * @return a new signature with the modified argument
     */
    public Signature replaceArg(String oldName, String newName, Class newType) {
        int offset = argOffset(oldName);
        String[] newArgNames = argNames;

        if (!oldName.equals(newName)) {
            newArgNames = Arrays.copyOf(argNames, argNames.length);
            newArgNames[offset] = newName;
        }

        Class oldType = methodType.parameterType(offset);
        MethodType newMethodType = methodType;

        if (!oldType.equals(newType)) newMethodType = methodType.changeParameterType(offset, newType);

        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Spread the trailing [] argument into its component type assigning given names.
     *
     * @param names names to use for the decomposed arguments
     * @param types types to use for the decomposed arguments
     * @return a new signature with decomposed arguments in place of the trailing array
     */
    public Signature spread(String[] names, Class... types) {
        assert names.length == types.length : "names and types must be of the same length";

        String[] newArgNames = new String[argNames.length - 1 + names.length];
        System.arraycopy(names, 0, newArgNames, newArgNames.length - names.length, names.length);
        System.arraycopy(argNames, 0, newArgNames, 0, argNames.length - 1);

        MethodType newMethodType = methodType
                .dropParameterTypes(methodType.parameterCount() - 1, methodType.parameterCount())
                .appendParameterTypes(types);

        return new Signature(newMethodType, newArgNames);
    }

    /**
     * Spread the trailing [] argument into its component type assigning given names.
     *
     * @param names names to use for the decomposed arguments
     * @return a new signature with decomposed arguments in place of the trailing array
     */
    public Signature spread(String... names) {
        Class aryType = lastArgType();

        assert lastArgType().isArray();

        Class[] newTypes = new Class[names.length];
        Arrays.fill(newTypes, aryType.getComponentType());

        return spread(names, newTypes);
    }

    /**
     * Spread the trailing [] argument into its component type assigning given names.
     *
     * @param baseName base name of the spread arguments
     * @param count number of arguments into which the last argument will decompose
     * @return a new signature with decomposed arguments in place of the trailing array
     */
    public Signature spread(String baseName, int count) {
        String[] spreadNames = new String[count];

        for (int i = 0; i < count; i++) spreadNames[i] = baseName + i;

        return spread(spreadNames);
    }

    /**
     * Collect sequential arguments matching pattern into an array. They must have the same type.
     *
     * @param newName the name of the new array argument
     * @param oldPattern the pattern of arguments to collect
     * @return a new signature with an array argument where the collected arguments were
     */
    public Signature collect(String newName, String oldPattern) {
        int start = -1;
        int newCount = 0;
        int gatherCount = 0;
        Class type = null;
        Pattern pattern = Pattern.compile(oldPattern);

        MethodType newType = type();

        for (int i = 0; i < argNames.length; i++) {
            if (pattern.matcher(argName(i)).matches()) {
                gatherCount++;
                newType = newType.dropParameterTypes(newCount, newCount + 1);
                Class argType = argType(i);
                if (start == -1) start = i;
                if (type == null) {
                    type = argType;
                } else {
                    if (argType != type) {
                        throw new InvalidTransformException("arguments matching " + pattern + " are not all of the same type");
                    }
                }
            } else {
                newCount++;
            }
        }

        if (start != -1) {
            String[] newNames = new String[newCount + 1];

            // pre
            System.arraycopy(argNames, 0, newNames, 0, start);

            // vararg
            newNames[start] = newName;
            newType = newType.insertParameterTypes(start, Array.newInstance(type, 0).getClass());

            // post
            if (newCount + 1 > start) { // args not at end
                System.arraycopy(argNames, start + gatherCount, newNames, start + 1, newCount - start);
            }

            return new Signature(newType, newNames);
        }

        return this;
    }

    /**
     * The current java.lang.invoke.MethodType for this Signature.
     *
     * @return the current method type
     */
    public MethodType type() {
        return methodType;
    }

    /**
     * The current argument count.
     *
     * @return argument count of this signature
     */
    public int argCount() {
        return argNames.length;
    }

    /**
     * The current argument names for this signature.
     *
     * @return the current argument names
     */
    public String[] argNames() {
        return argNames;
    }

    /**
     * Retrieve the name of the argument at the given index.
     *
     * @param index the index from which to get the argument name
     * @return the argument name
     */
    public String argName(int index) {
        return argNames[index];
    }

    /**
     * Retrieve the offset of the given argument name in this signature's
     * arguments. If the argument name is not in the argument list, returns -1.
     *
     * @param name the argument name to search for
     * @return the offset at which the argument name was found or -1
     */
    public int argOffset(String name) {
        for (int i = 0; i < argNames.length; i++) {
            if (argNames[i].equals(name)) return i;
        }
        return -1;
    }

    /**
     * Retrieve the offset of the given argument name in this signature's
     * arguments. If the argument name is not in the argument list, returns -1.
     *
     * @param pattern the argument name to search for
     * @return the offset at which the argument name was found or -1
     */
    public int argOffsets(String pattern) {
        for (int i = 0; i < argNames.length; i++) {
            if (Pattern.compile(pattern).matcher(argNames[i]).find()) return i;
        }
        return -1;
    }

    /**
     * Get the first argument name.
     *
     * @return the first argument name
     */
    public String firstArgName() {
        return argNames[0];
    }

    /**
     * Get the last argument name.
     *
     * @return the last argument name
     */
    public String lastArgName() {
        return argNames[argNames.length - 1];
    }

    /**
     * Set the argument name at the given index.
     *
     * @param index the index at which to set the argument name
     * @param name the name to set
     * @return a new signature with the given name at the given index
     */
    public Signature argName(int index, String name) {
        String[] argNames = Arrays.copyOf(argNames(), argNames().length);
        argNames[index] = name;
        return new Signature(type(), argNames);
    }

    /**
     * Get the argument type at the given index.
     *
     * @param index the index from which to get the argument type
     * @return the argument type
     */
    public Class argType(int index) {
        return methodType.parameterType(index);
    }

    /**
     * Get the first argument type.
     *
     * @return the first argument type
     */
    public Class firstArgType() {
        return methodType.parameterType(0);
    }

    /**
     * Get the last argument type.
     *
     * @return the last argument type
     */
    public Class lastArgType() {
        return argType(methodType.parameterCount() - 1);
    }

    /**
     * Set the argument type at the given index.
     *
     * @param index the index at which to set the argument type
     * @param type the type to set
     * @return a new signature with the given type at the given index
     */
    public Signature argType(int index, Class type) {
        return new Signature(type().changeParameterType(index, type), argNames());
    }

    /**
     * Create a new signature containing the same return value as this one, but
     * only the specified arguments.
     *
     * @param permuteArgs the names of the arguments to preserve
     * @return the new signature
     */
    public Signature permute(String... permuteArgs) {
        Pattern[] patterns = new Pattern[permuteArgs.length];
        for (int i = 0; i < permuteArgs.length; i++) patterns[i] = Pattern.compile(permuteArgs[i]);

        List<Class> types = new ArrayList<Class>(argNames.length);
        List<String> names = new ArrayList<String>(argNames.length);

        for (Pattern pattern : patterns) {
            for (int argOffset = 0; argOffset < argNames.length; argOffset++) {
                String arg = argNames[argOffset];
                Matcher matcher = pattern.matcher(arg);
                if (matcher.find()) {
                    types.add(methodType.parameterType(argOffset));
                    names.add(arg);
                }
            }
        }
        return new Signature(MethodType.methodType(methodType.returnType(), types.toArray(new Class[0])), names.toArray(new String[0]));
    }

    /**
     * Create a new signature containing the same return value as this one, but
     * omitting the specified arguments. Blacklisting to #permute's whitelisting.
     *
     * @param excludeArgs the names of the arguments to exclude
     * @return the new signature
     */
    public Signature exclude(String... excludeArgs) {
        Pattern[] patterns = new Pattern[excludeArgs.length];
        for (int i = 0; i < excludeArgs.length; i++) patterns[i] = Pattern.compile(excludeArgs[i]);

        List<Class> types = new ArrayList<Class>(argNames.length);
        List<String> names = new ArrayList<String>(argNames.length);

        OUTER:
        for (int argOffset = 0; argOffset < argNames.length; argOffset++) {
            String arg = argNames[argOffset];
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(arg);
                if (matcher.find()) continue OUTER;
            }

            // no matches, include
            types.add(methodType.parameterType(argOffset));
            names.add(arg);
        }
        return new Signature(MethodType.methodType(methodType.returnType(), types.toArray(new Class[0])), names.toArray(new String[0]));
    }

    /**
     * Produce a method handle permuting the arguments in this signature using
     * the given permute arguments and targeting the given java.lang.invoke.MethodHandle.
     *
     * Example:
     *
     * <pre>
     * Signature sig = Signature.returning(String.class).appendArg("a", int.class).appendArg("b", int.class);
     * MethodHandle handle = handleThatTakesOneInt();
     * MethodHandle newHandle = sig.permuteTo(handle, "b");
     * </pre>
     *
     * @param target      the method handle to target
     * @param permuteArgs the arguments to permute
     * @return a new handle that permutes appropriate positions based on the
     * given permute args
     */
    public MethodHandle permuteWith(MethodHandle target, String... permuteArgs) {
        return MethodHandles.permuteArguments(target, methodType, to(permute(permuteArgs)));
    }

    /**
     * Produce a new SmartHandle by permuting this Signature's arguments to the
     * Signature of a target SmartHandle. The new SmartHandle's signature will
     * match this one, permuting those arguments and invoking the target handle.
     *
     * @param target the SmartHandle to use as a permutation target
     * @return a new SmartHandle that permutes this Signature's args into a call
     * to the target SmartHandle.
     * @see Signature#permuteWith(java.lang.invoke.MethodHandle, java.lang.String[])
     */
    public SmartHandle permuteWith(SmartHandle target) {
        String[] argNames = target.signature().argNames();
        return new SmartHandle(this, permuteWith(target.handle(), argNames));
    }

    /**
     * Generate an array of argument offsets based on permuting this signature
     * to the given signature.
     *
     * @param other the signature to target
     * @return an array of argument offsets that will permute to the given
     * signature
     */
    public int[] to(Signature other) {
        return nonMatchingTo(other.argNames);
    }

    /**
     * Generate an array of argument offsets based on permuting this signature
     * to the given signature. Repeats are permitted, and the patterns will be
     * matched against actual argument names using regex matching.
     *
     * @param otherArgPatterns the argument name patterns to permute
     * @return an array of argument offsets that will permute to the matching
     * argument names
     */
    public int[] to(String... otherArgPatterns) {
        return to(permute(otherArgPatterns));
    }

    private int[] nonMatchingTo(String... otherArgNames) {
        int[] offsets = new int[otherArgNames.length];
        int i = 0;
        for (String arg : otherArgNames) {
            int pos = -1;
            for (int offset = 0; offset < argNames.length; offset++) {
                if (argNames[offset].equals(arg)) {
                    pos = offset;
                    break;
                }
            }
            assert pos >= 0 : "argument not found: \"" + arg + "\"";
            offsets[i++] = pos;
        }
        return offsets;
    }

}
