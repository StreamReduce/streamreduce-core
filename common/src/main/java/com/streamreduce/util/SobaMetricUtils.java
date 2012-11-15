/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.streamreduce.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/28/12 13:54</p>
 */
public class SobaMetricUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SobaMetricUtils.class);

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
