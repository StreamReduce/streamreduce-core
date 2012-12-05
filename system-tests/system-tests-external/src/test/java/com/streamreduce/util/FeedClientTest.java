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

import com.streamreduce.core.model.Connection;
import com.streamreduce.test.service.TestUtils;
import org.junit.Test;

import java.net.MalformedURLException;

/**
 * Test class for {@link FeedClient}.
 */
public class FeedClientTest {

    @Test
    public void testValidateFeedConnection() throws Exception {
        FeedClient client = new FeedClient(TestUtils.createTestFeedConnection());
        client.validateConnection();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFeedConnectionNonFeedProvider() throws Exception {
        Connection feedConnection = TestUtils.createTestFeedConnection();

        feedConnection.setProviderId("nonFeedProviderId");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    @Test(expected = MalformedURLException.class)
    public void testValidateFeedConnectionMalformedUrl() throws Exception {
        Connection feedConnection = TestUtils.createTestFeedConnection();

        feedConnection.setUrl("abc123");

        FeedClient client = new FeedClient(feedConnection);

        client.validateConnection();
    }

    @Test(expected = RuntimeException.class)
    public void testValidateFeedConnectionUnavailableUrl() throws Exception {
        Connection feedConnection = TestUtils.createTestFeedConnection();
        feedConnection.setUrl("http://foo.bar.ham.eggs.com");
        FeedClient client = new FeedClient(feedConnection);
        client.validateConnection();
    }

    @Test(expected = RuntimeException.class)
    public void testValidateFeedConnectionNonFeedUrl() throws Exception {
        Connection feedConnection = TestUtils.createTestFeedConnection();
        feedConnection.setUrl("http://www.google.com");
        FeedClient client = new FeedClient(feedConnection);
        client.validateConnection();
    }
}
