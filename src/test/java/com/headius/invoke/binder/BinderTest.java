/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.headius.invoke.binder;

import junit.framework.TestCase;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * @author headius
 */
public class BinderTest extends TestCase {

    public BinderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInvoke() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    public void testInvokeReflected() throws Throwable {
        Method target = BinderTest.class.getMethod("concatStatic", String.class, String.class);
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invoke(MethodHandles.lookup(), target);

        assertEquals(MethodType.methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    public void testInvokeStatic() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeStatic(MethodHandles.lookup(), BinderTest.class, "concatStatic");

        assertEquals(MethodType.methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    public void testInvokeVirtual() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, BinderTest.class, String.class, String.class)
                .invokeVirtual(MethodHandles.lookup(), BinderTest.class, "concatVirtual");

        assertEquals(MethodType.methodType(String.class, BinderTest.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(this, "Hello, ", "world"));
    }

    public void testInvokeConstructor() throws Throwable {
        MethodHandle handle = Binder
                .from(Constructable.class, String.class, String.class)
                .invokeConstructor(MethodHandles.lookup(), Constructable.class);

        assertEquals(MethodType.methodType(Constructable.class, String.class, String.class), handle.type());
        assertEquals(new Constructable("foo", "bar"), (Constructable) handle.invokeExact("foo", "bar"));
    }

    public void testInsert() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .insert(1, "world")
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, "));
    }

    public void testDropInsert() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .drop(1)
                .insert(1, "world")
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));
    }

    public void testConvert() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, Integer.class, Float.class)
                .convert(target.type())
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Object.class, Integer.class, Float.class), handle.type());
        assertEquals(null, (String) handle.invokeExact((Object) "foo", (Integer) 5, (Float) 5.0f));
    }

    public void testConvert2() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, Integer.class, Float.class)
                .convert(target.type().returnType(), target.type().parameterArray())
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Object.class, Integer.class, Float.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (Integer)5, (Float)5.0f));
    }

    public void testCast() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, byte.class, int.class)
                .cast(target.type())
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Object.class, byte.class, int.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (byte)5, 5));
    }

    public void testCast2() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, byte.class, int.class)
                .cast(target.type().returnType(), target.type().parameterArray())
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Object.class, byte.class, int.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (byte)5, 5));
    }

    public void testDropReorder() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Integer.class, Float.class, String.class)
                .drop(0, 2)
                .permute(0, 0)
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Integer.class, Float.class, String.class), handle.type());
        assertEquals("foofoo", (String)handle.invokeExact((Integer) 0, (Float) 0.0f, "foo"));
    }
    
    public void testSpread() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Object[].class)
                .spread(String.class, String.class)
                .invoke(target);
        
        assertEquals(MethodType.methodType(String.class, Object[].class), handle.type());
        assertEquals("foobar", (String)handle.invokeExact(new Object[] {"foo", "bar"}));
    }
    
    public void testConstant() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .constant("hello");

        assertEquals(MethodType.methodType(String.class), handle.type());
        assertEquals("hello", (String)handle.invokeExact());
    }

    public void testConstant2() throws Throwable {
        MethodHandle handle = Binder
                .from(Object.class)
                .constant("hello");

        assertEquals(MethodType.methodType(Object.class), handle.type());
        assertEquals("hello", (Object)handle.invokeExact());
    }
    
    public void testFold() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle fold = Binder
                .from(String.class, String.class)
                .drop(0)
                .constant("yahoo");
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .fold(fold)
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class), handle.type());
        assertEquals("yahoofoo", (String)handle.invokeExact("foo"));
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MethodHandle concatHandle() throws Exception {
        return MethodHandles.lookup().findStatic(BinderTest.class, "concatStatic", MethodType.methodType(String.class, String.class, String.class));
    }

    public static String concatStatic(String a, String b) {
        return a + b;
    }

    public String concatVirtual(String a, String b) {
        return a + b;
    }

    /**
     * Represents a constructable object that's always equal to other constructables.
     */
    public static class Constructable {
        private final String a, b;
        public Constructable(String a, String b) {
            this.a = a;
            this.b = b;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Constructable)) return false;
            Constructable c = (Constructable)other;
            return a.equals(c.a) && b.equals(c.b);
        }
    }

    public static MethodHandle mixedHandle() throws Exception {
        return MethodHandles.lookup().findStatic(BinderTest.class, "mixed", MethodType.methodType(void.class, String.class, int.class, float.class));
    }

    public static void mixed(String a, int b, float c) {
    }
}
