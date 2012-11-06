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

import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.Constants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.EventService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.MessageService;
import com.streamreduce.util.AWSClient;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.compute.domain.ComputeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class InventoryServiceITCase extends AbstractServiceTestCase {

    @Autowired
    InventoryService inventoryService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    MessageService messageService;
    @Autowired
    EventService eventService;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    private Connection connection;

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connectionService.deleteConnection(connection);
        }
    }

    @Test
    public void testTwitterPolling() throws Exception {
        ConnectionCredentials credentials = new ConnectionCredentials();

        credentials.setOauthToken(twitterProperties.getString("nodeable.integrations.twitter.oauth_token"));
        credentials.setOauthTokenSecret(twitterProperties.getString("nodeable.integrations.twitter.oauth_secret"));

        ConnectionProvider twitterProvider =
                connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.TWITTER_PROVIDER_ID);
        connection = connectionService.createConnection(
                new Connection.Builder()
                        .alias("Test Twitter Connection")
                        .credentials(credentials)
                        .provider(twitterProvider )
                        .user(getTestUser())
                        .authType(AuthType.OAUTH)
                        .build());

        int iterationCount = 0;
        Event taEvent = null;

        while (taEvent == null) {
            Thread.sleep(10000);

            if (iterationCount++ == 5) {
                Assert.fail("Unable to poll Twitter in the allotted amount of time.");
            }

            List<Event> allEvents = eventService.getEventsForAccount(getTestAccount());

            for (Event event : allEvents) {
                if (event.getEventId().equals(EventId.ACTIVITY)) {
                    taEvent = event;
                }
            }
        }

        Map<String, Object> taEventMetadata = taEvent.getMetadata();

        Assert.assertEquals(EventId.ACTIVITY, taEvent.getEventId());
        Assert.assertTrue(taEventMetadata.containsKey("activityTitle"));
        Assert.assertTrue(taEventMetadata.containsKey("activityContent"));
        Assert.assertTrue(taEventMetadata.containsKey("activityPayload"));
    }

    /**
     * Tests that inventory items found/processed by Nodeable end up as expected.
     * <p/>
     * *Note: This test requires that the connection have at least one EC2 inventory item and at least one S3 inventory
     * item.
     *
     * @throws Exception
     */
    @Test
    public void testAWSInventoryPolling() throws Exception {
        ConnectionProvider awsProvider =
                connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.AWS_PROVIDER_ID);
        connection = new Connection.Builder()
                .credentials(new ConnectionCredentials(cloudProperties.getString("nodeable.aws.accessKeyId"),
                        cloudProperties.getString("nodeable.aws.secretKey")))
                .description("This is Nodeable's AWS cloud.")
                .alias("AWS Inventory Polling Connection " + new Date().getTime())
                .provider(awsProvider)
                .user(getTestUser())
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        connectionService.createConnection(connection);

        AWSClient awsClient = new AWSClient(connection);
        int realInventoryItemCount = Iterables.size(Iterables.concat(awsClient.getEC2Instances(),
                awsClient.getS3BucketsAsJson()));
        List<InventoryItem> internalInventoryItems = inventoryService.getInventoryItems(connection);
        int iterationCount = 0;

        while (realInventoryItemCount != internalInventoryItems.size()) {
            Thread.sleep(10000);

            if (iterationCount++ == 12) {
                Assert.fail("Unable to get the internal inventory item count to match the external inventory item " +
                        "count in the time allotted.");
            }

            internalInventoryItems = inventoryService.getInventoryItems(connection);
        }

        boolean foundS3Bucket = false;
        boolean foundEC2Instance = false;

        for (InventoryItem inventoryItem : internalInventoryItems) {
            BasicDBObject metadata = inventoryService.getInventoryItemPayload(inventoryItem);
            String rawType = metadata.getString("type");

            if (inventoryItem.getType().equals(Constants.BUCKET_TYPE)) {
                Assert.assertEquals(StorageType.CONTAINER.toString(), rawType);
                Assert.assertFalse(inventoryItem.getHashtags().contains("#compute"));
                Assert.assertTrue(inventoryItem.getHashtags().contains("#bucket"));
                foundS3Bucket = true;
            } else if (inventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE)) {
                Assert.assertEquals(ComputeType.NODE.toString(), rawType);
                Assert.assertFalse(inventoryItem.getHashtags().contains("#bucket"));
                Assert.assertTrue(inventoryItem.getHashtags().contains("#compute"));
                foundEC2Instance = true;
            }

            if (foundEC2Instance && foundS3Bucket) {
                break;
            }
        }

        Assert.assertTrue(foundEC2Instance && foundS3Bucket);
    }
}
