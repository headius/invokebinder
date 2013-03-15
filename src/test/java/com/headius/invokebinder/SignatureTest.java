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
public class SignatureTest {
    public SignatureTest() {
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
     * Test of toString method, of class Signature.
     */
    @Test
    public void testToString() {
        assertEquals("(Object obj, int num)String", stringObjectInt.toString());
    }

    /**
     * Test of returning method, of class Signature.
     */
    @Test
    public void testReturning() {
        Signature sig = Signature.returning(String.class);
        
        assertEquals(String.class, sig.methodType().returnType());
    }

    /**
     * Test of asFold method, of class Signature.
     */
    @Test
    public void testAsFold() {
        Signature sig = Signature
                .returning(String.class)
                .asFold(Object.class);
        assertEquals(Object.class, sig.methodType().returnType());
    }

    /**
     * Test of appendArg method, of class Signature.
     */
    @Test
    public void testAppendArg() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("obj", Object.class);
        
        assertEquals("(Object obj)String", sig.toString());
        
        sig = sig
                .appendArg("num", int.class);
        
        assertEquals("(Object obj, int num)String", sig.toString());
    }

    /**
     * Test of prependArg method, of class Signature.
     */
    @Test
    public void testPrependArg() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("num", int.class);
        
        assertEquals("(int num)String", sig.toString());
        
        sig = sig
                .prependArg("obj", Object.class);
        
        assertEquals("(Object obj, int num)String", sig.toString());
    }

    /**
     * Test of insertArg method, of class Signature.
     */
    @Test
    public void testInsertArg() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("obj", Object.class)
                .appendArg("num", int.class)
                .insertArg(1, "flo", float.class);
        
        assertEquals("(Object obj, float flo, int num)String", sig.toString());
    }

    /**
     * Test of insertArgs method, of class Signature.
     */
    @Test
    public void testInsertArgs() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("obj", Object.class)
                .appendArg("num", int.class)
                .insertArgs(1, new String[]{"flo", "dub"}, new Class[] {float.class, double.class});
        
        assertEquals("(Object obj, float flo, double dub, int num)String", sig.toString());
    }

    /**
     * Test of methodType method, of class Signature.
     */
    @Test
    public void testMethodType() {
        assertEquals(MethodType.methodType(String.class, Object.class, int.class), stringObjectInt.methodType());
    }

    /**
     * Test of argNames method, of class Signature.
     */
    @Test
    public void testArgNames() {
        assertArrayEquals(new String[] {"obj", "num"}, stringObjectInt.argNames());
    }

    /**
     * Test of permute method, of class Signature.
     */
    @Test
    public void testPermute() {
        Signature sig = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .permute("obj", "dub");
        
        assertEquals("(Object obj, double dub)String", sig.toString());
    }

    /**
     * Test of permuteTo method, of class Signature.
     */
    @Test
    public void testPermuteTo() throws Throwable {
        MethodHandle handle = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .permuteWith(stringObjectIntTarget, "obj", "num");
        
        assertEquals(MethodType.methodType(String.class, Object.class, int.class, float.class, double.class), handle.type());
        assertEquals("foo1", (String)handle.invokeExact((Object)"foo", 1, 1.0f, 1.0));
    }

    /**
     * Test of to method, of class Signature.
     */
    @Test
    public void testTo_Signature() {
        int[] permuteInts = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .to(stringObjectInt);
        
        assertArrayEquals(new int[] {0, 1}, permuteInts);
    }

    /**
     * Test of to method, of class Signature.
     */
    @Test
    public void testTo_StringArr() {
        int[] permuteInts = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .to("obj", "flo");
        
        assertArrayEquals(new int[] {0, 2}, permuteInts);
    }
    
    private static final Signature stringObjectInt = Signature
            .returning(String.class)
            .appendArg("obj", Object.class)
            .appendArg("num", int.class);
    
    private static final MethodHandle stringObjectIntTarget = Binder
            .from(String.class, Object.class, int.class)
            .invokeStaticQuiet(MethodHandles.lookup(), SignatureTest.class, "stringObjectIntMethod");
    
    private static String stringObjectIntMethod(Object obj, int num) {
        return obj.toString() + num;
    }
}
