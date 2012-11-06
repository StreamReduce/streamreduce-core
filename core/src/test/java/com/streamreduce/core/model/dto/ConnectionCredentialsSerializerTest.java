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

package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.ConnectionCredentials;
import junit.framework.Assert;
import net.sf.json.JSONObject;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectionCredentialsSerializerTest {

    static ObjectMapper mapper;

    @BeforeClass
    public static void createMapper() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("whatevs", new Version(0, 0, 0, null));
        module.addSerializer(new ConnectionCredentialsSerializer());
        mapper.registerModule(module);
    }

    @Test
    public void testEncrypt() throws Exception {
        ConnectionCredentials cc = new ConnectionCredentials("foo", "bar");
        String jsonAsString = mapper.writeValueAsString(cc);
        JSONObject json = JSONObject.fromObject(jsonAsString);
        Assert.assertEquals("foo", json.getString("identity"));
        Assert.assertFalse(json.getString("credential").equals("bar"));
    }

    @Test
    public void testEncrypt_NullIdentity() throws Exception {
        ConnectionCredentials cc = new ConnectionCredentials(null, "bar");
        String jsonAsString = mapper.writeValueAsString(cc);
        JSONObject json = JSONObject.fromObject(jsonAsString);
        Assert.assertFalse(json.containsKey("identity"));
    }

    @Test
    public void testEncrypt_NullCredential() throws Exception {
        ConnectionCredentials cc = new ConnectionCredentials("foo", null);
        String jsonAsString = mapper.writeValueAsString(cc);
        JSONObject json = JSONObject.fromObject(jsonAsString);
        Assert.assertFalse(json.containsKey("credential"));
    }
}
