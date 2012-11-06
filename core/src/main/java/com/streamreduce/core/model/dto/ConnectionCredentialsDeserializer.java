package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.ConnectionCredentialsEncrypter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

public class ConnectionCredentialsDeserializer extends JsonDeserializer<ConnectionCredentials> {

    @Override
    public ConnectionCredentials deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        Map<String,String> keyValues = new HashMap<String,String>();
        while (jp.nextValue() != null && jp.getCurrentToken() != JsonToken.END_OBJECT) {
            String name = jp.getCurrentName();
            String value = jp.getText();
            keyValues.put(name,value);
        }

        ConnectionCredentials cc = new ConnectionCredentials();
        cc.setIdentity(keyValues.get("identity"));
        cc.setCredential(keyValues.get("credential"));
        cc.setApiKey(keyValues.get("apiKey"));
        cc.setOauthToken(keyValues.get("oauthToken"));
        cc.setOauthTokenSecret(keyValues.get("oauthTokenSecret"));
        cc.setOauthVerifier(keyValues.get("oauthVerifier"));

        ConnectionCredentialsEncrypter credentialsEncrypter = new ConnectionCredentialsEncrypter();
        credentialsEncrypter.decrypt(cc);

        return cc;
    }
}
