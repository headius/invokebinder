/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.headius.invokebinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Arrays;

/**
 * Maintains both a Binder, for building a series of transformations, and a
 * current Signature that maps symbolic names to arguments.
 * 
 * @author headius
 */
public class SmartBinder {
    private final Signature signature;
    private final Binder binder;

    private SmartBinder(Signature signature, Binder binder) {
        this.signature = signature;
        this.binder = binder;
    }
    
    public Signature signature() {
        return signature;
    }
    
    public Binder binder() {
        return binder;
    }

    public static SmartBinder from(Signature inbound) {
        return new SmartBinder(inbound, Binder.from(inbound.type()));
    }

    public static SmartBinder from(Lookup lookup, Signature inbound) {
        return new SmartBinder(inbound, Binder.from(lookup, inbound.type()));
    }

    public SmartBinder fold(String newName, MethodHandle function) {
        return new SmartBinder(signature.prependArg(newName, function.type().returnType()), binder.fold(function));
    }

    public SmartBinder fold(String newName, SmartHandle function) {
        if (Arrays.equals(signature.argNames(), function.signature().argNames())) {
            return fold(newName, function.handle());
        } else {
            return fold(newName, signature.permuteWith(function).handle());
        }
    }

    public SmartBinder foldVoid(MethodHandle function) {
        return new SmartBinder(signature, binder.foldVoid(function));
    }

    public SmartBinder foldVoid(SmartHandle function) {
        if (Arrays.equals(signature.argNames(), function.signature().argNames())) {
            return foldVoid(function.handle());
        } else {
            return foldVoid(signature.asFold(void.class).permuteWith(function).handle());
        }
    }

    public SmartBinder foldStatic(String newName, Lookup lookup, Class target, String method) {
        Binder newBinder = binder.foldStatic(lookup, target, method);
        return new SmartBinder(signature.prependArg(newName, newBinder.type().parameterType(0)), binder);
    }

    public SmartBinder foldStatic(String newName, Class target, String method) {
        Binder newBinder = binder.foldStatic(target, method);
        return new SmartBinder(signature.prependArg(newName, newBinder.type().parameterType(0)), binder);
    }

    public SmartBinder foldVirtual(String newName, Lookup lookup, String method) {
        Binder newBinder = binder.foldVirtual(lookup, method);
        return new SmartBinder(signature.prependArg(newName, newBinder.type().parameterType(0)), binder);
    }

    public SmartBinder foldVirtual(String newName, String method) {
        Binder newBinder = binder.foldVirtual(method);
        return new SmartBinder(signature.prependArg(newName, newBinder.type().parameterType(0)), binder);
    }

    public SmartBinder permute(Signature target) {
        return new SmartBinder(target, binder.permute(signature.to(target)));
    }

    public SmartBinder permute(String... targetNames) {
        return permute(signature.permute(targetNames));
    }

    /**
     * Spread a trailing Object[] into the specified argument types.
     *
     * @param spreadTypes the types into which to spread the incoming Object[]
     * @return a new Binder
     */
    public SmartBinder spread(String[] spreadNames, Class... spreadTypes) {
        return new SmartBinder(signature.spread(spreadNames, spreadTypes), binder.spread(spreadTypes));
    }
    
    public SmartBinder spread(String baseName, int count) {
        return new SmartBinder(signature.spread(baseName, count), binder.spread(count));
    }
    
