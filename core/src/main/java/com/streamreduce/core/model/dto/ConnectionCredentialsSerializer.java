package com.streamreduce.core.model.dto;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.ConnectionCredentialsEncrypter;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

public class ConnectionCredentialsSerializer extends JsonSerializer<ConnectionCredentials> {

    @Override
    public Class<ConnectionCredentials> handledType() {
        return ConnectionCredentials.class;
    }

    @Override
    public void serialize(ConnectionCredentials connectionCredentials, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        //Create a copy so we don't encrypt the existing reference
        ConnectionCredentials copy = ConnectionCredentials.copyOf(connectionCredentials);

        ConnectionCredentialsEncrypter credentialsEncrypter = new ConnectionCredentialsEncrypter();
        credentialsEncrypter.encrypt(copy);

        jgen.writeStartObject();
        if (copy.getIdentity() != null) {
            jgen.writeStringField("identity",copy.getIdentity());
        }
        if (copy.getCredential() != null) {
            jgen.writeStringField("credential",copy.getCredential());
        }
        if (copy.getApiKey() != null) {
            jgen.writeStringField("apiKey",copy.getApiKey());
        }
        if (copy.getOauthToken() != null) {
            jgen.writeStringField("oauthToken",copy.getOauthToken());
        }
        if (copy.getOauthTokenSecret() != null) {
            jgen.writeStringField("oauthTokenSecret",copy.getOauthTokenSecret());
        }
        if (copy.getOauthVerifier() != null) {
            jgen.writeStringField("oauthVerifier",copy.getOauthVerifier());
        }
        jgen.writeEndObject();
    }
}
