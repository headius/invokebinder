/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.headius.invokebinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
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
    public Signature insertArgs(int index, String[] names, Class[] types) {
        assert names.length == types.length : "names and types must be of the same length";
        
        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(names, 0, newArgNames, index, names.length);
        if (index != 0) System.arraycopy(argNames, 0, newArgNames, 0, index);
        if (argNames.length - index != 0) System.arraycopy(argNames, index, newArgNames, index + names.length, argNames.length - index);
        
        MethodType newMethodType = methodType.insertParameterTypes(index, types);
        
        return new Signature(newMethodType, newArgNames);
    }

    /**
     * The current java.lang.invoke.MethodType for this Signature.
     * 
     * @return the current method type
     */
    public MethodType methodType() {
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

    public Signature permute(String... permuteArgs) {
        List<Class> types = new ArrayList<Class>(argNames.length);
        List<String> names = new ArrayList<String>(argNames.length);
        for (String permuteArg : permuteArgs) {
            Pattern pattern = Pattern.compile(permuteArg);
            boolean found = false;
            for (int argOffset = 0; argOffset < argNames.length; argOffset++) {
                String arg = argNames[argOffset];
                if (pattern.matcher(arg).find()) {
                    found = true;
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
     * to the given signature. Repeats are permitted.
     * 
     * @param otherArgPatterns the argument names to permute
     * @return an array of argument offsets that will permute to the given
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
