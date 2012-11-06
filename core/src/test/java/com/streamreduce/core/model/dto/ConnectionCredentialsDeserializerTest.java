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
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ConnectionCredentialsDeserializerTest {

    static ObjectMapper mapper;

    @BeforeClass
    public static void createMapper() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("whatevs", new Version(0, 0, 0, null));
        module.addSerializer(new ConnectionCredentialsSerializer());
        module.addDeserializer(ConnectionCredentials.class, new ConnectionCredentialsDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        ConnectionCredentials cc = new ConnectionCredentials("foo",null);
        cc.setCredential("ihateyou");
        cc.setApiKey("ihateyou");
        cc.setOauthToken("ihateyou");
        cc.setOauthTokenSecret("ihateyou");
        cc.setOauthVerifier("notencrypted");

        String jsonAsString = mapper.writeValueAsString(cc);

        assertFalse(jsonAsString.contains("ihateyou"));

        ConnectionCredentials deserializedCC = mapper.readValue(jsonAsString,ConnectionCredentials.class);
        Assert.assertEquals(cc,deserializedCC);
    }
}
