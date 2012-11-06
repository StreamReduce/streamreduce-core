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

package com.streamreduce.rest.resource.api;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test to exercise methods used to GET/POST/PUT/DELETE configs
 */
public class UserResource_ConfigIT extends AbstractServiceTestCase {

    @Autowired
    UserResource userResource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityService mockSecurityService = mock(SecurityService.class);
        when(mockSecurityService.getCurrentUser()).thenReturn(testUser);
        userResource.securityService = mockSecurityService;
    }

    @Test
    public void testConfigGET() throws Exception {
        //Asserts a default config is available through the REST endpoint
        Response response = userResource.getConfig();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject config = (JSONObject) response.getEntity();
        assertTrue(config.getBoolean(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS));
        assertTrue(config.getBoolean(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS));
        assertNotNull(config.getString(User.ConfigKeys.GRAVATAR_HASH));
    }

    @Test
    public void testConfigPOST() throws Exception {
        //Asserts that a config can be appended to and modified with the values of a new JSONObject
        JSONObject newConfigValues = new JSONObjectBuilder()
                .add("foo", 1)
                .add("bar", new JSONArray())
                .add(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, false)
                .build();

        Response response = userResource.setConfigValues(newConfigValues);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject config = (JSONObject) response.getEntity();
        assertTrue(config.getBoolean(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS));
        assertFalse(config.getBoolean(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS));
        assertEquals(1, config.getInt("foo"));
        assertEquals(new JSONArray(), config.getJSONArray("bar"));
    }

    @Test
    public void testConfigKeyGET() throws Exception {
        testUser.setConfigValue("foo", true);
        Response response = userResource.getConfigValueByKey("foo");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONObject config = (JSONObject) response.getEntity();
        assertTrue(config.size() == 1);
        assertTrue(config.getBoolean("foo"));
    }

    @Test
    public void testConfigKeyDELETE_unrequiredKey() throws Exception {
        testUser.setConfigValue("foo", true);
        Response originalGetResponse = userResource.getConfigValueByKey("foo");
        assertEquals(Response.Status.OK.getStatusCode(), originalGetResponse.getStatus());

        Response deleteResponse = userResource.removeConfigKey("foo");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());


        Response afterDeleteGetResponse = userResource.getConfigValueByKey("foo");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), afterDeleteGetResponse.getStatus());
    }

    @Test
    public void testConfigKeyPOST_arrayWithNull() {
        //Tests SOBA-2079 when passing in [null] as a key value in a JSONObject
        JSONArray array = new JSONArray();
        array.add(null);
        JSONObject newConfigValues = new JSONObjectBuilder()
                .add("foo", array)
                .build();
        Response responsePOST = userResource.setConfigValues(newConfigValues);
        assertEquals(Response.Status.OK.getStatusCode(), responsePOST.getStatus());
        JSONObject responseBody = (JSONObject) responsePOST.getEntity();
        assertEquals(0, responseBody.getJSONArray("foo").size());
    }

    @Test
    public void testConfigKeyPOST_arrayWithNullAsString() {
        //Tests SOBA-2079 when passing in [null] as a key value in a JSONObject
        JSONObject newConfigValues = new JSONObjectBuilder()
                .add("foo", "[null]")
                .build();
        Response responsePOST = userResource.setConfigValues(newConfigValues);
        assertEquals(Response.Status.OK.getStatusCode(), responsePOST.getStatus());
        JSONObject responseBody = (JSONObject) responsePOST.getEntity();
        assertEquals(0, responseBody.getJSONArray("foo").size());
    }

    @Test
    public void testConfigKeyPOST_NullsInObjectsAndDeeplyNestedNullsInArrays() {
        //Tests SOBA-2079 when passing in [null] as a key value in a JSONObject
        JSONObject newConfigValues = new JSONObjectBuilder()
                .add("foo", new JSONObjectBuilder()
                        .add("bar", JSONNull.getInstance())
                        .add("baz", 1)
                        .array("bing", 1, 2, 3, JSONNull.getInstance())
                        .build())
                .array("arr", 1, 2, 3, JSONNull.getInstance()).build();

        Response responsePOST = userResource.setConfigValues(newConfigValues);
        assertEquals(Response.Status.OK.getStatusCode(), responsePOST.getStatus());

    }
}
