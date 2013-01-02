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


import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.test.service.TestUtils;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.RandomStringUtils;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import static com.streamreduce.connections.AuthType.OAUTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

    @Test
    public void testBuilderIncludesAuthType() {
        //Unit test to make sure Connection.Builder properly includes authtype in the
        //Connection object.
        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build())
                .authType(OAUTH)
                .url("http://someUrl")
                .build();
        assertTrue(c.getAuthType() == OAUTH);
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderWhenAuthTypeNotSpecified() {
        //Unit test to make sure Connection.Builder.build() throws IllegalStateException when authType isn't specified.
         new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build())
                .url("http://someUrl")
                .build();
    }

    @Test
    public void testBuilderTrimsUrl() {
        //Ensures that a Connection created from a Builder has a trimmed url.
        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build())
                .authType(OAUTH)
                .url("  http://someUrl  ")
                .build();
        assertEquals("http://someUrl",c.getUrl());
    }

    @Test
    public void testCreateBuilderFromJson() {
        User u  = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();
        u.setId(new ObjectId());

        Connection expected = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(u)
                .credentials(new ConnectionCredentials("user", "password"))
                .url("http://someUrl")
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        JSONObject json = new JSONObject();
        json.put("alias", expected.getAlias());
        json.put("description", expected.getDescription());
        json.put("visibility",expected.getVisibility());
        json.put("providerId", expected.getProviderId());
        json.put("type", expected.getType());
        json.put("user", u.getId());
        json.put("account",u.getAccount().getId());
        JSONObject credentials = new JSONObject();
        credentials.put("identity",expected.getCredentials().getIdentity());
        credentials.put("credential",expected.getCredentials().getCredential());
        json.put("credentials",credentials);
        json.put("url", expected.getUrl());
        json.put("authType",expected.getAuthType().toString());
        json.put("hashtags",JSONArray.fromObject(expected.getHashtags()));


        Connection actual = new Connection.Builder(json)
                .user(u) //this is cheating, but user usually comes from SecurityService
                .build();
        
        assertEquals(expected, actual);
    }

    @Test
    public void testEquals() {
        User u  = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();
        u.setId(new ObjectId());

        Connection expected = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(u)
                .credentials(new ConnectionCredentials("user","password"))
                .url("http://someUrl")
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        Connection actual = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(u)
                .credentials(new ConnectionCredentials("user","password"))
                .url("http://someUrl")
                .authType(AuthType.USERNAME_PASSWORD)
                .build();
        
        assertEquals(expected,actual);
    }

    @Test
    public void testHashCode() {
        User u  = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();
        u.setId(new ObjectId());

        Connection expected = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(u)
                .credentials(new ConnectionCredentials("user","password"))
                .url("http://someUrl")
                .authType(AuthType.USERNAME_PASSWORD)
                .build();
        Connection actual = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(u)
                .credentials(new ConnectionCredentials("user","password"))
                .url("http://someUrl")
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        assertEquals(expected.hashCode(),actual.hashCode());
    }

    @Test
    public void testConnectionHasNoOutboundConnectionsByDefault() {
        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build())
                .url("http://someUrl")
                .authType(AuthType.NONE)
                .build();

         assertEquals(0,c.getOutboundConfigurations().size());
    }

    @Test
    public void testConnectionBuilderWithOneOutboundConnection() {
        User testUser = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();

        Connection c = new Connection.Builder()
                .alias("test github" )
                .description("test github" )
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(testUser)
                .url("http://someUrl" )
                .authType(AuthType.NONE)
                .outboundConfigurations(
                        new OutboundConfiguration.Builder()
                                .credentials(new ConnectionCredentials("user", "pass" ))
                                .destination("s3" )
                                .protocol("my.bucket.name" )
                                .namespace("key/prefix/here/" )
                                .dataTypes(OutboundDataType.PROCESSED)
                                .build()
                )
                .build();

        assertEquals(1,c.getOutboundConfigurations().size());
    }

    @Test
    public void testConnectionBuilderSetsInboundConnectionReferenceOnOutboundConnection() {
        User testUser = new User.Builder().username("maynard").account(new Account.Builder().name("Tool").build()).build();

        OutboundConfiguration outboundConfiguration = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials("accessKey", "secretKey" ))
                .destination("s3")
                .protocol("my.bucket.name")
                .namespace("key/prefix/here/")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();


        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.GITHUB_PROVIDER)
                .user(testUser)
                .url("http://someUrl")
                .authType(AuthType.NONE)
                .outboundConfigurations(outboundConfiguration)
                .build();

        assertEquals(c, outboundConfiguration.getOriginatingConnection());
    }


    @Test
    public void testConnectionMergeWithJson_CopiesAllConnectionCredentialFields() {
        String expectedIdentity = RandomStringUtils.randomAlphanumeric(10);
        String expectedCredential = RandomStringUtils.randomAlphanumeric(10);
        String expectedApiKey = RandomStringUtils.randomAlphanumeric(10);
        String expectedOAuthToken = RandomStringUtils.randomAlphanumeric(10);
        String expectedOAuthTokenSecret = RandomStringUtils.randomAlphanumeric(10);

        Connection connection = TestUtils.createCloudConnection();
        JSONObject jsonObject = new JSONObjectBuilder()
                .add("credentials",new JSONObjectBuilder()
                    .add("identity",expectedIdentity)
                    .add("credential",expectedCredential)
                    .add("api_key",expectedApiKey)
                    .add("oauthToken",expectedOAuthToken)
                    .add("oauthTokenSecret",expectedOAuthTokenSecret)
                       .build())
                .build();

        connection.mergeWithJSON(jsonObject);

        ConnectionCredentials mergedCredentials = connection.getCredentials();
        Assert.assertEquals(expectedIdentity,mergedCredentials.getIdentity());
        Assert.assertEquals(expectedCredential,mergedCredentials.getCredential());
        Assert.assertEquals(expectedApiKey,mergedCredentials.getApiKey());
        Assert.assertEquals(expectedOAuthToken,mergedCredentials.getOauthToken());
        Assert.assertEquals(expectedOAuthTokenSecret,mergedCredentials.getOauthTokenSecret());
    }

}
