/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.headius.invoke.binder;

import java.dyn.MethodHandle;
import java.dyn.MethodHandles;
import java.dyn.MethodType;
import junit.framework.TestCase;

/**
 *
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

    public void testInsert() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class)
                .insert(1, "world")
                .invoke(target);
        
        assertEquals(MethodType.methodType(String.class, String.class), handle.type());
        assertEquals("Hello, world", handle.invokeWithArguments("Hello, "));
    }

    public void testDropInsert() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, String.class, Object.class)
                .drop(1)
                .insert(1, "world")
                .invoke(target);
        
        assertEquals(MethodType.methodType(String.class, String.class, Object.class), handle.type());
        assertEquals("Hello, world", handle.invokeWithArguments("Hello, ", new Object()));
    }
    
    public void testConvert() throws Throwable {
        MethodHandle target = mixedHandle();
        MethodHandle handle = Binder
                .from(String.class, Object.class, Integer.class, Float.class)
                .convert(target.type())
                .invoke(target);
        
        assertEquals(MethodType.methodType(String.class, Object.class, Integer.class, Float.class), handle.type());
        // does not seem to be doing JLS conversion from float to double; see below
        assertEquals(null, handle.invokeWithArguments("foo", 5, 5.0f));
    }
    
    public void testDropReorder() throws Throwable {
        MethodHandle target = concatHandle();
        MethodHandle handle = Binder
                .from(String.class, Integer.class, Float.class, String.class)
                .drop(0, 2)
                .reorder(0, 0)
                .invoke(target);
        
        assertEquals(MethodType.methodType(String.class, Integer.class, Float.class, String.class), handle.type());
        assertEquals("foofoo", handle.invokeWithArguments((Integer)0, (Float)0.0, "foo"));
    }
    
    public static MethodHandle concatHandle() throws Exception {
        return MethodHandles.lookup().findStatic(BinderTest.class, "concat", MethodType.methodType(String.class, String.class, String.class));
    }
    
    public static String concat(String a, String b) {
        return a + b;
    }
    
    public static MethodHandle mixedHandle() throws Exception {
//        return MethodHandles.lookup().findStatic(BinderTest.class, "mixed", MethodType.methodType(void.class, String.class, int.class, double.class));
        return MethodHandles.lookup().findStatic(BinderTest.class, "mixed", MethodType.methodType(void.class, String.class, int.class, float.class));
    }
    
//    public static void mixed(String a, int b, double c) {
//    }
    
    public static void mixed(String a, int b, float c) {
    }
}
