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
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.mongodb.BasicDBObject;
import com.streamreduce.storm.GroupingNameConstants;
import com.streamreduce.storm.MongoClient;
import com.streamreduce.storm.Visibility;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Extension of {@link AbstractScheduledDBCollectionSpout} that will
 * emit the {@link com.mongodb.BasicDBObject} representation for each Nodeable Event
 */
public class EventSpout extends AbstractScheduledDBCollectionSpout {

    private static final Logger logger = Logger.getLogger(EventSpout.class);
    private static final long serialVersionUID = -3339407048421810230L;
    // Not final like all others due to EventSpoutTest needing to set mock/set the MongoClient
    private static MongoClient mongoClient = new MongoClient(MongoClient.MESSAGEDB_CONFIG_ID);

    private Date lastProcessedEventDate;

    /**
     * Constructor.  (Calls super constructor with a sleep duration of 15s for now.)
     */
    public EventSpout() {
        super(15000);
    }

    /**
     * Constructor.
     *
     * @param lastProcessedEventDate the date at which we should start polling events from
     */
    public EventSpout(Date lastProcessedEventDate) {
        this();

        this.lastProcessedEventDate = lastProcessedEventDate;
    }

    /**
     * {@inheritDoc}
     */
    public EventSpout(int sleepDuration) {
        super(sleepDuration);
    }

    /**
     * Constructor.
     *
     * @param sleepDuration          the sleep duration in between queue populations
     * @param lastProcessedEventDate the date at which we should start polling events from
     */
    public EventSpout(int sleepDuration, Date lastProcessedEventDate) {
        super(sleepDuration);

        this.lastProcessedEventDate = lastProcessedEventDate;
    }

    public Date getLastProcessedEventDate() {
        return lastProcessedEventDate;
    }

    public void setLastProcessedEventDate(Date lastProcessedEventDate) {
        this.lastProcessedEventDate = lastProcessedEventDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        // added the default stream because storm complained
        outputFieldsDeclarer.declare(new Fields());
        outputFieldsDeclarer.declareStream(GroupingNameConstants.ACCOUNT_GROUPING_NAME, new Fields("event"));
        outputFieldsDeclarer.declareStream(GroupingNameConstants.CONNECTION_GROUPING_NAME, new Fields("event"));
        outputFieldsDeclarer.declareStream(GroupingNameConstants.INVENTORY_ITEM_GROUPING_NAME, new Fields("event"));
        outputFieldsDeclarer.declareStream(GroupingNameConstants.USER_GROUPING_NAME, new Fields("event"));
        outputFieldsDeclarer.declareStream(GroupingNameConstants.MESSAGE_GROUPING_NAME, new Fields("event"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDBEntry(SpoutOutputCollector collector, BasicDBObject entry) {
        BasicDBObject metadata = entry.containsField("metadata") ? (BasicDBObject) entry.get("metadata") : new BasicDBObject();
        String eventType = metadata.getString("targetType");

        // Just in case
        if (eventType == null) {
            // TODO: Figure out the best way to handle this

            // Log the inability to process further
            logger.error("Event with id of " + entry.get("_id") + " has no target type.  Unable to process.");

            // Early return to avoid emitting the event
            return;
        }

        String v = (String) metadata.get("targetVisibility");
        if (v != null) {
            Visibility visibility = Visibility.valueOf(v);
            if (visibility == Visibility.SELF) {
                // do not process private information
                return;
            }
        }

        MongoClient.mapMongoToPlainJavaTypes(entry);
        // Emit the entry to the type-specific stream
        collector.emit(eventType, new Values(entry.toMap()));
        ack(entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BasicDBObject> getDBEntries() {
        try {

            //Get events in a one hour sliding window, or return all events if lastProcessedEventDate is null, which is
            //needed when dealing with a new bootstrapped message db, or a new eventStream.
            Date oneHourAfterLastProcessed = null;
            long lastProcessedEvent = mongoClient.readLastProcessedEventDate("EventSpout");
            if (lastProcessedEvent != -1) {
                lastProcessedEventDate = new Date(lastProcessedEvent);
            }

            logger.info("Determine last processed event date....");

            if (lastProcessedEventDate != null) {
                oneHourAfterLastProcessed = new Date(lastProcessedEventDate.getTime() + TimeUnit.HOURS.toMillis(1));
                logger.info("Retrieving all events between " + lastProcessedEventDate + " and " + oneHourAfterLastProcessed);
            }

            logger.debug("Mongo geEvents Query Time, Begin:" + System.currentTimeMillis());
            List<BasicDBObject> events = mongoClient.getEvents(lastProcessedEventDate, oneHourAfterLastProcessed);
            logger.debug("Mongo geEvents Query Time, End:" + System.currentTimeMillis());

            logger.info("Number of events to be emitted:  " + events.size());

            persistLastProcessedEventDate(oneHourAfterLastProcessed, events);
            return events;
        } catch (Exception e) {
            logger.error("0 events emmitted due to failure in getDBEntries",e);
            return new ArrayList<BasicDBObject>();
        }
    }

    /**
     * Persists the lastProcessedEventDate to the last entry of the passed in events if we had events or to the end of
     * the sliding window if it isn't null.
     *
     * @param endOfSlidingWindow
     * @param events
     */
    private void persistLastProcessedEventDate(Date endOfSlidingWindow, List<BasicDBObject> events) {
        if (events.size() > 0) {
            //If we had events to process, lastProcessedEventDate is the date of the last event.
            BasicDBObject mostRecentEntry = getMostRecentEntryFromEvents(events);

            //events.get(events.size() - 1);
            lastProcessedEventDate = new Date(mostRecentEntry.getLong("timestamp"));
        } else if (endOfSlidingWindow != null &&
                endOfSlidingWindow.getTime() < (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1))) {
            //If we didn't have events, only update lastProcessEventDate to the end our hour sliding window
            //if it is before the current system + a buffer of one minute to avoid updating lastProcessedEventDate to
            //a value in the future.
            lastProcessedEventDate = endOfSlidingWindow;
        }

        if (lastProcessedEventDate != null) {
            mongoClient.updateLastProcessedEventDate("EventSpout", lastProcessedEventDate.getTime());
        }
    }

    private BasicDBObject getMostRecentEntryFromEvents(List<BasicDBObject> events) {
        if (events == null) {
            return null;
        }
        BasicDBObject mostRecent = null;
        for (BasicDBObject event : events) {
            if (event == null) {
                continue;
            }
            if (mostRecent == null) {
                mostRecent = event;
                continue;
            }
            if (event.getLong("timestamp") > mostRecent.getLong("timestamp")) {
                mostRecent = event;
            }
        }
        return mostRecent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicDBObject getDBEntry(String id) {
        try {
            return mongoClient.getEvent(id);
        } catch (Exception e) {
            logger.error("Unable to retrieve DBEntry due to unexpected failure",e);
            return null;
        }
    }
}
