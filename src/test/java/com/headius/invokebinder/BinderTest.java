package com.headius.invokebinder;

import org.junit.Test;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;

/**
 * @author headius
 */
public class BinderTest {
    private static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public void testFrom() throws Throwable {
        MethodHandle target = Subjects.concatHandle();

        Binder binder1 = Binder
                .from(String.class, String.class, Object.class)
                .drop(1);

        MethodHandle handle = Binder
                .from(binder1)
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));
    }

    @Test
    public void testInsertPrimitive() throws Throwable {
        Binder b1 = Binder
                .from(void.class)
                .insert(0, true);
        assertEquals(methodType(void.class, boolean.class), b1.type());
        Binder b2 = Binder
                .from(void.class)
                .insert(0, (byte)1);
        assertEquals(methodType(void.class, byte.class), b2.type());
        Binder b3 = Binder
                .from(void.class)
                .insert(0, (short)1);
        assertEquals(methodType(void.class, short.class), b3.type());
        Binder b4 = Binder
                .from(void.class)
                .insert(0, (char)1);
        assertEquals(methodType(void.class, char.class), b4.type());
        Binder b5 = Binder
                .from(void.class)
                .insert(0, 1);
        assertEquals(methodType(void.class, int.class), b5.type());
        Binder b6 = Binder
                .from(void.class)
                .insert(0, 1L);
        assertEquals(methodType(void.class, long.class), b6.type());
        Binder b7 = Binder
                .from(void.class)
                .insert(0, 1.0F);
        assertEquals(methodType(void.class, float.class), b7.type());
        Binder b8 = Binder
                .from(void.class)
                .insert(0, 1.0);
        assertEquals(methodType(void.class, double.class), b8.type());

        MethodHandle target = intLongHandle();

        MethodHandle handle = Binder
                .from(String.class)
                .insert(0, new Class[]{int.class, long.class}, 1, 1L)
                .invoke(target);

        assertEquals(methodType(String.class), handle.type());
        assertEquals("intLong ok", (String) handle.invokeExact());
    }

    @Test
    public void testTo() throws Throwable {
        Binder otherBinder = Binder
                .from(String.class, String.class, int.class)
                .drop(1)
                .insert(1, ", world");
        
        Binder thisBinder = Binder
                .from(String.class)
                .insert(0, "Hello")
                .insert(1, 1);
        
        Binder newBinder = thisBinder.to(otherBinder);
        
        assertEquals(methodType(String.class, String.class, String.class), otherBinder.type());
        assertEquals(methodType(String.class, String.class, int.class), thisBinder.type());
        assertEquals(methodType(String.class, String.class, String.class), newBinder.type());
        
        MethodHandle target = newBinder.invoke(Subjects.concatHandle());
        
        assertEquals("Hello, world", (String)target.invokeExact());
    }

    @Test
    public void testType() throws Throwable {
        Binder binder = Binder
                .from(String.class, String.class, Integer.class);

        assertEquals(methodType(String.class, String.class, Integer.class), binder.type());

        binder = binder
                .drop(1);

        assertEquals(methodType(String.class, String.class), binder.type());
    }

    @Test
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

    @Test
    public void testInsert() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, "));

        MethodHandle target2 = Subjects.concatCharSequenceHandle();
        MethodHandle handle2 = Binder
                .from(String.class, String.class)
                .insert(1, CharSequence.class, "world")
                .invoke(target2);

        assertEquals(methodType(String.class, String.class), handle2.type());
        assertEquals("Hello, world", (String) handle2.invokeExact("Hello, "));
    }

    @Test
    public void testAppend() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .append("world")
                .drop(1)
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));

        MethodHandle target2 = Subjects.concatCharSequenceHandle();
        MethodHandle handle2 = Binder
                .from(String.class, String.class, Object.class)
                .append(CharSequence.class, "world")
                .drop(1)
                .invoke(target2);

        assertEquals(methodType(String.class, String.class, Object.class), handle2.type());
        assertEquals("Hello, world", (String) handle2.invokeExact("Hello, ", new Object()));
    }

    @Test
    public void testPrepend() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, String.class)
                .prepend("Hello, ")
                .drop(1)
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(new Object(), "world"));

        MethodHandle target2 = Subjects.concatHandle();
        MethodHandle handle2 = Binder
                .from(String.class, Object.class, String.class)
                .prepend(String.class, "Hello, ")
                .drop(1)
                .invoke(target2);

        assertEquals(methodType(String.class, Object.class, String.class), handle2.type());
        assertEquals("Hello, world", (String) handle2.invokeExact(new Object(), "world"));
    }

    @Test
    public void testDropInsert() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .drop(1)
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));
    }
    
    @Test
    public void testDropLast() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .dropLast()
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));

        handle = Binder
                .from(String.class, String.class, Object.class, double.class)
                .dropLast(2)
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class, double.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object(), 1.0));
    }

    @Test
    public void testDropFirst() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, String.class)
                .dropFirst()
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(new Object(), "Hello, "));

        handle = Binder
                .from(String.class, Object.class, double.class, String.class)
                .dropFirst(2)
                .insert(1, "world")
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, double.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(new Object(), 1.0, "Hello, "));
    }

    @Test
    public void testDropAll() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .dropAll()
                .insert(0, "Hello, ", "world")
                .invoke(target);

        assertEquals(methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", new Object()));
    }

    @Test
    public void testConvert() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, Integer.class, Float.class)
                .convert(target.type())
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, Integer.class, Float.class), handle.type());
        assertEquals(null, (String) handle.invokeExact((Object) "foo", (Integer) 5, (Float) 5.0f));
    }

    @Test
    public void testConvert2() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, Integer.class, Float.class)
                .convert(target.type().returnType(), target.type().parameterArray())
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, Integer.class, Float.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (Integer)5, (Float)5.0f));
    }

    @Test
    public void testCast() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, byte.class, int.class)
                .cast(target.type())
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, byte.class, int.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (byte)5, 5));
    }

    @Test
    public void testCast2() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, byte.class, int.class)
                .cast(target.type().returnType(), target.type().parameterArray())
                .invoke(target);

        assertEquals(methodType(String.class, Object.class, byte.class, int.class), handle.type());
        assertEquals(null, (String)handle.invokeExact((Object)"foo", (byte)5, 5));
    }

    @Test
    public void testDropReorder() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Integer.class, Float.class, String.class)
                .drop(0, 2)
                .permute(0, 0)
                .invoke(target);

        assertEquals(methodType(String.class, Integer.class, Float.class, String.class), handle.type());
        assertEquals("foofoo", (String)handle.invokeExact((Integer) 0, (Float) 0.0f, "foo"));
    }

    @Test
    public void testSpread() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Object[].class)
                .spread(String.class, String.class)
                .invoke(target);
        
        assertEquals(methodType(String.class, Object[].class), handle.type());
        assertEquals("foobar", (String)handle.invokeExact(new Object[] {"foo", "bar"}));
    }

    @Test
    public void testSpreadCount() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String[].class)
                .spread(2)
                .invoke(target);
        
        assertEquals(methodType(String.class, String[].class), handle.type());
        assertEquals("foobar", (String)handle.invokeExact(new String[] {"foo", "bar"}));
    }

    @Test
    public void testCollect() throws Throwable {
        MethodHandle handle = Binder
                .from(String[].class, String.class, String.class, String.class)
                .collect(1, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "varargs");

        assertEquals(methodType(String[].class, String.class, String.class, String.class), handle.type());
        String[] ary = (String[])handle.invokeExact("one", "two", "three");
        assertEquals(2, ary.length);
        assertEquals("two", ary[0]);
        assertEquals("three", ary[1]);

        MethodHandle handle2 = Binder
                .from(Subjects.StringIntegerIntegerIntegerString.type())
                .collect(1, 3, Integer[].class)
                .invoke(Subjects.StringIntegersStringHandle);

        assertEquals(methodType(String.class, String.class, Integer.class, Integer.class, Integer.class, String.class), handle2.type());
        assertEquals("[foo, [1, 2, 3], bar]", (String)handle2.invokeExact("foo", new Integer(1), new Integer(2), new Integer(3), "bar"));
    }

    @Test
    public void testVarargs() throws Throwable {
        MethodHandle handle = Binder
                .from(String[].class, String.class, String.class, String.class)
                .varargs(1, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "varargs");

        assertEquals(methodType(String[].class, String.class, String.class, String.class), handle.type());
        String[] ary = (String[])handle.invokeExact("one", "two", "three");
        assertEquals(2, ary.length);
        assertEquals("two", ary[0]);
        assertEquals("three", ary[1]);

        // from #2
        MethodHandle foo = Binder.from(methodType(String.class, String.class))
                .varargs(0, Object[].class)
                .invokeStatic(MethodHandles.publicLookup(), getClass(), "varargs");

        assertEquals(foo.invokeWithArguments("value"), "value");
    }

    @Test
    public void testConstant() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .constant("hello");

        assertEquals(methodType(String.class), handle.type());
        assertEquals("hello", (String)handle.invokeExact());
    }

    @Test
    public void testConstant2() throws Throwable {
        MethodHandle handle = Binder
                .from(Object.class)
                .constant("hello");

        assertEquals(methodType(Object.class), handle.type());
        assertEquals("hello", (Object)handle.invokeExact());
    }

    @Test
    public void testIdentity() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .identity();

        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("hello", (String)handle.invokeExact("hello"));
    }

    @Test
    public void testFold() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle fold = Binder
                .from(String.class, String.class)
                .drop(0)
                .constant("yahoo");
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .fold(fold)
                .invoke(target);

        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("yahoofoo", (String)handle.invokeExact("foo"));
    }

    @Test
    public void testFoldStatic() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(LOOKUP, String.class, String.class)
                .foldStatic(BinderTest.class, "alwaysYahooStatic")
                .invoke(target);

        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("yahoofoo", (String)handle.invokeExact("foo"));
    }

    @Test
    public void testFoldVirtual() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(LOOKUP, String.class, String.class)
                .insert(0, this)
                .foldVirtual("alwaysYahooVirtual")
                .drop(1)
                .invoke(target);

        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("yahoofoo", (String)handle.invokeExact("foo"));
    }

    @Test
    public void testFilter() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle filter = LOOKUP.findStatic(BinderTest.class, "addBaz", methodType(String.class, String.class));
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .filter(0, filter, filter)
                .invoke(target);

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("foobazbarbaz", (String)handle.invokeExact("foo", "bar"));
    }

    @Test
    public void testFilterForward() throws Throwable {
        MethodHandle target = LOOKUP.findStatic(Subjects.class, "twoIntsToString", methodType(String.class, int.class, int.class));
        MethodHandle[] filters = {Subjects.nextInt, Subjects.nextInt};
        MethodHandle handle = Binder.from(String.class, int.class, int.class)
                .filterForward(0, filters)
                .invoke(target);

        int first = Subjects.counter.get();

        assertEquals("(" + first + ", " + (first + 1) + ")", (String) handle.invokeExact(0, 0));
    }

    @Test
    public void testInvoke() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invoke(target);

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    @Test
    public void testInvokeReflected() throws Throwable {
        Method target = Subjects.class.getMethod("concatStatic", String.class, String.class);
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invoke(LOOKUP, target);

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    @Test
    public void testInvokeReflected2() throws Throwable {
        Method target = Subjects.class.getMethod("concatStatic", String.class, String.class);
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeQuiet(LOOKUP, target);

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    @Test
    public void testInvokeStatic() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeStatic(LOOKUP, Subjects.class, "concatStatic");

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    @Test
    public void testInvokeStatic2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class, String.class)
                .invokeStaticQuiet(LOOKUP, Subjects.class, "concatStatic");

        assertEquals(methodType(String.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact("Hello, ", "world"));
    }

    @Test
    public void testInvokeVirtual() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, BinderTest.class, String.class, String.class)
                .invokeVirtual(LOOKUP, "concatVirtual");

        assertEquals(methodType(String.class, BinderTest.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(this, "Hello, ", "world"));
    }

    @Test
    public void testInvokeVirtual2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, BinderTest.class, String.class, String.class)
                .invokeVirtualQuiet(LOOKUP, "concatVirtual");

        assertEquals(methodType(String.class, BinderTest.class, String.class, String.class), handle.type());
        assertEquals("Hello, world", (String) handle.invokeExact(this, "Hello, ", "world"));
    }

    @Test
    public void testInvokeConstructor() throws Throwable {
        MethodHandle handle = Binder
                .from(Constructable.class, String.class, String.class)
                .invokeConstructor(LOOKUP, Constructable.class);

        assertEquals(methodType(Constructable.class, String.class, String.class), handle.type());
        assertEquals(new Constructable("foo", "bar"), (Constructable) handle.invokeExact("foo", "bar"));
    }

    @Test
    public void testInvokeConstructor2() throws Throwable {
        MethodHandle handle = Binder
                .from(Constructable.class, String.class, String.class)
                .invokeConstructorQuiet(LOOKUP, Constructable.class);

        assertEquals(methodType(Constructable.class, String.class, String.class), handle.type());
        assertEquals(new Constructable("foo", "bar"), (Constructable) handle.invokeExact("foo", "bar"));
    }

    @Test
    public void testGetField() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(String.class, Fields.class)
                .getField(LOOKUP, "instanceField");
        
        assertEquals(methodType(String.class, Fields.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact(fields));
    }

    @Test
    public void testGetField2() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(String.class, Fields.class)
                .getFieldQuiet(LOOKUP, "instanceField");

        assertEquals(methodType(String.class, Fields.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact(fields));
    }

    @Test
    public void testGetStatic() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .getStatic(LOOKUP, Fields.class, "staticField");

        assertEquals(methodType(String.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact());
    }

    @Test
    public void testGetStatic2() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class)
                .getStaticQuiet(LOOKUP, Fields.class, "staticField");

        assertEquals(methodType(String.class), handle.type());
        assertEquals("initial", (String)handle.invokeExact());
    }

    @Test
    public void testSetField() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(void.class, Fields.class, String.class)
                .setField(LOOKUP, "instanceField");

        assertEquals(methodType(void.class, Fields.class, String.class), handle.type());
        handle.invokeExact(fields, "modified");
        assertEquals("modified", fields.instanceField);
    }

    @Test
    public void testSetField2() throws Throwable {
        Fields fields = new Fields();
        MethodHandle handle = Binder
                .from(void.class, Fields.class, String.class)
                .setFieldQuiet(LOOKUP, "instanceField");

        assertEquals(methodType(void.class, Fields.class, String.class), handle.type());
        handle.invokeExact(fields, "modified");
        assertEquals("modified", fields.instanceField);
    }

    @Test
    public void testSetStatic() throws Throwable {
        try {
            MethodHandle handle = Binder
                    .from(void.class, String.class)
                    .setStatic(LOOKUP, Fields.class, "staticField");

            assertEquals(methodType(void.class, String.class), handle.type());
            handle.invokeExact("modified");
            assertEquals("modified", Fields.staticField);
        } finally {
            Fields.staticField = "initial";
        }
    }

    @Test
    public void testSetStatic2() throws Throwable {
        try {
            MethodHandle handle = Binder
                    .from(void.class, String.class)
                    .setStaticQuiet(LOOKUP, Fields.class, "staticField");

            assertEquals(methodType(void.class, String.class), handle.type());
            handle.invokeExact("modified");
            assertEquals("modified", Fields.staticField);
        } finally {
            Fields.staticField = "initial";
        }
    }
    
    @Test
    public void testNop() throws Throwable {
        MethodHandle handle = Binder
                .from(void.class, int.class, String.class)
                .nop();
        
        assertEquals(methodType(void.class, int.class, String.class), handle.type());
        try {
            handle.invokeExact(1, "foo");
        } catch (Throwable t) {
            assertTrue("should not reach here", false);
        }
    }
    
    @Test
    public void testThrowException() throws Throwable {
        MethodHandle handle = Binder
                .from(void.class, BlahException.class)
                .throwException();
        
        assertEquals(methodType(void.class, BlahException.class), handle.type());
        try {
            handle.invokeExact(new BlahException());
            assertTrue("should not reach here", false);
        } catch (BlahException be) {
        }
    }

    @Test
    public void testTryFinally() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(void.class, String[].class)
                .tryFinally(post)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFoo");

        assertEquals(methodType(void.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        handle.invokeExact(stringAry);
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testTryFinally2() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(void.class, String[].class)
                .tryFinally(post)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFooAndRaise");

        assertEquals(methodType(void.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        try {
            handle.invokeExact(stringAry);
            assertTrue("should not have reached here", false);
        } catch (BlahException re) {
        }
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testTryFinally3() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");
        
        MethodHandle ignoreException = Binder
                .from(void.class, BlahException.class, String[].class)
                .nop();

        MethodHandle handle = Binder
                .from(void.class, String[].class)
                .tryFinally(post)
                .catchException(BlahException.class, ignoreException)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFooAndRaise");

        assertEquals(methodType(void.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        try {
            handle.invokeExact(stringAry);
        } catch (BlahException re) {
            assertTrue("should not have reached here", false);
        }
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testTryFinallyReturn() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(int.class, String[].class)
                .tryFinally(post)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFooReturnInt");

        assertEquals(methodType(int.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        assertEquals(1, (int)handle.invokeExact(stringAry));
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testTryFinallyReturn2() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");

        MethodHandle handle = Binder
                .from(int.class, String[].class)
                .tryFinally(post)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFooReturnIntAndRaise");

        assertEquals(methodType(int.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        try {
            int x = (int)handle.invokeExact(stringAry);
            assertTrue("should not have reached here", false);
        } catch (BlahException re) {
        }
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testTryFinallyReturn3() throws Throwable {
        MethodHandle post = Binder
                .from(void.class, String[].class)
                .invokeStatic(LOOKUP, BinderTest.class, "finallyLogic");

        MethodHandle ignoreException = Binder
                .from(int.class, BlahException.class, String[].class)
                .drop(0, 2)
                .constant(1);

        MethodHandle handle = Binder
                .from(int.class, String[].class)
                .tryFinally(post)
                .catchException(BlahException.class, ignoreException)
                .invokeStatic(LOOKUP, BinderTest.class, "setZeroToFooReturnIntAndRaise");

        assertEquals(methodType(int.class, String[].class), handle.type());
        String[] stringAry = new String[1];
        try {
            assertEquals(1, (int)handle.invokeExact(stringAry));
        } catch (BlahException be) {
            assertTrue("should not have reached here", false);
        }
        assertEquals("foofinally", stringAry[0]);
    }

    @Test
    public void testArraySet() throws Throwable {
        MethodHandle handle = Binder
                .from(void.class, Object[].class, int.class, Object.class)
                .arraySet();

        assertEquals(methodType(void.class, Object[].class, int.class, Object.class), handle.type());
        Object[] ary = new Object[1];
        handle.invokeExact(ary, 0, (Object)"foo");
        assertEquals(ary[0], "foo");
    }

    @Test
    public void testArrayGet() throws Throwable {
        MethodHandle handle = Binder
                .from(Object.class, Object[].class, int.class)
                .arrayGet();

        assertEquals(methodType(Object.class, Object[].class, int.class), handle.type());
        Object[] ary = new Object[] {"foo"};
        assertEquals(handle.invokeExact(ary, 0), "foo");
    }

    public static final VarHandle.AccessMode[] GET_ACCESS_MODES = new VarHandle.AccessMode[]{
            VarHandle.AccessMode.GET,
            VarHandle.AccessMode.GET_VOLATILE,
            VarHandle.AccessMode.GET_ACQUIRE,
            VarHandle.AccessMode.GET_OPAQUE};

    public static final VarHandle.AccessMode[] SET_ACCESS_MODES = new VarHandle.AccessMode[]{
            VarHandle.AccessMode.SET,
            VarHandle.AccessMode.SET_VOLATILE,
            VarHandle.AccessMode.SET_RELEASE,
            VarHandle.AccessMode.SET_OPAQUE};

    @Test
    public void testArrayAccess() throws Throwable {
        for (VarHandle.AccessMode mode : GET_ACCESS_MODES) {
            MethodHandle handle = Binder
                    .from(Object.class, Object[].class, int.class)
                    .arrayAccess(mode);

            assertEquals(methodType(Object.class, Object[].class, int.class), handle.type());
            Object[] ary = new Object[]{"foo"};
            assertEquals(handle.invokeExact(ary, 0), "foo");
        }

        for (VarHandle.AccessMode mode : SET_ACCESS_MODES) {
            MethodHandle handle = Binder
                    .from(void.class, Object[].class, int.class, Object.class)
                    .arrayAccess(mode);

            assertEquals(methodType(void.class, Object[].class, int.class, Object.class), handle.type());
            Object[] ary = new Object[1];
            handle.invokeExact(ary, 0, (Object) "foo");
            assertEquals(ary[0], "foo");
        }
    }
    
    @Test
    public void testBranch() throws Throwable {
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .branch(
                        Binder
                                .from(boolean.class, String.class)
                                .invokeStatic(LOOKUP, BinderTest.class, "isStringFoo"),
                        Binder
                                .from(String.class, String.class)
                                .invokeStatic(LOOKUP, BinderTest.class, "addBar"),
                        Binder
                                .from(String.class, String.class)
                                .invokeStatic(LOOKUP, BinderTest.class, "addBaz")
                );
        
        assertEquals(methodType(String.class, String.class), handle.type());
        assertEquals("foobar", (String)handle.invokeExact("foo"));
        assertEquals("quuxbaz", (String)handle.invokeExact("quux"));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MethodHandle intLongHandle() throws Exception {
        return LOOKUP.findStatic(BinderTest.class, "intLong", methodType(String.class, int.class, long.class));
    }

    public String concatVirtual(String a, String b) {
        return a + b;
    }

    public static boolean isStringFoo(String a) {
        return a.equals("foo");
    }

    public static String addBar(String a) {
        return a + "bar";
    }

    public static String addBaz(String a) {
        return a + "baz";
    }

    public static void setZeroToFoo(String[] ary) {
        ary[0] = "foo";
    }

    public static void setZeroToFooAndRaise(String[] ary) throws BlahException {
        ary[0] = "foo";
        throw new BlahException();
    }

    public static int setZeroToFooReturnInt(String[] ary) {
        ary[0] = "foo";
        return 1;
    }

    public static int setZeroToFooReturnIntAndRaise(String[] ary) throws BlahException {
        ary[0] = "foo";
        throw new BlahException();
    }

    public static void finallyLogic(String[] ary) {
        ary[0] = ary[0] + "finally";
    }

    public static String[] varargs(String arg0, String... args) {
        return args;
    }

    public static String varargs(Object... args)
    {
        return (String) args[0];
    }

    public static String intLong(int a, long b) {
        return "intLong ok";
    }
    
    public static class BlahException extends Exception {}

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
        return LOOKUP.findStatic(BinderTest.class, "mixed", methodType(void.class, String.class, int.class, float.class));
    }

    public static void mixed(String a, int b, float c) {
    }
    
    public static String alwaysYahooStatic(String ignored) {
        return "yahoo";
    }
    
    public String alwaysYahooVirtual(String ignored) {
        return "yahoo";
    }
}
