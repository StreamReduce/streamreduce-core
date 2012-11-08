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

package com.streamreduce.storm.bolts;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.storm.MockOutputCollector;
import com.streamreduce.storm.MongoClient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that {@link InternalConnectionInventoryBolt} works properly.
 */
public class InternalConnectionInventoryBoltIT {

    /**
     * Tests {@link InternalConnectionInventoryBolt#execute(backtype.storm.tuple.Tuple)}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore("This test assumes a mongo instance is alive on localhost at 27017.  Ignored until this restriction is lifted")
    public void testExecute() throws Exception {
        MongoClient mongoClient = new MongoClient(MongoClient.BUSINESSDB_CONFIG_ID);
        List<BasicDBObject> connections = mongoClient.getConnections();

        for (BasicDBObject connection : connections) {
            InternalConnectionInventoryBolt bolt = new InternalConnectionInventoryBolt();
            MockOutputCollector outputCollector = new MockOutputCollector();
            Tuple tuple = mock(Tuple.class);
            List<BasicDBObject> expectedInventoryItems = new ArrayList<BasicDBObject>();
            String connectionId = connection.getString("_id");
            final String connectionType = connection.getString("type");

            when(tuple.getValue(0)).thenReturn(connection);

            bolt.prepare(null, null, new OutputCollector(outputCollector));

            bolt.execute(tuple);

            if (connectionType.equals(ConnectionTypeConstants.PROJECT_HOSTING_TYPE)) {
                expectedInventoryItems = mongoClient.getProjectHostingInventoryItems(connectionId);
            } else if (connectionType.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
               expectedInventoryItems = mongoClient.getCloudInventoryItems(connectionId);
            }

            List<Values> emittedTuples = outputCollector.getEmittedValues();

            // Make sure all tuples are acked
            Assert.assertEquals(1, outputCollector.getAckedTuples().size());

            // Make sure the size of the emitted values is right
            Assert.assertEquals(expectedInventoryItems.size(), emittedTuples.size());

            Set<String> connectionTypesWithInventory = ImmutableSet.of(ConnectionTypeConstants.CLOUD_TYPE,
                    ConnectionTypeConstants.PROJECT_HOSTING_TYPE);

            // For connection types without supported inventory, making sure the actual/expected inventory counts
            // are equal is enough.
            if (!connectionTypesWithInventory.contains(connectionType)) {
                continue;
            }

            // Make sure each inventory item in the expected inventory list is in the emitted values
            Iterable<String> expectedKeys = Iterables.transform(expectedInventoryItems,
                                                                new Function<BasicDBObject, String>() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public String apply(BasicDBObject input) {
                    if (connectionType.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
                        return input.getString("nodeId");
                    } else if (connectionType.equals(ConnectionTypeConstants.PROJECT_HOSTING_TYPE)){
                        return input.getString("key");
                    } else {
                        Assert.fail("Unsupported connection provider type: " + connectionType);
                        return null;
                    }
                }
            });
            Set<String> actualKeys = new HashSet<String>();

            for (Values value : emittedTuples) {
                String tType = (String)value.get(0);
                BasicDBObject tConnection = (BasicDBObject)value.get(1);
                BasicDBObject tInventoryItem = (BasicDBObject)value.get(2);

                Assert.assertEquals("internal", tType);
                Assert.assertEquals(connection, tConnection);

                if (connectionType.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
                    actualKeys.add(tInventoryItem.getString("nodeId"));
                } else if (connectionType.equals(ConnectionTypeConstants.PROJECT_HOSTING_TYPE)){
                    actualKeys.add(tInventoryItem.getString("key"));
                } else {
                    Assert.fail("Unsupported connection provider type: " + connectionType);
                }
            }

            Assert.assertTrue(Sets.difference(ImmutableSet.copyOf(actualKeys),
                                              ImmutableSet.copyOf(expectedKeys)).size() == 0);
        }
    }

}
