package com.streamreduce.util;

import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link JSONUtils} works as expected.
 */
public class JSONUtilsTest {

    /**
     * Ensures that {@link JSONUtils#flattenJSON(net.sf.json.JSON, String)} works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testFlattenJSON() throws Exception {
        Set<String> expectedKeys = ImmutableSet.of(
                "plainValue",
                "objectValue.plainValue",
                "objectValue.objectValue.plainValue",
                "objectValue.arrayValue.0",
                "objectValue.arrayValue.1.plainValue",
                "objectValue.arrayValue.2.0",
                "objectValue.arrayValue.2.1.plainValue",
                "arrayValue.0",
                "arrayValue.1.plainValue",
                "arrayValue.2.0",
                "arrayValue.2.1.plainValue"
        );
        JSONObject originalJSON = JSONObject.fromObject(JSONUtils.readJSONFromClasspath("JSONUtils-Test.json"));
        JSONObject flattenedJSON = JSONUtils.flattenJSON(originalJSON, "");

        assertEquals(flattenedJSON.keySet().size(), expectedKeys.size());

        for (Object rawKey : flattenedJSON.keySet()) {
            String key = rawKey.toString();

            Assert.assertTrue(expectedKeys.contains(key));
            assertEquals("Nodeable", flattenedJSON.getString(key));
        }
    }

    @Test
    public void testSanitizeMapForJsonConvertsObjectIdsToStrings() {
        ObjectId a = new ObjectId();
        ObjectId b = new ObjectId();

        Map<String,Object> map = new HashMap<String,Object>();
        map.put("a",a);
        map.put("somethingElse",42);

        Map<Object,Object> innerMap = new HashMap<Object,Object>();
        innerMap.put("b", b);

        map.put("innerMap", innerMap);

        Map<String,Object> actualMap = JSONUtils.sanitizeMapForJson(map);

        assertEquals(a.toString(), actualMap.get("a"));
        assertEquals(b.toString(), ((Map) actualMap.get("innerMap")).get("b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReplaceJSONNullsFromList_JSONArray() {
        JSONArray arr = new JSONArray();
        arr.add("foo");
        arr.add(null);
        arr.add(JSONNull.getInstance());

        //Assert that original JSONArray is untouched and that JSONNull is replaced with null.
        List<Object> newList = JSONUtils.replaceJSONNullsFromList(arr);
        assertEquals(3,arr.size());
        assertEquals(3, newList.size());
        assertEquals("foo", newList.get(0));
        assertNull(newList.get(1));
        assertNull(newList.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReplaceJSONNullsFromList_JSONArrayRecursively() {
        JSONArray outer = new JSONArray();
        JSONArray inner = new JSONArray();
        JSONArray twoDeep = new JSONArray();

        twoDeep.add("twoDeep");
        twoDeep.add(JSONNull.getInstance());
        twoDeep.add(null);

        inner.add("inner");
        inner.add(twoDeep);
        inner.add(JSONNull.getInstance());
        inner.add(null);

        outer.add(inner);

        List<Object> trimmedOuter = JSONUtils.replaceJSONNullsFromList(outer);
        assertEquals(1, trimmedOuter.size());

        List<Object> newInner = (List<Object>) trimmedOuter.get(0);
        assertEquals(4, newInner.size());
        assertNull(newInner.get(2));
        assertNull(newInner.get(3));

        List<Object> newTwoDeep = (List<Object>) newInner.get(1);
        assertEquals(3, newTwoDeep.size());
        assertNull(newTwoDeep.get(1));
        assertNull(null, newTwoDeep.get(2));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testConvertJSONObjectToMap() throws Exception {
        JSONObject jsonObject = new JSONObject();

        JSONArray jsonArray = new JSONArray();
        jsonArray.add("innerArray");
        jsonArray.add(JSONNull.getInstance());
        jsonArray.add(null);

        jsonObject.put("foo","bar");
        jsonObject.put("arr",jsonArray);
        jsonObject.put("nullValue",JSONNull.getInstance());

        Map<String,Object> map = JSONUtils.convertJSONObjectToMap(jsonObject);

        assertEquals(3, map.size());

        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));

        assertTrue(map.containsKey("arr"));

        List<Object> innerList = (List<Object>) map.get("arr");
        assertNull(innerList.get(1));
        assertNull(innerList.get(2));
    }
}
