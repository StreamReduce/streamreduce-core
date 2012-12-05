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
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * I need to revisit this once the client has better support for reading files. @NJH
 */
public class OutboundStorageSobaMessageToWebHDFSIT extends AbstractServiceTestCase {

    @Autowired
    OutboundStorageService outboundStorageService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testEndToEndForProcessedMessages() throws Exception {
        /*
        Connection feedConnection =
                TestUtils.createTestFeedConnection();
        Connection webHDFSConection = TestUtils.createOutboundWebHDFSConnection();

        //Let ConnectionService take care of creating the Id
        feedConnection.setId(null);
        webHDFSConection.setId(null);

        //Make sure user/account are set to persisted values on the connections
        feedConnection.setUser(testUser);
        feedConnection.setAccount(testAccount);
        webHDFSConection.setUser(testUser);
        webHDFSConection.setAccount(testAccount);

        Connection createdCloudConnection = connectionService.createConnection(webHDFSConection);

        //Reconfigure outboundConfiguration on the feed Connection
        feedConnection.setOutboundConfigurations(Sets.newHashSet(
                new OutboundConfiguration.Builder()
                        .connection(webHDFSConection)
                        .protocol("webhdfs")
                        .dataTypes(OutboundDataType.PROCESSED)
                        .build()
        ));

        //Make the feed connection have a last_activity_poll from before the feed messages
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String feb282012TimeStamp = Long.toString(sdf.parse("2012-02-28").getTime());
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("last_activity_poll", feb282012TimeStamp);
        feedConnection.setMetadata(metadata);

        //Create the feed connection and fire the job to create messages
        Connection createdFeedConnection = connectionService.createConnection(feedConnection);
        connectionService.fireOneTimeHighPriorityJobForConnection(createdCloudConnection);

        //Chill a few seconds and let the job and outbound service do their work.
        //It's not very fast...
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        WebHDFSClient webHDFSClient = new WebHDFSClient(webHDFSConection);

        //List<Blob> blobs = webHDFSClient.list();
        //Assert.assertEquals(3, blobs.size());
        */
    }
}
