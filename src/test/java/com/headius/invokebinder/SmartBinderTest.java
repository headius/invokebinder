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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author headius
 */
public class SmartBinderTest {
    
    public SmartBinderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of from method, of class SmartBinder.
     */
    @Test
    public void testFrom_Signature() throws Throwable {
        SmartHandle handle = SmartBinder
                .from(Signature.returning(String.class).appendArg("int", int.class))
                .invoke(stringInt);
        
        assertEquals("42", (String)handle.handle().invokeExact(42));
    }

    @Test
    public void testDrop() throws Throwable {
        Signature oldSig = Subjects.StringIntegerIntegerIntegerString;

        SmartHandle handle = SmartBinder.from(oldSig)
                .drop("b1")
                .drop("b2")
                .drop("b3")
                .insert(1, "bs", new Integer[]{1,2,3})
                .invoke(Subjects.StringIntegersStringHandle);

        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact("foo", new Integer(5), new Integer(6), new Integer(7), "bar"));
    }

    @Test
    public void testCollect() throws Throwable {
        Signature oldSig = Subjects.StringIntegerIntegerIntegerString;

        SmartHandle handle = SmartBinder
                .from(oldSig)
                .collect("bs", "b.*")
                .invoke(Subjects.StringIntegersStringHandle);

        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact("foo", new Integer(1), new Integer(2), new Integer(3), "bar"));
    }

    @Test
    public void testInvokeStatic() throws Throwable {
        SmartHandle handle = SmartBinder
                .from(Subjects.StringIntegersString)
                .invokeStatic(LOOKUP, Subjects.class, "stringIntegersString");

        assertEquals(Subjects.StringIntegersString, handle.signature());
        assertEquals(Subjects.StringIntegersString.type(), handle.handle().type());
        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact("foo", new Integer[]{1,2,3}, "bar"));

        handle = SmartBinder
                .from(Subjects.StringIntegersString)
                .invokeStatic(LOOKUP, Subjects.class, "stringIntegersString");

        assertEquals(Subjects.StringIntegersString, handle.signature());
        assertEquals(Subjects.StringIntegersString.type(), handle.handle().type());
        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact("foo", new Integer[]{1,2,3}, "bar"));
    }

    @Test
    public void testInvokeVirtual() throws Throwable {
        Subjects subjects = new Subjects();
        Signature thisSig = Subjects.StringIntegersString.prependArg("this", Subjects.class);

        SmartHandle handle = SmartBinder
                .from(thisSig)
                .invokeVirtual(LOOKUP, "stringIntegersString2");

        assertEquals(thisSig, handle.signature());
        assertEquals(thisSig.type(), handle.handle().type());
        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact(subjects, "foo", new Integer[]{1,2,3}, "bar"));

        handle = SmartBinder
                .from(thisSig)
                .invokeVirtual(LOOKUP, "stringIntegersString2");

        assertEquals(thisSig, handle.signature());
        assertEquals(thisSig.type(), handle.handle().type());
        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact(subjects, "foo", new Integer[]{1,2,3}, "bar"));
    }

    @Test
    public void testInsert() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        SmartHandle handle = SmartBinder
                .from(String.class, "arg0", String.class)
                .insert(1, "arg1", "world")
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class), handle.signature().type());
        assertEquals("Hello, world", (String) handle.handle().invokeExact("Hello, "));

        MethodHandle target2 = Subjects.concatCharSequenceHandle();
        SmartHandle handle2 = SmartBinder
                .from(String.class, "arg0", String.class)
                .insert(1, "arg1", CharSequence.class, "world")
                .invoke(target2);

        assertEquals(MethodType.methodType(String.class, String.class), handle2.signature().type());
        assertEquals("Hello, world", (String) handle2.handle().invokeExact("Hello, "));
    }

    @Test
    public void testAppend() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        SmartHandle handle = SmartBinder
                .from(String.class, new String[] {"arg0", "arg1"}, String.class, Object.class)
                .append("arg2", "world")
                .drop("arg1")
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, String.class, Object.class), handle.signature().type());
        assertEquals("Hello, world", (String) handle.handle().invokeExact("Hello, ", new Object()));

        MethodHandle target2 = Subjects.concatCharSequenceHandle();
        SmartHandle handle2 = SmartBinder
                .from(String.class, new String[] {"arg0", "arg1"}, String.class, Object.class)
                .append("arg2", CharSequence.class, "world")
                .drop("arg1")
                .invoke(target2);

        assertEquals(MethodType.methodType(String.class, String.class, Object.class), handle2.signature().type());
        assertEquals("Hello, world", (String) handle2.handle().invokeExact("Hello, ", new Object()));
    }

    @Test
    public void testPrepend() throws Throwable {
        MethodHandle target = Subjects.concatHandle();
        SmartHandle handle = SmartBinder
                .from(String.class, new String[]{"arg1", "arg2"}, Object.class, String.class)
                .prepend("arg0", "Hello, ")
                .drop("arg1")
                .invoke(target);

        assertEquals(MethodType.methodType(String.class, Object.class, String.class), handle.signature().type());
        assertEquals("Hello, world", (String) handle.handle().invokeExact(new Object(), "world"));

        MethodHandle target2 = Subjects.concatHandle();
        SmartHandle handle2 = SmartBinder
                .from(String.class, new String[] {"arg1", "arg2"}, Object.class, String.class)
                .prepend("arg0", String.class, "Hello, ")
                .drop("arg1")
                .invoke(target2);

        assertEquals(MethodType.methodType(String.class, Object.class, String.class), handle2.signature().type());
        assertEquals("Hello, world", (String) handle2.handle().invokeExact(new Object(), "world"));
    }

    @Test
    public void testCast() throws Throwable {
        MethodHandle target = LOOKUP.unreflect(String.class.getMethod("split", String.class));
        SmartHandle handle  = SmartBinder
                .from(Object.class, new String[]{"this", "regex"}, Object.class, Object.class)
                .cast(String[].class, String.class, String.class)
                .invoke(target);

        assertArrayEquals(new String[]{"foo", "bar"}, (String[])(Object)handle.handle().invokeExact((Object)"foo,bar", (Object)","));
    }
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    
    private static final MethodHandle stringInt = Binder
            .from(String.class, int.class)
            .invokeStaticQuiet(LOOKUP, SmartBinderTest.class, "stringInt");

    public static String stringInt(int value) {
        return Integer.toString(value);
    }

}