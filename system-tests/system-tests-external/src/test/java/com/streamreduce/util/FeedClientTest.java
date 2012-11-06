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
public class FeedClientTest {

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
