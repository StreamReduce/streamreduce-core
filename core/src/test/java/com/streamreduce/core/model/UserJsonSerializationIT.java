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
