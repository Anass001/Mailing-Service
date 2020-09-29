/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 */
package tech.bluemail.platform.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.apache.commons.lang.ArrayUtils;
import tech.bluemail.platform.exceptions.SystemException;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.meta.annotations.Column;

public class Inspector {
    public static String[] classFields(Object object) {
        Object[] fields = new String[]{};
        if (object == null) return fields;
        Field[] arrfield = object.getClass().getDeclaredFields();
        int n = arrfield.length;
        int n2 = 0;
        while (n2 < n) {
            Field field = arrfield[n2];
            fields = (String[])ArrayUtils.add((Object[])fields, (Object)field.getName());
            ++n2;
        }
        return fields;
    }

    public static Column columnMeta(Object object, String columnName) {
        if (object == null) return null;
        try {
            Field field = object.getClass().getDeclaredField(columnName);
            Column[] annotations = (Column[])field.getAnnotationsByType(Column.class);
            if (annotations.length <= 0) return null;
            return annotations[0];
        }
        catch (Exception e) {
            Logger.error(new SystemException(e), Inspector.class);
        }
        return null;
    }
}

