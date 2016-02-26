package com.n1global.acc.util;

import java.util.Arrays;

public class BufUtils {
    public static byte[] concat(byte[] src, byte[] dst) {
        byte[] result = Arrays.copyOf(dst, dst.length + src.length);

        System.arraycopy(src, 0, result, dst.length, src.length);

        return result;
    }
}
 