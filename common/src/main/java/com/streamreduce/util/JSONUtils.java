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

import com.google.common.collect.Maps;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JSONUtils {

    /**
     * Takes an XML string and converts it to a JSONObject.
     *
     * @param xml the XML string
     *
     * @return a JSONObject
     */
    public static JSONObject xmlToJSON(String xml) {
        return ((JSONObject)new XMLSerializer().read(xml));
    }

    /**
     * Flattens JSON into a single level of key/value pairs.
     *
     * @param json the {@link JSON} object to flatten
     * @param prefix the prefix to append to each key
     *
     * @return the newly flattened {@link JSONObject}
     */
    public static JSONObject flattenJSON(JSON json, String prefix) {
        JSONObject flattenedJSON = new JSONObject();

        prefix = (StringUtils.isNotBlank(prefix) ? prefix + "." : "");

        if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray)json;

            for (int i = 0; i < jsonArray.size(); i++) {
                Object value = jsonArray.get(i);
                String flatKey = prefix + i;

                if (value instanceof JSONArray) {
                    flattenedJSON.putAll(flattenJSON((JSONArray)value,flatKey));
                } else if (value instanceof JSONObject) {
                    flattenedJSON.putAll(flattenJSON((JSONObject)value, flatKey));
                } else {
                    flattenedJSON.put(flatKey, value);
                }
            }
        } else if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)json;

            for (Object rawKey : jsonObject.keySet()) {
                String key = rawKey.toString();
                Object value = jsonObject.get(key);
                String flatKey = prefix + key;

                if (value instanceof JSONArray) {
                    flattenedJSON.putAll(flattenJSON((JSONArray)value, flatKey));
                } else if (value instanceof JSONObject) {
                    flattenedJSON.putAll(flattenJSON((JSONObject)value, flatKey));
                } else {
                    flattenedJSON.put(flatKey, value);
                }
            }
        } else {
            throw new IllegalArgumentException("Unable to flatten a JSONNull.");
        }

        return flattenedJSON;
    }

    /**
     * Reads in a JSON file from the classpath.
     *
     * @param resource the resource to read in as a string
     *
     * @return the contents of the resource as string
     *
     * @throws Exception if anything goes wrong
     */
    public static String readJSONFromClasspath(String resource) throws Exception {
        InputStream inputStream = JSONUtils.class.getResourceAsStream(resource);
        StringWriter writer = new StringWriter();

        IOUtils.copy(inputStream, writer, "UTF-8");

        return writer.toString();
    }

    /**
     * Copies and sanitizes any Map so that it is suitable for conversion to JSON.  This method only converts any
     * (nested) values that are of type ObjectId to its .toString() value.
     *
     * @param map Any Map.
     * @return A mutable Map&lt;Object,Object&gt; copied from the passed in Map
     * with sanitized values specific to Nodeable for converting to JSON.
     */
    public static <T> Map<T, Object> sanitizeMapForJson(Map<T,Object> map) {
        Map<T,Object> returnMap = Maps.newHashMap(map);
        for (T key : returnMap.keySet()) {
            Object o = returnMap.get(key);
            if (o instanceof Map) {
                returnMap.put(key, sanitizeMapForJson((Map<Object,Object>)o));
            }
            if (o instanceof ObjectId) {
                returnMap.put(key,o.toString());
            }
        }
        return returnMap;
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T,Object> replaceJSONNullsFromMap(Map<T,Object> oldMap) {
        if (oldMap == null) { return null;}

        Map<T,Object> map = new HashMap<>(oldMap);

        //Iterate over old keyset since we modify the new map
        for (T key : oldMap.keySet()) {
            Object value = oldMap.get(key);
            if (JSONNull.getInstance().equals(value)) {
                map.put(key,null);
            } else if (value instanceof Map) {
                map.put(key, replaceJSONNullsFromMap((Map<T, Object>) value));
            } else if (value instanceof List) {
                map.put(key, replaceJSONNullsFromList((List<Object>) value));
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> replaceJSONNullsFromList(List<Object> list) {
        if (list == null) { return null; }

        List<Object> newList = new ArrayList<>(list);

        for (int i = 0; i < newList.size() ; i++) {
            Object o = newList.get(i);
            if (o instanceof Map) {
                newList.set(i, replaceJSONNullsFromMap((Map) o));
            } else if (o instanceof List) {
                newList.set(i, replaceJSONNullsFromList((List<Object>) o));
            } else if (JSONNull.getInstance().equals(o)) {
                newList.set(i,null);
            }
        }
        return newList;
    }

    public static Map<String,Object> convertJSONObjectToMap(JSONObject jsonObject) {
        if (jsonObject == null) { return null;}

        Map<String,Object> map = new HashMap<>();
        for (Object key : jsonObject.keySet()) {
            map.put(key.toString(),jsonObject.get(key));
        }

        return replaceJSONNullsFromMap(map);

    }
}
