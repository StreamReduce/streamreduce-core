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
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.User;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.ResourceBundle;

/**
 * Tests for {@link TwitterClient}.
 */
public class TwitterClientIT {

    private ResourceBundle twitterProperties = ResourceBundle.getBundle("twitter");
    private String twitterOAuthToken = twitterProperties.getString("nodeable.integrations.twitter.oauth_token");
    private String twitterOAuthSecret = twitterProperties.getString("nodeable.integrations.twitter.oauth_secret");
    private Connection connection;

    @Before
    public void setUp() throws Exception {
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

        ConnectionCredentials credentials = new ConnectionCredentials();

        credentials.setOauthToken(twitterOAuthToken);
        credentials.setOauthTokenSecret(twitterOAuthSecret);

        connection = new Connection.Builder()
                .credentials(credentials)
                .provider(ConnectionProvidersForTests.TWITTER_PROVIDER)
                .alias("Test Twitter Connection")
                .authType(AuthType.OAUTH)
                .user(testUser)
                .build();
    }

    /**
     * Tests that {@link com.streamreduce.util.TwitterClient#getLoggedInProfile()} works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetLoggedInProfile() throws Exception {
        TwitterClient client = new TwitterClient(connection,ConnectionProvidersForTests.TWITTER_PROVIDER.getOAuthService());
        JSONObject profile = client.getLoggedInProfile();

        Assert.assertNotNull(profile);
        Assert.assertTrue(profile.containsKey("screen_name"));
        Assert.assertTrue(profile.getString("screen_name").equals("Nodeable"));
    }

}
