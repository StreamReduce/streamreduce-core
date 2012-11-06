package com.streamreduce.core.model;

import com.streamreduce.test.service.TestUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

public class UserJsonSerializationIT {

    @Test
    public void testUserConfigDeserializationWithNullUserConfigValue() throws Exception {
        User testUser = TestUtils.createTestUser();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(null);
        jsonArray.add(JSONNull.getInstance());
        jsonArray.add(true);
        jsonArray.add(false);
        testUser.setConfigValue("foo",jsonArray);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValueAsString(testUser);
    }

    @Test
    public void testUserConfigDeserializationWithJSONArrayAsString() throws Exception {
        User testUser = TestUtils.createTestUser();
        String jsonArrayWithNullAsString = "[null]";
        testUser.setConfigValue("foo",jsonArrayWithNullAsString);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValueAsString(testUser);
    }

    @Test
    public void testUserConfigDeserializationMapJSONObject() throws Exception {
        User testUser = TestUtils.createTestUser();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a",null);
        jsonObject.put("b",JSONNull.getInstance());
        jsonObject.put("c","test");

        testUser.setConfigValue("foo", jsonObject);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValueAsString(testUser);
    }
}
