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

package com.streamreduce.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.Constants;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.MessageService;
import com.streamreduce.core.service.UserService;
import com.streamreduce.test.service.TestService;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that exercises different permutations of
 * {@link com.streamreduce.core.service.MessageService#getAllMessages(com.streamreduce.core.model.User, Long, Long, int, boolean, String, java.util.List, String, boolean)}.
 */
public class MessageServiceGetMessagesITCase extends AbstractServiceTestCase {

    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private TestService testService;


    private User user;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        user = userService.getUser(Constants.NODEABLE_SUPER_USERNAME);
    }

    @Test
    public void testGetAllMessagesAscOrder() throws Exception {
        List<SobaMessage> messages = messageService.getAllMessages(user, null, null, 4, true, null, null, null, false);
        Assert.assertEquals(2, messages.size());

        // test the order, should be ascending
        long lastDate = 0;
        for (SobaMessage message : messages) {
            if (lastDate != 0) {
                logger.debug(message.getCreated() + ":" + message.getTransformedMessage());
                Assert.assertTrue(message.getCreated() >= lastDate);
            }
            lastDate = message.getCreated();
        }

    }

    @Test
    public void testGetAllMessagesDescOrder() throws Exception {
        List<SobaMessage> messages = messageService.getAllMessages(user, null, null, 4, false, null, null, null, false);
        Assert.assertEquals(2, messages.size());

        // test the order, should be descending
        long lastDate = 0;
        for (SobaMessage message : messages) {
            if (lastDate != 0) {
                logger.debug(message.getCreated() + ":" + message.getTransformedMessage());
                Assert.assertTrue(message.getCreated() <= lastDate);
            }
            lastDate = message.getCreated();
        }
    }

    @Test
    public void testGetAllMessagesSearchByHashtagORSender() throws Exception {
        List<SobaMessage> messages = messageService.getAllMessages(user, null, null, 4, false, null, Lists.newArrayList("#foo"), "nodeable", false);
        Assert.assertEquals(2, messages.size());
    }

    @Test
    public void testGetAllMessagesShowsPrivatesForOwner() throws Exception {
        long beforeCreateConnectionTimestamp = System.currentTimeMillis();
        testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);

        // We expect 3 messages for the testUser since they are private and only testUser should see them.
        List<SobaMessage> messages = messageService.getAllMessages(testUser, beforeCreateConnectionTimestamp, null, 5, false, null, null, null, false);
        Assert.assertEquals(3, messages.size());

        // The super user should not see those same messages.
        User superUser = userService.getSuperUser();
        superUser.setAccount(testUser.getAccount());
        List<SobaMessage> messagesForSuperUser = messageService.getAllMessages(superUser, beforeCreateConnectionTimestamp, null, 5, false, null, null, null, false);
        Assert.assertEquals(0, messagesForSuperUser.size());
    }

    @Test
    public void testGetAllMessagesSearchParameterForTransformedMessage() throws Exception {
        testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);

        //Search for all messages that contain the text "operating".  There is exactly one message in the sample feed that has "operating"
        List<SobaMessage> messages = messageService.getAllMessages(testUser, null, null, 50, false, "operating", null, null, false);
        Assert.assertEquals(1, messages.size());
    }

    @Test
    public void testGetAllMessagesSearchParameterForSenderName() throws Exception {
        Connection c = testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);

        //Search for all messages from the Connection.alias sender name.  There should be 3 total (2 feed entries and 1 for creation).
        List<SobaMessage> messages = messageService.getAllMessages(testUser, null, null, 50, false, c.getAlias(), null, null, false);
        Assert.assertEquals(3, messages.size());
    }


    @Test
    public void testGetMessagesExcludesInsights() throws Exception {
        // Create a connection just to get some messages to assert against... this should create 3 messages for the testUser's account
        long beforeCreateConnectionTimestamp = System.currentTimeMillis();
        testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);

        // Send one insight message for the 4th message
        sendInsightMessage(beforeCreateConnectionTimestamp - 10);

        // We expect 3 messages for the testUser since we are are excluding Nodeables/Inishgts
        List<SobaMessage> messages = messageService.getAllMessages(testUser, beforeCreateConnectionTimestamp, null, 5, false, "", null, null, true);
        Assert.assertEquals(3, messages.size());

        //Not excluding Nodeables should give us 4 messages now
        List<SobaMessage> messagesWithNodeables = messageService.getAllMessages(testUser, beforeCreateConnectionTimestamp, null, 5, false, "", null, null, false);
        Assert.assertEquals(4, messagesWithNodeables.size());
    }

    @Test
    public void testSendNodebellyAnalyticsMessage() throws Exception {
        long sampleTime = System.currentTimeMillis();
        sendInsightMessage(sampleTime);

        //Look for insight messages with the #insight tag
        List<SobaMessage> messagesWithNodeables = messageService.getAllMessages(testUser, sampleTime - 1000, null, 5, false, "", Lists.newArrayList("#insight"), null, false);
        Assert.assertEquals(1, messagesWithNodeables.size());
    }


    @Test
    public void testGetAllMessagesSearchParameterDoesNotRevealPrivates() throws Exception {
        long beforeCreateConnectionTimestamp = System.currentTimeMillis();
        Connection c = testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);


        // The super user should not see those same messages.
        User superUser = userService.getSuperUser();
        superUser.setAccount(testUser.getAccount());
        List<SobaMessage> messagesForSuperUser = messageService.getAllMessages(superUser, beforeCreateConnectionTimestamp, null, 5, false, c.getAlias(), null, null, false);
        Assert.assertEquals(0, messagesForSuperUser.size());
    }

    @Test
    public void testGetAllMessagesReturnsInsightsAndConversations() throws Exception {
        //ElasticSearch can't be implemented soon enough.

        //create a Connection to generate messages, send an insight message, and a user message
        long messageSendTime = System.currentTimeMillis();
        testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);
        sendInsightMessage(messageSendTime);
        sendUserMessage(messageSendTime);

        //ensure that when searching for #insight and conversation we do an OR on #insight and #conversation (instead of AND)
        //and that the connection messages do not appear
        List<SobaMessage> messages = messageService.getAllMessages(testUser, messageSendTime, null, 5, false, null, Lists.newArrayList("#insight", "#conversation"), null, false);
        Assert.assertEquals(2, messages.size());
    }


    private void sendInsightMessage(long sampleTime) {
        // Send one insight message for the 4th message
        Event event = new Event();
        Map<String, Object> metadata = Maps.newHashMap();
        metadata.put("targetType", "feed");
        metadata.put("targetProviderId", "rss");
        metadata.put("name", "someMetric");
        metadata.put("timestamp", sampleTime);
        event.setAccountId(testUser.getAccount().getId());
        event.setMetadata(metadata);
        event.setEventId(EventId.NODEBELLY_SUMMARY);
        messageService.sendNodebellyInsightMessage(event, sampleTime, Sets.newHashSet("#foo"));
    }

    private void sendUserMessage(long sampleTime) throws Exception {
        // Send one insight message for the 4th message
        Event event = new Event();
        Map<String, Object> metadata = Maps.newHashMap();
        metadata.put("targetType", "when_can_we_have_elastic_search_please?");
        metadata.put("targetProviderId", "when_can_we_have_elastic_search_please?");
        metadata.put("name", "when_can_we_have_elastic_search_please?");
        metadata.put("timestamp", sampleTime);
        event.setAccountId(testUser.getAccount().getId());
        event.setMetadata(metadata);
        event.setEventId(EventId.NODEBELLY_SUMMARY);
        messageService.sendUserMessage(event, testUser, "All praise be to Shay Banon");
    }

}
