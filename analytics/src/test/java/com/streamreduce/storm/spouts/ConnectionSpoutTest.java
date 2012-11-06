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

package com.streamreduce.storm.spouts;

import com.mongodb.BasicDBObject;
import com.streamreduce.storm.MongoClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionSpoutTest {

    ConnectionSpout connectionSpout;

    @Before
    public void setUp() throws Exception {
        MongoClient mockMongoClient = mock(MongoClient.class);
        when(mockMongoClient.getConnections()).thenThrow(new RuntimeException("mongo failure"));
        when(mockMongoClient.getConnection(anyString())).thenThrow(new RuntimeException("mongo failure"));

        connectionSpout = new ConnectionSpout();
        ReflectionTestUtils.setField(connectionSpout, "mongoClient", mockMongoClient);
    }

    @Test
    public void testGetDBEntries() throws Exception {
        try {
            List<BasicDBObject> dbObjects = connectionSpout.getDBEntries();
            assertEquals(0, dbObjects.size());
        } catch (Exception e) {
            fail("Did not catch unexpected exception.");
        }
    }

    @Test
    public void testGetDBEntry() throws Exception {
        try {
            BasicDBObject dbObject = connectionSpout.getDBEntry("blah");
            assertNull(dbObject);
        } catch (Exception e) {
            fail("Did not catch unexpected exception.");
        }
    }
}
