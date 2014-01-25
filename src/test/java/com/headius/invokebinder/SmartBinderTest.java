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
                .invoke(stringIntegersString);

        assertEquals("[foo, [1, 2, 3], bar]", (String)handle.handle().invokeExact("foo", new Integer(5), new Integer(6), new Integer(7), "bar"));
    }
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    
    private static final MethodHandle stringInt = Binder
            .from(String.class, int.class)
            .invokeStaticQuiet(LOOKUP, SmartBinderTest.class, "stringInt");

    private static final MethodHandle stringIntegersString = Binder
            .from(String.class, String.class, Integer[].class, String.class)
            .invokeStaticQuiet(LOOKUP, Subjects.class, "stringIntegersString");
    
    public static String stringInt(int value) {
        return Integer.toString(value);
    }

}