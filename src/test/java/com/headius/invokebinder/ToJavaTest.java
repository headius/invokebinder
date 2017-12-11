package com.headius.invokebinder;

import com.headius.invokebinder.transform.Cast;
import com.headius.invokebinder.transform.Catch;
import com.headius.invokebinder.transform.Collect;
import com.headius.invokebinder.transform.Convert;
import com.headius.invokebinder.transform.Drop;
import com.headius.invokebinder.transform.Filter;
import com.headius.invokebinder.transform.FilterReturn;
import com.headius.invokebinder.transform.Fold;
import com.headius.invokebinder.transform.Insert;
import com.headius.invokebinder.transform.Spread;
import com.headius.invokebinder.transform.Transform;
import com.headius.invokebinder.transform.Varargs;
import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ToJavaTest {
    @Test
    public void testCast() {
        Cast cast = new Cast(MethodType.methodType(String.class, Integer[].class, float.class));

        String toJava = cast.toJava(null);

        Assert.assertEquals("handle = MethodHandles.explicitCastArguments(handle, java.lang.Integer[].class, float.class);", toJava);
    }

    @Test
    public void testCatch() {
        Catch ctch = new Catch(RuntimeException.class, DUMMY_HANDLE);

        String toJava = ctch.toJava(null);

        Assert.assertEquals("handle = MethodHandles.catchException(handle, java.lang.RuntimeException.class, " + Catch.EXCEPTION_HANDLER_JAVA + ");", toJava);
    }

    @Test
    public void testCollect() {
        MethodType source = MethodType.methodType(void.class, int.class, int.class, int.class);
        MethodType incoming = MethodType.methodType(void.class, int.class, int[].class);

        Collect collect = new Collect(source, 1, int[].class);

        String toJava = collect.toJava(incoming);

        Assert.assertEquals("handle = handle.asCollector(int[].class, 2);", toJava);

        collect = new Collect(source, 1, 1, int[].class);

        toJava = collect.toJava(incoming);

        String expected =
                "handle = MethodHandles.permuteArguments(handle, MethodType.methodType(void.class, int.class, int.class, int[].class), new int[] {0, 1});\n" +
                "handle = handle.asCollector(int[].class, 1);\n" +
                "handle = MethodHandles.permuteArguments(handle, MethodType.methodType(void.class, int.class, int.class, int.class), new int[] {0, 2, 1});";

        Assert.assertEquals(expected, toJava);
    }

    @Test
    public void testConvert() {
        MethodType source = MethodType.methodType(void.class, String.class);
        MethodType incoming = MethodType.methodType(void.class, Object.class);

        Convert convert = new Convert(source);

        String toJava = convert.toJava(incoming);

        String expected = "handle = MethodHandles.explicitCastArguments(handle.asType(MethodType.methodType(void.class, java.lang.String.class), MethodType.methodType(void.class, java.lang.String.class));";

        Assert.assertEquals(expected, toJava);

        source = MethodType.methodType(long.class, String.class);
        incoming = MethodType.methodType(int.class, Object.class);

        convert = new Convert(source);

        toJava = convert.toJava(incoming);

        Assert.assertEquals("handle = handle.asType(MethodType.methodType(long.class, java.lang.String.class));", toJava);
    }

    @Test
    public void testDrop() {
        MethodType incoming = MethodType.methodType(void.class);

        Drop drop = new Drop(0, int.class, int.class, int.class);

        String toJava = drop.toJava(incoming);

        Assert.assertEquals("handle = MethodHandles.dropArguments(handle, 0, int.class, int.class, int.class);", toJava);
    }

    @Test
    public void testFilter() {
        Filter filter = new Filter(0, null);

        String toJava = filter.toJava(DUMMY_HANDLE.type());

        Assert.assertEquals("handle = MethodHandles.filterArguments(handle, 0, " + Filter.FILTER_FUNCTIONS_JAVA + ");", toJava);
    }

    @Test
    public void testFilterReturn() {
        FilterReturn filterReturn = new FilterReturn(DUMMY_HANDLE);

        String toJava = filterReturn.toJava(null);

        Assert.assertEquals("handle = MethodHandles.filterReturnValue(handle, " + FilterReturn.FILTER_FUNCTION_JAVA + ");", toJava);
    }

    @Test
    public void testFold() {
        Fold fold = new Fold(DUMMY_HANDLE);

        String toJava = fold.toJava(null);

        Assert.assertEquals("handle = MethodHandles.foldArguments(handle, " + Fold.FOLD_FUNCTION_JAVA + ");", toJava);
    }

    @Test
    public void testInsert() {
        Insert insert = new Insert(0, new Class[] {int.class, double.class, Object.class}, 1L, 1.0F, "hello");

        String toJava = insert.toJava(null);

        Assert.assertEquals("handle = MethodHandles.insertArguments(handle, 0, (int)1L, (double)1.0F, (java.lang.Object)value3);", toJava);
    }

    @Test
    public void testSpread() {
        Spread spread = new Spread(OBJECTARRAY_HANDLE.type(), Object.class, String.class, Integer.class);

        String toJava = spread.toJava(null);

        Assert.assertEquals("handle = handle.asSpreader(java.lang.Object[].class, 3);", toJava);
    }

    @Test
    public void testVarargs() {
        Varargs varargs = new Varargs(OBJECTS_HANDLE.type(), 0, Object[].class);

        String toJava = varargs.toJava(null);

        Assert.assertEquals("handle = handle.asVarargsCollector(java.lang.Object[].class).asType(" + Transform.generateMethodType(OBJECTS_HANDLE.type()) + ");", toJava);
    }

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle DUMMY_HANDLE = Binder.from(void.class).invokeStaticQuiet(LOOKUP, ToJavaTest.class, "dummy");
    private static void dummy() {}

    private static final MethodHandle OBJECTARRAY_HANDLE = Binder.from(void.class, Object[].class).invokeStaticQuiet(LOOKUP, ToJavaTest.class, "objectArray");
    private static void objectArray(Object[] ary) {}

    private static final MethodHandle OBJECTS_HANDLE = Binder.from(void.class, Object.class, Object.class, Object.class).invokeStaticQuiet(LOOKUP, ToJavaTest.class, "objects");
    private static void objects(Object o, Object p, Object q) {}
}
