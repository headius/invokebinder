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
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * @param retval the return value for the new signature
     * @param argTypes the argument types for the new signature
     * @param argNames the argument names for the new signature
     */
    Signature(Class retval, Class[] argTypes, String... argNames) {
        this(MethodType.methodType(retval, argTypes), argNames);
    }


    /**
     * Construct a new signature with the given method type and argument names.
     * 
     * @param methodType the method type for the new signature
     * @param argNames the argument names for the new signature
     */
    Signature(MethodType methodType, String... argNames) {
        assert methodType.parameterCount() == argNames.length : "arg name count " + argNames.length + " does not match parameter count " + methodType.parameterCount();
        this.methodType = methodType;
        this.argNames = argNames;
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
     * Create a new signature based on this one with a different return type.
     * 
     * @param retval the class for the new signature's return type
     * @return the new signature
     */
    public Signature changeReturn(Class retval) {
        return new Signature(methodType.changeReturnType(retval), argNames);
    }

    /**
     * Produce a new signature based on this one with a different return type.
     * 
     * @param retval the new return type for the new signature
     * @return the new signature
     */
    public Signature asFold(Class retval) {
        return new Signature(methodType.changeReturnType(retval), argNames);
    }

    /**
     * Append an argument (name + type) to the signature.
     * 
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature
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
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature
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
     * @return a new signature
     */
    public Signature prependArg(String name, Class type) {
        String[] newArgNames = new String[argNames.length + 1];
        System.arraycopy(argNames, 0, newArgNames, 1, argNames.length);
        newArgNames[0] = name;
        MethodType newMethodType = methodType.insertParameterTypes(0, type);
        return new Signature(newMethodType, newArgNames);
    }
    
    /**
     * Prepend an argument (name + type) to the signature.
     * 
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature
     */
    public Signature prependArgs(String[] names, Class[] types) {
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
     * @param name the name of the argument
     * @param type the type of the argument
     * @return a new signature
     */
    public Signature insertArg(int index, String name, Class type) {
        return insertArgs(index, new String[]{name}, new Class[]{type});
    }
    
    /**
     * Insert arguments (names + types) into the signature.
     * 
     * @param index the index at which to insert
     * @param names the names of the argument
     * @param types the types of the argument
     * @return a new signature
     */
    public Signature insertArgs(int index, String[] names, Class... types) {
        assert names.length == types.length : "names and types must be of the same length";
        
        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(names, 0, newArgNames, index, names.length);
        if (index != 0) System.arraycopy(argNames, 0, newArgNames, 0, index);
        if (argNames.length - index != 0) System.arraycopy(argNames, index, newArgNames, index + names.length, argNames.length - index);
        
        MethodType newMethodType = methodType.insertParameterTypes(index, types);
        
        return new Signature(newMethodType, newArgNames);
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
                newType = newType.dropParameterTypes(j, j+1);
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
     * Drop the last argument from this signature.
     * 
     * @return a new signature
     */
    public Signature dropLast() {
        return new Signature(
                methodType.dropParameterTypes(methodType.parameterCount() - 1, methodType.parameterCount()),
                Arrays.copyOfRange(argNames, 0, argNames.length - 1));
    }
    
    /**
     * Drop the first argument from this signature.
     * 
     * @return a new signature
     */
    public Signature dropFirst() {
        return new Signature(
                methodType.dropParameterTypes(0, 1),
                Arrays.copyOfRange(argNames, 1, argNames.length));
    }
    
    /**
     * Spread the trailing [] argument into the given argument types
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
     */
    public Signature spread(String baseName, int count) {
        String[] spreadNames = new String[count];
        
        for (int i = 0; i < count; i++) spreadNames[i] = baseName + i;
        
        return spread(spreadNames);
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
     * Create a new signature containing the same return value as this one, but
     * only the specified arguments.
     * 
     * @param permuteArgs the names of the arguments to preserve
     * @return the new signature
     */
    public Signature permute(String... permuteArgs) {
        List<Class> types = new ArrayList<Class>(argNames.length);
        List<String> names = new ArrayList<String>(argNames.length);
        for (String permuteArg : permuteArgs) {
            Pattern pattern = Pattern.compile(permuteArg);
            for (int argOffset = 0; argOffset < argNames.length; argOffset++) {
                String arg = argNames[argOffset];
                if (pattern.matcher(arg).find()) {
                    types.add(methodType.parameterType(argOffset));
                    names.add(argNames[argOffset]);
                }
            }
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
     * @param target the method handle to target
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