    public SmartBinder insert(int index, String name, Object value) {
        return new SmartBinder(signature.insertArg(index, name, value.getClass()), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, boolean value) {
        return new SmartBinder(signature.insertArg(index, name, boolean.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, byte value) {
        return new SmartBinder(signature.insertArg(index, name, byte.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, short value) {
        return new SmartBinder(signature.insertArg(index, name, short.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, char value) {
        return new SmartBinder(signature.insertArg(index, name, char.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, int value) {
        return new SmartBinder(signature.insertArg(index, name, int.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, long value) {
        return new SmartBinder(signature.insertArg(index, name, long.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, float value) {
        return new SmartBinder(signature.insertArg(index, name, float.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String name, double value) {
        return new SmartBinder(signature.insertArg(index, name, double.class), binder.insert(index, value));
    }
    
    public SmartBinder insert(int index, String[] names, Class[] types, Object... values) {
        return new SmartBinder(signature.insertArgs(index, names, types), binder.insert(index, types, values));
    }
    
    public SmartBinder append(String name, Object value) {
        return new SmartBinder(signature.appendArg(name, value.getClass()), binder.append(value));
    }
    
    public SmartBinder append(String name, boolean value) {
        return new SmartBinder(signature.appendArg(name, boolean.class), binder.append(value));
    }
    
    public SmartBinder append(String name, byte value) {
        return new SmartBinder(signature.appendArg(name, byte.class), binder.append(value));
    }
    
    public SmartBinder append(String name, short value) {
        return new SmartBinder(signature.appendArg(name, short.class), binder.append(value));
    }
    
    public SmartBinder append(String name, char value) {
        return new SmartBinder(signature.appendArg(name, char.class), binder.append(value));
    }
    
    public SmartBinder append(String name, int value) {
        return new SmartBinder(signature.appendArg(name, int.class), binder.append(value));
    }
    
    public SmartBinder append(String name, long value) {
        return new SmartBinder(signature.appendArg(name, long.class), binder.append(value));
    }
    
    public SmartBinder append(String name, float value) {
        return new SmartBinder(signature.appendArg(name, float.class), binder.append(value));
    }
    
    public SmartBinder append(String name, double value) {
        return new SmartBinder(signature.appendArg(name, double.class), binder.append(value));
    }
    
    public SmartBinder append(String[] names, Class[] types, Object... values) {
        return new SmartBinder(signature.appendArgs(names, types), binder.append(types, values));
    }
    
    public SmartBinder prepend(String name, Object value) {
        return new SmartBinder(signature.prependArg(name, value.getClass()), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, boolean value) {
        return new SmartBinder(signature.prependArg(name, boolean.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, byte value) {
        return new SmartBinder(signature.prependArg(name, byte.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, short value) {
        return new SmartBinder(signature.prependArg(name, short.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, char value) {
        return new SmartBinder(signature.prependArg(name, char.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, int value) {
        return new SmartBinder(signature.prependArg(name, int.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, long value) {
        return new SmartBinder(signature.prependArg(name, long.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, float value) {
        return new SmartBinder(signature.prependArg(name, float.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String name, double value) {
        return new SmartBinder(signature.prependArg(name, double.class), binder.prepend(value));
    }
    
    public SmartBinder prepend(String[] names, Class[] types, Object... values) {
        return new SmartBinder(signature.prependArgs(names, types), binder.prepend(types, values));
    }

    public SmartBinder cast(Signature target) {
        return new SmartBinder(target, binder.cast(target.type()));
    }

    public SmartBinder cast(Class returnType, Class... argTypes) {
        return new SmartBinder(new Signature(returnType, argTypes, signature.argNames()), binder.cast(returnType, argTypes));
    }

    public SmartHandle invokeVirtualQuiet(Lookup lookup, String name) {
        return new SmartHandle(signature, binder.invokeVirtualQuiet(lookup, name));
    }
    
    public SmartHandle invokeStaticQuiet(Lookup lookup, Class target, String name) {
        return new SmartHandle(signature, binder.invokeStaticQuiet(lookup, target, name));
    }
    
    public SmartHandle invoke(SmartHandle target) {
        return new SmartHandle(signature, binder.invoke(target.handle()));
    }
    
    public SmartHandle invoke(MethodHandle target) {
        return new SmartHandle(signature, binder.invoke(target));
    }
    
    public SmartBinder printSignature() {
        System.out.println(signature.toString());
        return this;
    }
    
}
