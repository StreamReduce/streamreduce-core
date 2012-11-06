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

package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ResourceBundle;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Integration tests for ConnectionService that test the end to end creating of new connections to external providers.
 */
public class ConnectionServiceCreateConnectionIT extends AbstractServiceTestCase {

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    private ResourceBundle cloudBundle = ResourceBundle.getBundle("cloud");


    /**
     * Test creating a FeedProvider within a Connection.
     */
    @Test
    public void testCreateConnectionForFeedProvider() {
        Connection feedConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(testUser)
                .hashtag("US-CA")
                .authType(AuthType.NONE)
                .url("http://status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam")
                .authType(AuthType.NONE)
                .build();
        try {
            Connection createdConnection = connectionService.createConnection(feedConnection);
            Connection retrievedConnection = connectionService.getConnection(createdConnection.getId());
            assertEquals(createdConnection.getId(), retrievedConnection.getId());
        } catch (Exception e) {
            logger.error("creating a feedConnection should not fail", e);
            fail("creating a feedConnection should not fail");
        }
    }

    @Test
    public void testCreateTwitterConnectionWithValidOAuthToken() throws Exception {
        ConnectionCredentials credentials = new ConnectionCredentials();

        credentials.setOauthToken(twitterProperties.getString("nodeable.integrations.twitter.oauth_token"));
        credentials.setOauthTokenSecret(twitterProperties.getString("nodeable.integrations.twitter.oauth_secret"));
        Connection twitterConnection = connectionService.createConnection(new Connection.Builder()
                .account(getTestAccount())
                .alias("Test Twiter Connection")
                .authType(AuthType.OAUTH)
                .credentials(credentials)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.TWITTER_PROVIDER_ID))
                .user(getTestUser())
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build());
        ConnectionCredentials twitterCredentials = twitterConnection.getCredentials();

        Assert.assertNotNull(twitterCredentials.getOauthToken());
        Assert.assertNotNull(twitterCredentials.getOauthTokenSecret());
        Assert.assertEquals("Nodeable", twitterCredentials.getIdentity());
    }

    @Test
    public void testCreateConnectionForFeedDoesNotTryToInsertHttp() throws Exception {
        //Tests that ConnectionService.createConnection will attempt to insert an "http://" protocol in front of a URL
        //in the event that a URL does not specify a protocol

        Connection feedConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(testUser)
                .authType(AuthType.NONE)
                .url("status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam")
                .build();

        Connection createdConnection = connectionService.createConnection(feedConnection);
        assertEquals("http://status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam", createdConnection.getUrl());
    }

    /**
     * Test creating a FeedProvider within a Connection.
     */
    @Test(expected = RuntimeException.class)
    public void testCreateConnectionForFeedProviderWithValidButNotRSSUrl() throws Exception {
        //Exercises behavior when we try to create a feed connection from a url that does not return an RSS/Atom payload.
        Connection feedConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(testUser)
                .hashtag("US-CA")
                .authType(AuthType.NONE)
                .url("http://www.google.com")
                .build();
        connectionService.createConnection(feedConnection);
    }

    @Test
    public void testCreateConnectionForFeedTriesToInsertHttp() throws Exception {
        //Tests that ConnectionService.createConnection will attempt to insert an "http://" protocol in front of a URL
        //in the event that a URL does not specify a protocol

        Connection feedConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(testUser)
                .authType(AuthType.NONE)
                .url("status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam")
                .build();

        Connection createdConnection = connectionService.createConnection(feedConnection);
        assertEquals("http://status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam", createdConnection.getUrl());
    }

    @Test
    public void getConnectionPostLoadSetsReferenceInOutboundConnections() throws Exception {
        //Test that verifies that an OutboundConfiguration will include a reference to its containing connection
        //after it has been loaded from mongo.

        OutboundConfiguration outboundConfiguration = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials(cloudBundle.getString("nodeable.aws.accessKeyId"), cloudBundle.getString("nodeable.aws.secretKey")))
                .protocol("s3")
                .namespace("my.bucket.name.here")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Connection testConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.CUSTOM_PROVIDER_ID))
                .user(testUser)
                .authType(AuthType.NONE)
                .outboundConfigurations(outboundConfiguration)
                .build();

        ObjectId objectId = connectionService.createConnection(testConnection).getId();

        Connection loadedFeedConnection = connectionService.getConnection(objectId);
        for (OutboundConfiguration loadedOutboundConfiguration : loadedFeedConnection.getOutboundConfigurations()) {
            Assert.assertEquals(loadedFeedConnection, loadedOutboundConfiguration.getOriginatingConnection());
        }
    }

    @Test
    public void testCreateConnectionPersistsOutboundConfigurations() throws Exception {
        OutboundConfiguration outboundConfiguration = new OutboundConfiguration.Builder()
                .credentials(new ConnectionCredentials(cloudBundle.getString("nodeable.aws.accessKeyId"), cloudBundle.getString("nodeable.aws.secretKey")))
                .destination("eu-west-1")
                .protocol("s3")
                .namespace("com.streamreduce.bucket")
                .dataTypes(OutboundDataType.PROCESSED)
                .build();

        Connection feedConnection = new Connection.Builder()
                .alias("EC2 Test Connection")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(testUser)
                .authType(AuthType.NONE)
                .url("status.aws.amazon.com/rss/ec2-us-west-1.rss?junkParam")
                .outboundConfigurations(outboundConfiguration)
                .build();

        Connection createdFeedConnection = connectionService.createConnection(feedConnection);
        ObjectId objectId = createdFeedConnection.getId();

        Connection retrievedConnection = connectionService.getConnection(objectId);
        Set<OutboundConfiguration> retrievedConnectionOutboundConfigurations = retrievedConnection.getOutboundConfigurations();
        Set<OutboundConfiguration> feedConnectionOutboundConfigurations = feedConnection.getOutboundConfigurations();
        OutboundConfiguration[] retrivedConfigurations = retrievedConnectionOutboundConfigurations.toArray(new OutboundConfiguration[retrievedConnectionOutboundConfigurations.size()]);
        OutboundConfiguration[] feedConfigurations = feedConnectionOutboundConfigurations.toArray(new OutboundConfiguration[feedConnectionOutboundConfigurations.size()]);
        Assert.assertEquals(retrivedConfigurations[0], feedConfigurations[0]);
        Assert.assertEquals(retrievedConnection.getId(), feedConnection.getId());
    }

    @Test(expected = Exception.class)
    public void testCreateJiraConnectionWithInvalidURLFails() throws Exception {
        Connection tmpConnection = new Connection.Builder()
                .alias("Nodeable Jira")
                .description("Nodeable's Jira instance.")
                .credentials(new ConnectionCredentials(jiraProperties.getString("nodeable.jira.username"),
                        jiraProperties.getString("nodeable.jira.password")))
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.JIRA_PROVIDER_ID))
                .url("http://some.fake.url")
                .authType(AuthType.USERNAME_PASSWORD)
                .user(testUser)
                .build();

        connectionService.createConnection(tmpConnection);
    }

    @Test(expected = InvalidCredentialsException.class)
    public void testCreateJiraWithInvalidCredentials() throws Exception {
        Connection tmpConnection = new Connection.Builder()
                .alias("Nodeable Jira")
                .description("Nodeable's Jira instance.")
                .credentials(new ConnectionCredentials("fakeusername", "fakepassword"))
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.JIRA_PROVIDER_ID))
                .url(jiraProperties.getString("nodeable.jira.url"))
                .authType(AuthType.USERNAME_PASSWORD)
                .user(testUser)
                .build();

        connectionService.createConnection(tmpConnection);
    }

}
