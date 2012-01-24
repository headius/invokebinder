/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.headius.invoke.binder;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

    public void testType() throws Throwable {
        Binder binder = Binder
                .from(String.class, String.class, Integer.class);

        assertEquals(MethodType.methodType(String.class, String.class, Integer.class), binder.type());

        binder = binder
                .drop(1);

        assertEquals(MethodType.methodType(String.class, String.class), binder.type());
    }

    public void testPrintType() throws Throwable {
        Binder binder = Binder
                .from(String.class, String.class, Integer.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        binder.printType(ps);
        assertEquals("(String,Integer)String\n", baos.toString());

        binder = binder
                .drop(1);

        baos = new ByteArrayOutputStream();
        ps = new PrintStream(baos);
        binder.printType(ps);
        assertEquals("(String)String\n", baos.toString());
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

    public void testIdentity() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .identity();

        assertEquals(MethodType.methodType(String.class, String.class), handle.type());
        assertEquals("hello", (String)handle.invokeExact("hello"));
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
    
    public void testFilter() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle filter = MethodHandles.lookup().findStatic(BinderTest.class, "addBaz", MethodType.methodType(String.class, String.class));
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .filter(0, filter, filter)
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class, String.class), handle.type());
        assertEquals("foobazbarbaz", (String)handle.invokeExact("foo", "bar"));
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

    public void testInvokeReflected2() throws Throwable {
        Method target = BinderTest.class.getMethod("concatStatic", String.class, String.class);
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeQuiet(MethodHandles.lookup(), target);

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

    public void testInvokeStatic2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeStaticQuiet(MethodHandles.lookup(), BinderTest.class, "concatStatic");

        assertEquals(MethodType.methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    public void testInvokeVirtual() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, BinderTest.class, String.class, String.class)
                .invokeVirtual(MethodHandles.lookup(), "concatVirtual");

        assertEquals(MethodType.methodType(String.class, BinderTest.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(this, "Hello, ", "world"));
    }

    public void testInvokeVirtual2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, BinderTest.class, String.class, String.class)
                .invokeVirtualQuiet(MethodHandles.lookup(), "concatVirtual");

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

    public void testInvokeConstructor2() throws Throwable {
        MethodHandle handle = Binder
                .from(Constructable.class, String.class, String.class)
                .invokeConstructorQuiet(MethodHandles.lookup(), Constructable.class);

        assertEquals(MethodType.methodType(Constructable.class, String.class, String.class), handle.type());
        assertEquals(new Constructable("foo", "bar"), (Constructable) handle.invokeExact("foo", "bar"));
    }

    public void testGetField() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(String.class, Fields.class)
                .getField(MethodHandles.lookup(), "instanceField");
        
        assertEquals(MethodType.methodType(String.class, Fields.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact(fields));
    }

    public void testGetField2() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(String.class, Fields.class)
                .getFieldQuiet(MethodHandles.lookup(), "instanceField");

        assertEquals(MethodType.methodType(String.class, Fields.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact(fields));
    }

    public void testGetStatic() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .getStatic(MethodHandles.lookup(), Fields.class, "staticField");

        assertEquals(MethodType.methodType(String.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact());
    }

    public void testGetStatic2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .getStaticQuiet(MethodHandles.lookup(), Fields.class, "staticField");

        assertEquals(MethodType.methodType(String.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact());
    }

    public void testSetField() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(void.class, Fields.class, String.class)
                .setField(MethodHandles.lookup(), "instanceField");

        assertEquals(MethodType.methodType(void.class, Fields.class, String.class), handle.type());
        handle.invokeExact(fields, "modified");
        assertEquals("modified", fields.instanceField);
    }

    public void testSetField2() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(void.class, Fields.class, String.class)
                .setFieldQuiet(MethodHandles.lookup(), "instanceField");

        assertEquals(MethodType.methodType(void.class, Fields.class, String.class), handle.type());
        handle.invokeExact(fields, "modified");
        assertEquals("modified", fields.instanceField);
    }

    public void testSetStatic() throws Throwable {
        try {
            MethodHandle handle = Binder
                    .from(void.class, String.class)
                    .setStatic(MethodHandles.lookup(), Fields.class, "staticField");

            assertEquals(MethodType.methodType(void.class, String.class), handle.type());
            handle.invokeExact("modified");
            assertEquals("modified", Fields.staticField);
        } finally {
            Fields.staticField = "initial";
        }
    }

    public void testSetStatic2() throws Throwable {
        try {
            MethodHandle handle = Binder
                    .from(void.class, String.class)
                    .setStaticQuiet(MethodHandles.lookup(), Fields.class, "staticField");

            assertEquals(MethodType.methodType(void.class, String.class), handle.type());
            handle.invokeExact("modified");
            assertEquals("modified", Fields.staticField);
        } finally {
            Fields.staticField = "initial";
        }
    }

    public void testTryFinally() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(MethodHandles.lookup(), BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(void.class, String[].class)
                .tryFinally(post)
                .invokeStatic(MethodHandles.lookup(), BinderTest.class, "setZeroToFoo");

        assertEquals(MethodType.methodType(void.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        handle.invokeExact(stringAry);
        assertEquals("foofinally", stringAry[0]);
    }

    public void testTryFinally2() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(MethodHandles.lookup(), BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(void.class, String[].class)
                .tryFinally(post)
                .invokeStatic(MethodHandles.lookup(), BinderTest.class, "setZeroToFooAndRaise");

        assertEquals(MethodType.methodType(void.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        try {
            handle.invokeExact(stringAry);
            assertTrue("should not have reached here", false);
        } catch (RuntimeException re) {
        }
        assertEquals("foofinally", stringAry[0]);
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

    public static String addBaz(String a) {
        return a + "baz";
    }

    public static void setZeroToFoo(String[] ary) {
        ary[0] = "foo";
    }

    public static void setZeroToFooAndRaise(String[] ary) {
        ary[0] = "foo";
        throw new RuntimeException();
    }

    public static void finallyLogic(String[] ary) {
        ary[0] = ary[0] + "finally";
    }

    public static class Fields {
        public String instanceField = "initial";
        public static String staticField = "initial";
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
