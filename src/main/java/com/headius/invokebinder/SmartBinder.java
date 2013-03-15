/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.headius.invokebinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

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

    public static SmartBinder from(Signature inbound) {
        return new SmartBinder(inbound, Binder.from(inbound.methodType()));
    }

    public static SmartBinder from(Lookup lookup, Signature inbound) {
        return new SmartBinder(inbound, Binder.from(lookup, inbound.methodType()));
    }

    public SmartBinder fold(String newName, MethodHandle folder) {
        return new SmartBinder(signature.prependArg(newName, folder.type().returnType()), binder.fold(folder));
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

    public SmartBinder cast(Signature target) {
        return new SmartBinder(target, binder.cast(target.methodType()));
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
    
    public SmartHandle invoke(MethodHandle target) {
        return new SmartHandle(signature, binder.invoke(target));
    }
    
}
