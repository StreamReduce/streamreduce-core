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

package com.streamreduce.test.service;

import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.FeedProvider;
import com.streamreduce.connections.RssProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Service("testService")
public class TestService {

    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    MessageService messageService;


    private static final String SAMPLE_FEED_FILE_PATH = TestService.class.getResource(
            "/com/streamreduce/rss/sample_EC2.rss").toString();

    /**
     * Creates a test RSS connection, saves it for the passed in User, and refreshesFeedMessages on it.
     * @return
     */
    public Connection createSampleRssFeedPrivateConnectionAndRefresh(User u) throws Exception {
        //First, setup a sample connection.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String feb282012TimeStamp = Long.toString(sdf.parse("2012-02-28").getTime());

        RssProvider mockProvider = mock(RssProvider.class);
        when(mockProvider.getId()).thenReturn(ProviderIdConstants.FEED_PROVIDER_ID);
        when(mockProvider.getType()).thenReturn(FeedProvider.TYPE);

        Connection sampleFeedConnection = new Connection.Builder()
                .provider(mockProvider)
                .url(SAMPLE_FEED_FILE_PATH)
                .alias("EC2")
                .user(u)
                .authType(AuthType.NONE)
                .visibility(SobaObject.Visibility.SELF)
                .build();
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("last_activity_poll", feb282012TimeStamp);
        sampleFeedConnection.setMetadata(metadata);

        sampleFeedConnection = connectionService.createConnection(sampleFeedConnection);
        connectionService.fireOneTimeHighPriorityJobForConnection(sampleFeedConnection);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        return sampleFeedConnection;
    }

}
