package com.headius.invokebinder;

/**
 * Utilities used by InvokeBinder classes.
 */
public class Util {
    public static boolean IS_JAVA9;

    static {
        boolean isJava9;
        try {
            Class.forName("java.lang.Module");
            isJava9 = true;
        } catch (Exception e) {
            isJava9 = false;
        }
        IS_JAVA9 = isJava9;
    }

    public static boolean isJava9() {
        return IS_JAVA9;
    }
}
