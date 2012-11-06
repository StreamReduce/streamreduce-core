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
