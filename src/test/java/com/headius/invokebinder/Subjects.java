package com.headius.invokebinder;

import java.util.Arrays;

/**
 * Created by headius on 1/25/14.
 */
public class Subjects {
    public static final Signature StringIntegerIntegerIntegerString = Signature
            .returning(String.class)
            .appendArg("a", String.class)
            .appendArg("b1", Integer.class)
            .appendArg("b2", Integer.class)
            .appendArg("b3", Integer.class)
            .appendArg("c", String.class);

    public static String stringIntegersString(String a, Integer[] bs, String c) {
        return Arrays.deepToString(new Object[]{a, bs, c});
    }
}
