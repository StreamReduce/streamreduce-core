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
