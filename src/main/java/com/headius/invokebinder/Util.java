package com.headius.invokebinder;

/**
 * Created by headius on 6/27/17.
 */
public class Util {
    public static boolean isJava9() {
        try {
            return System.getProperty("java.specification.version", "").equals("9");
        } catch (Exception e) {
            return false;
        }
    }
}
