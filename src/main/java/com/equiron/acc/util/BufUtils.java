package com.equiron.acc.util;

import java.util.Arrays;

public class BufUtils {
    public static byte[] concat(byte[] src, byte[] dst, int length) {
        byte[] result = Arrays.copyOf(dst, dst.length + length);

        System.arraycopy(src, 0, result, dst.length, length);

        return result;
    }
}
 