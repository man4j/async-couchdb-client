package com.equiron.yns.util;

public class NamedStrategy {
    public static String addUnderscores(String name) {
        StringBuffer buf = new StringBuffer(name.replace('.', '_'));

        for (int i = 1; i < buf.length() - 1; i++) {
            if (Character.isLowerCase(buf.charAt(i-1))
                && Character.isUpperCase(buf.charAt(i))
                && Character.isLowerCase(buf.charAt(i+1))) {
                buf.insert(i++, '_');
            }
        }

        return buf.toString().toLowerCase();
    }
}
