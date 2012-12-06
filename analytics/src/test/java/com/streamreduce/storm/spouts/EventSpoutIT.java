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

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.tuple.Values;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.mongodb.BasicDBObject;
import com.streamreduce.storm.MockOutputCollector;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tests that {@link EventSpout} works as expected.
 */
public class EventSpoutIT {

    /**
     * Tests {@link EventSpout} when a date is passed into its constructor.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventSpoutWithDate() throws Exception {
        MockOutputCollector outputCollector = new MockOutputCollector();
        final Date lastEventDate = new Date(System.currentTimeMillis() - 10000); // 10s ago
        EventSpout spout = new EventSpout(lastEventDate);

        spout.open(null, null, new SpoutOutputCollector(outputCollector));

        Assert.assertEquals(0,
                            Collections2.filter(spout.getQueue(), new Predicate<BasicDBObject>() {
            @Override
            public boolean apply(BasicDBObject basicDBObject) {
                Date eventDate = new Date(basicDBObject.getLong("timestamp"));

                return eventDate.before(lastEventDate);
            }
        }).size());
    }

    /**
     * Tests {@link EventSpout} works as expected.  To perform this test,
     * we'll call {@link com.streamreduce.storm.spouts.EventSpout#nextTuple()}
     * just as Storm does and ensure that the events emitted are of the
     * proper number and stream id.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore("Ignored until EventService and Event model objects can be properly factored/replaced in analytics tests.")
    public void testEventSpout() throws Exception {
//        EventService eventService = applicationManager.getEventService();
//        List<Event> allEvents = new ArrayList<Event>();
        MockOutputCollector outputCollector = new MockOutputCollector();
        EventSpout spout = new EventSpout();
        List<BasicDBObject> allRawEvents = new ArrayList<>();

        spout.open(null, null, new SpoutOutputCollector(outputCollector));

        // Add all events
        allRawEvents.addAll(spout.getQueue());

        // Filter out any events in the raw events after the last event date in the
        // events returned from the service.  (This is to work around a potential
        // situation where an event(s) is fired in between getting events via the
        // EventService and via the EventSpout.
        final Date lastEventDate = new Date(allRawEvents.get(allRawEvents.size() - 1).getLong("timestamp"));

//        allEvents.addAll(Collections2.filter(eventService.getEventsForAccount(null), new Predicate<Event>() {
//            @Override
//            public boolean apply(Event event) {
//                Date eventDate = new Date(event.getTimestamp());
//
//                return !eventDate.after(lastEventDate);
//            }
//        }));
//
//        // Make sure the events in the queue and the events in the DB collection are the same
//        Assert.assertEquals(allEvents.size(), allRawEvents.size());

        // Exhaust the queue
        while (!spout.isQuiet()) {
            spout.nextTuple();
            if (Math.random() < 0.3) {
                BasicDBObject last = (BasicDBObject) outputCollector.getLastEmmitedValue().get(0);
                Assert.assertNotNull(last);
                spout.fail(last.get("_id"));
            }
        }

        // Wait until we re-poll for new events
        while(spout.isQuiet()) {
            Thread.sleep(10000);

            spout.nextTuple();
        }

        // Get expected total of events
        int totalEventsEmitted = allRawEvents.size() + spout.getQueue().size();

        // Exhaust the queue
        while (!spout.isQuiet()) {
            spout.nextTuple();
        }

        // Make sure events processed count is as expected
        Assert.assertEquals(totalEventsEmitted, outputCollector.getEmittedSpoutValues().size());

        // Validate the events were sent to the proper stream
        for (Map.Entry<String, List<Values>> entry : outputCollector.getEmittedSpoutValuesMap().entrySet()) {
            String streamId = entry.getKey();
            List<Values> events = entry.getValue();

            for (Values values : events) {
                BasicDBObject event = (BasicDBObject)values.get(0);

                Assert.assertEquals(streamId, ((BasicDBObject)event.get("metadata")).getString("targetType"));
            }
        }
    }

}
