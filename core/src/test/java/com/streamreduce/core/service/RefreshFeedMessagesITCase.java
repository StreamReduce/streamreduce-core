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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.test.service.TestService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.List;

public class RefreshFeedMessagesITCase extends AbstractServiceTestCase{

    @Autowired
    MessageService messageService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    ConnectionService connectionService;

    @Autowired
    TestService testService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testRefreshFeedMessagesSavesFeedEntryDetails() throws Exception {
        //Integration test that ensures that a SobaMessage saved from a feed entry persists its FeedEntryDetails and
        //that those same FeedEntryDetails are retrievable from the MessageService.

        Connection sampleFeedConnection = testService.createSampleRssFeedPrivateConnectionAndRefresh(testUser);

        //Attempt to retrieve these messages, and filter out only those from our connection and that have MessageDetails
        //of type FEED_ENTRY.
        List<SobaMessage> messages = messageService.getAllMessages(testUser,null,null,0,false,null,null,sampleFeedConnection.getId().toString(),false);
        List onlyFeedEntries = Lists.newArrayList(Iterables.filter(messages, new Predicate<SobaMessage>() {
            @Override
            public boolean apply(@Nullable SobaMessage message) {
                return (message != null && message.getDetails() != null) &&
                    message.getDetails().getMessageDetailsType() == MessageDetailsType.FEED_ENTRY;
            }
        }));

        //There should be only be two left.
        Assert.assertEquals(2,onlyFeedEntries.size());
    }
}
