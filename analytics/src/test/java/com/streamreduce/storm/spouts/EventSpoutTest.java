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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.streamreduce.storm.MongoClient;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class EventSpoutTest {

    @Test
    public void testGetDBEntriesSetsCorrectLastProcessedEventDate() {
        Long now = System.currentTimeMillis();
        BasicDBObject earliest = new BasicDBObject("timestamp", now - 10);
        BasicDBObject middle = new BasicDBObject("timestamp", now);
        BasicDBObject latest = new BasicDBObject("timestamp", now + 10);

        MongoClient mockMongoClient = mock(MongoClient.class);
        when(mockMongoClient.getEvents(any(Date.class), any(Date.class))).thenReturn(Lists.newArrayList(latest, earliest, middle));

        Logger mockLogger = mock(Logger.class);

        EventSpout spout = new EventSpout();
        ReflectionTestUtils.setField(spout, "mongoClient", mockMongoClient);
        ReflectionTestUtils.setField(spout, "logger", mockLogger);
        spout.getDBEntries();

        assertEquals(new Date(latest.getLong("timestamp")), spout.getLastProcessedEventDate());
    }

    @Test
    public void testGetDBEntriesGracefullyFails() {
        try {
            MongoClient mockMongoClient = mock(MongoClient.class);
            when(mockMongoClient.readLastProcessedEventDate("EventSpout")).thenThrow(new RuntimeException("mongo failure"));

            EventSpout spout = new EventSpout();
            ReflectionTestUtils.setField(spout, "mongoClient", mockMongoClient);
            List<BasicDBObject> entries = spout.getDBEntries();

            //AssertsEmptyList
            assertEquals(0, entries.size());
        } catch (Exception e) {
            fail("Did not gracefully handle exceptions from mongoClient.");
        }
    }

    @Test
    public void testGetDBEntryGracefullyFails() {
        MongoClient mockMongoClient = mock(MongoClient.class);
        when(mockMongoClient.getEvent(anyString())).thenThrow(new RuntimeException("mongo failure"));

        EventSpout spout = new EventSpout();
        ReflectionTestUtils.setField(spout, "mongoClient", mockMongoClient);
        BasicDBObject basicDBObject  = spout.getDBEntry("");

        assertNull(basicDBObject);
    }
}
