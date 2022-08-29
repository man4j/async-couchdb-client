package com.equiron.yns.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils {
    public static Field[] getAllFields(Class<?> cl) {
        List<Field> fields = new ArrayList<>();

        fields.addAll(Arrays.asList(cl.getDeclaredFields()));

        if (cl.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getAllFields(cl.getSuperclass())));
        }

        return fields.toArray(new Field[] {});
    }
}
