package com.streamreduce.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/28/12 13:54</p>
 */
public class NodeableMetricUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeableMetricUtils.class);

    public static Boolean getBoolean(String key, Map m, Boolean defaultValue) {
        return get(key, m, defaultValue);
    }

    public static Boolean getBoolean(String key, Map m) {
        return get(key, m, null);
    }

    public static String getString(String key, Map m, String defaultValue) {
        return get(key, m, defaultValue);
    }

    public static String getString(String key, Map m) {
        return get(key, m, null);
    }

    public static Float getFloat(String key, Map m, Float defaultValue) {
        return get(key, m, defaultValue);
    }

    public static Float getFloat(String key, Map m) {
        return get(key, m, null);
    }

    public static Long getLong(String key, Map m, Long defaultValue) {
        return get(key, m, defaultValue);
    }

    public static Long getLong(String key, Map m) {
        return get(key, m, null);
    }

    public static Integer getInteger(String key, Map m, Integer defaultValue) {
        return get(key, m, defaultValue);
    }

    public static Integer getInteger(String key, Map m) {
        return get(key, m, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Map m, T defaultValue) {
        if (!m.containsKey(key)) {
            return defaultValue;
        }
        return (T) m.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Map m) {
        return (T) m.get(key);
    }

}
