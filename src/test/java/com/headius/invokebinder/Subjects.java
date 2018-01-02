package com.headius.invokebinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by headius on 1/25/14.
 */
public class Subjects {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Signature StringIntegerIntegerIntegerString = Signature
            .returning(String.class)
            .appendArg("a", String.class)
            .appendArg("b1", Integer.class)
            .appendArg("b2", Integer.class)
            .appendArg("b3", Integer.class)
            .appendArg("c", String.class);

    public static final Signature StringIntegerIntegerInteger = Signature
            .returning(String.class)
            .appendArg("a", String.class)
            .appendArg("b1", Integer.class)
            .appendArg("b2", Integer.class)
            .appendArg("b3", Integer.class);

    public static final Signature StringIntegersString = Signature
            .returning(String.class)
            .appendArg("a", String.class)
            .appendArg("bs", Integer[].class)
            .appendArg("c", String.class);

    public static final MethodHandle StringIntegersStringHandle = Binder
                    .from(String.class, String.class, Integer[].class, String.class)
                    .invokeStaticQuiet(LOOKUP, Subjects.class, "stringIntegersString");

    public static final MethodHandle StringIntegersHandle = Binder
            .from(String.class, String.class, Integer[].class)
            .invokeStaticQuiet(LOOKUP, Subjects.class, "stringIntegers");

    public static String stringIntegersString(String a, Integer[] bs, String c) {
        return Arrays.deepToString(new Object[]{a, bs, c});
    }

    public static String stringIntegers(String a, Integer[] bs) {
        return Arrays.deepToString(new Object[]{a, bs});
    }

    public static MethodHandle concatHandle() throws Exception {
        return LOOKUP.findStatic(Subjects.class, "concatStatic", MethodType.methodType(String.class, String.class, String.class));
    }

    public static MethodHandle concatCharSequenceHandle() throws Exception {
        return LOOKUP.findStatic(Subjects.class, "concatStatic", MethodType.methodType(String.class, String.class, CharSequence.class));
    }

    public static String concatStatic(String a, String b) {
        return a + b;
    }

    public static String concatStatic(String a, CharSequence b) {
        return a + b;
    }

    public String stringIntegersString2(String a, Integer[] bs, String c) {
        return Arrays.deepToString(new Object[]{a, bs, c});
    }
    
    public static String foldStringIntegerIntegerInteger(String a, Integer b1, Integer b2, Integer b3) {
        return "forty_two";
    }

    public String foldVirtualStringIntegerIntegerInteger(String a, Integer b1, Integer b2, Integer b3) {
        return "forty_two";
    }


    public static String upperCase(String x) {
        return x.toUpperCase();
    }

    public static String twoIntsToString(int a, int b) {
        return "(" + a + ", " + b + ")";
    }

    public static int nextInt(int ignored) {
        return counter.getAndIncrement();
    }

    public static MethodHandle nextInt = Binder.from(int.class, int.class).invokeStaticQuiet(LOOKUP, Subjects.class, "nextInt");

    public static final AtomicInteger counter = new AtomicInteger(0);
    
}
