/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.logging.Logger;

public class Mapper {
    public static Object getMapValue(TreeMap map, String key, Object defaultValue) {
        if (map == null) return defaultValue;
        if (map.isEmpty()) return defaultValue;
        if (!map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value;
    }

    public static Object getMapValue(HashMap map, String key, Object defaultValue) {
        if (map == null) return defaultValue;
        if (map.isEmpty()) return defaultValue;
        if (!map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value;
    }

    public static HashMap<String, String> readProperties(String filePath) {
        HashMap<String, String> results = new HashMap<String, String>();
        Properties properties = new Properties();
        try {
            FileInputStream in = FileUtils.openInputStream(new File(filePath));
            properties.load(in);
            properties.stringPropertyNames().forEach(key -> {
                String value = properties.getProperty((String)key);
                results.put((String)key, value);
            });
            return results;
        }
        catch (IOException e) {
            Logger.error(e, Mapper.class);
        }
        return results;
    }
}

