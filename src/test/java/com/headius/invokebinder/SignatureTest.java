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
import java.util.List;
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
        
        assertEquals(String.class, sig.type().returnType());
    }

    /**
     * Test of asFold method, of class Signature.
     */
    @Test
    public void testAsFold() {
        Signature sig = Signature
                .returning(String.class)
                .asFold(Object.class);
        assertEquals(Object.class, sig.type().returnType());
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

    @Test
    public void testInsertArgBefore() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("obj", Object.class)
                .appendArg("num", int.class)
                .insertArg("num", "flo", float.class);
        
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
    @Test
    public void testInsertArgsBefore() {
        Signature sig = Signature
                .returning(String.class)
                .appendArg("obj", Object.class)
                .appendArg("num", int.class)
                .insertArgs("num", new String[]{"flo", "dub"}, new Class[] {float.class, double.class});
        
        assertEquals("(Object obj, float flo, double dub, int num)String", sig.toString());
    }

    /**
     * Test of methodType method, of class Signature.
     */
    @Test
    public void testMethodType() {
        assertEquals(MethodType.methodType(String.class, Object.class, int.class), stringObjectInt.type());
    }

    /**
     * Test of argNames method, of class Signature.
     */
    @Test
    public void testArgNames() {
        assertArrayEquals(new String[] {"obj", "num"}, stringObjectInt.argNames());
    }
    
    @Test
    public void testArgName() {
        assertEquals("num", stringObjectInt.appendArg("flo", float.class).argName(1));
    }
    
    @Test
    public void testLastArgName() {
        assertEquals("flo", stringObjectInt.appendArg("flo", float.class).lastArgName());
    }
    
    @Test
    public void testFirstArgName() {
        assertEquals("obj", stringObjectInt.appendArg("flo", float.class).firstArgName());
    }
    
    @Test
    public void testArgOffset() {
        assertEquals(1, stringObjectInt.argOffset("num"));
        assertEquals(-1, stringObjectInt.argOffset("flo"));
    }
    
    @Test
    public void testArgOffsets() {
        assertEquals(1, stringObjectInt.argOffsets("nu*"));
        assertEquals(-1, stringObjectInt.argOffsets("fl."));
    }
    
    @Test
    public void testArgType() {
        assertEquals(int.class, stringObjectInt.appendArg("flo", float.class).argType(1));
    }
    
    @Test
    public void testFirstArgType() {
        assertEquals(Object.class, stringObjectInt.appendArg("flo", float.class).firstArgType());
    }
    
    @Test
    public void testLastArgType() {
        assertEquals(float.class, stringObjectInt.appendArg("flo", float.class).lastArgType());
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
    
    @Test
    public void testExclude() {
        Signature sig = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .exclude("obj", "dub");
        
        assertEquals("(int num, float flo)String", sig.toString());
    }

    /**
     * Test of permuteTo method, of class Signature.
     */
    @Test
    public void testPermuteWith() throws Throwable {
        MethodHandle handle = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .permuteWith(stringObjectIntTarget, "obj", "num");
        
        assertEquals(MethodType.methodType(String.class, Object.class, int.class, float.class, double.class), handle.type());
        assertEquals("foo1", (String)handle.invokeExact((Object)"foo", 1, 1.0f, 1.0));
    }
    
    @Test
    public void testPermuteWithSmartHandle() throws Throwable {
        SmartHandle target = new SmartHandle(stringObjectInt, stringObjectIntTarget);
        SmartHandle permuted = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .permuteWith(target);
        
        assertEquals("(Object obj, int num, float flo, double dub)String", permuted.signature().toString());
        assertEquals("foo1", permuted.handle().invokeWithArguments("foo", 1, 1.0f, 1.0));
    }
    
    @Test
    public void testSpreadNamesAndTypes() throws Throwable {
        Signature sig = stringObjectAry
                .spread(new String[]{"int", "flo"}, Integer.class, Float.class);
        
        assertEquals("(Integer int, Float flo)String", sig.toString());
    }
    
    @Test
    public void testSpreadNames() throws Throwable {
        Signature sig = stringObjectAry
                .spread("obj0", "obj1");
        
        assertEquals("(Object obj0, Object obj1)String", sig.toString());
    }
    
    @Test
    public void testSpreadNameAndCount() throws Throwable {
        Signature sig = stringObjectAry
                .spread("obj", 2);
        
        assertEquals("(Object obj0, Object obj1)String", sig.toString());
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
                .to(".*o.*");
        
        assertArrayEquals(new int[] {0, 2}, permuteInts);
        
        permuteInts = stringObjectInt
                .appendArg("flo", float.class)
                .appendArg("dub", double.class)
                .to("num", "dub");
        
        assertArrayEquals(new int[] {1, 3}, permuteInts);
    }
    
    @Test
    public void testDropArg() {
        Signature newSig = stringObjectInt
                .appendArg("flo", float.class)
                .dropArg("num");
        
        assertEquals("(Object obj, float flo)String", newSig.toString());
    }
    
    @Test
    public void testDropArgIndex() {
        Signature newSig = stringObjectInt
                .appendArg("flo", float.class)
                .dropArg(1);
        
        assertEquals("(Object obj, float flo)String", newSig.toString());
    }
    
    @Test
    public void testDropLast() {
        Signature newSig = stringObjectInt
                .dropLast();
        
        assertEquals("(Object obj)String", newSig.toString());
    }
    
    @Test
    public void testDropFirst() {
        Signature newSig = stringObjectInt
                .dropFirst();
        
        assertEquals("(int num)String", newSig.toString());
    }
    
    @Test
    public void testReplaceArg() {
        Signature newSig = stringObjectInt
                .replaceArg("obj", "list", List.class);
        
        assertEquals("(List list, int num)String", newSig.toString());
    }
    
    private static final Signature stringObjectInt = Signature
            .returning(String.class)
            .appendArg("obj", Object.class)
            .appendArg("num", int.class);
    
    private static final Signature stringObjectAry = Signature
            .returning(String.class)
            .appendArg("objs", Object[].class);
    
    private static final MethodHandle stringObjectIntTarget = Binder
            .from(String.class, Object.class, int.class)
            .invokeStaticQuiet(MethodHandles.lookup(), SignatureTest.class, "stringObjectIntMethod");
    
    private static String stringObjectIntMethod(Object obj, int num) {
        return obj.toString() + num;
    }
}
