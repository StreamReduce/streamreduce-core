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

package com.streamreduce.util;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.Date;


/**
 * Test class for {@link FeedClient}.
 */
public class FeedClientIT {

    @Test
    public void testValidateFeedConnection() throws Exception {
        FeedClient client = new FeedClient(createValidFeedConnection());
        client.validateConnection();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFeedConnectionNonFeedProvider() throws Exception {
        Connection feedConnection = createValidFeedConnection();

        feedConnection.setProviderId("nonFeedProviderId");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    @Test(expected = MalformedURLException.class)
    public void testValidateFeedConnectionMalformedUrl() throws Exception {
        Connection feedConnection = createValidFeedConnection();

        feedConnection.setUrl("abc123");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    @Test(expected = RuntimeException.class)
    public void testValidateFeedConnectionUnavailableUrl() throws Exception {
        Connection feedConnection = createValidFeedConnection();

        feedConnection.setUrl("http://foo.bar.ham.eggs.com");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    @Test(expected = RuntimeException.class)
    public void testValidateFeedConnectionNonFeedUrl() throws Exception {
        Connection feedConnection = createValidFeedConnection();

        feedConnection.setUrl("http://www.google.com");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    private Connection createValidFeedConnection() {
        Account testAccount = new Account.Builder()
                .url("http://nodeable.com")
                .description("Nodeable Test Account")
                .name("Nodeable Testing")
                .build();

        User testUser = new User.Builder()
                .account(testAccount)
                .accountLocked(false)
                .accountOriginator(true)
                .fullname("Nodeable Test User")
                .username("test_user_" + new Date().getTime() + "@nodeable.com")
                .build();

        return new Connection.Builder()
                .alias("[EC2] North California Status")
                .description("Reports the status of Amazon Elastic Compute Cloud (N. California).")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .hashtag("US-CA")
                .url("http://status.aws.amazon.com/rss/ec2-us-west-1.rss")
                .authType(AuthType.NONE)
                .user(testUser)
                .build();
    }

}
