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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableSet;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.event.EventId;
import com.streamreduce.storm.MockOutputCollector;
import org.bson.types.ObjectId;
import org.mockito.Mockito;

/**
 * Class all tests will extend if the bolt being tested extends {@link AbstractMetricsBolt}.
 */
public abstract class AbstractMetricsBoltTest {

    protected MockOutputCollector outputCollector;

    /**
     * Returns an instance of the bolt being tested.
     *
     * @return the bolt
     */
    public abstract AbstractMetricsBolt getBolt();

    /**
     * Takes a list of mock events, executes them using the bolt specified and returns the calculated metrics.
     *
     * @param events the events
     *
     * @return the map of calculated events
     */
    protected Map<String, Float> processEvents(List<Map<String, Object>> events) {
        Map<String, Float> metricCounts = new HashMap<String, Float>();
        AbstractMetricsBolt bolt = getBolt();

        outputCollector = new MockOutputCollector();

        // Prepare the bolt so that it uses our mock output collector
        bolt.prepare(null, null, new OutputCollector(outputCollector));

        // Emit all events
        for (Map<String, Object> event : events) {
            Tuple tuple = Mockito.mock(Tuple.class);

            Mockito.when(tuple.getValue(0)).thenReturn(event);

            bolt.execute(tuple);
        }

        // Turn all emitted metrics into the metrics map
        for (Values metric : outputCollector.getEmittedValues()) {
            String accountId = metric.get(0).toString();
            String metricId = metric.get(6).toString();
            Float metricValue = (Float)metric.get(5);
            String mapKey = accountId + "." + metricId;
            Float mapValue = metricCounts.containsKey(mapKey) ? metricCounts.get(mapKey) : 0f;

            metricCounts.put(mapKey, mapValue += metricValue);
        }

        return metricCounts;
    }

    /**
     * Helper to create a full cycle of CRUD events for a target type.
     *
     * @param targetType the target type
     *
     * @return map where each CRUD event is the key and the object for said event is the value
     */
    protected Map<EventId, Map<String, Object>> createCRUDEvents(String targetType) {
        Map<String, Object> createEvent = createBaseEventMock(EventId.CREATE, targetType, null);
        Map<String, Object> readEvent = createBaseEventMock(EventId.READ, createEvent.get("accountId").toString(),
                                                            createEvent.get("userId").toString(),
                                                            createEvent.get("targetId").toString(), targetType, null);
        Map<String, Object> updateEvent = createBaseEventMock(EventId.UPDATE, createEvent.get("accountId").toString(),
                                                              createEvent.get("userId").toString(),
                                                              createEvent.get("targetId").toString(), targetType, null);
        Map<String, Object> deleteEvent = createBaseEventMock(EventId.DELETE, createEvent.get("accountId").toString(),
                                                              createEvent.get("userId").toString(),
                                                              createEvent.get("targetId").toString(), targetType, null);

        Map<EventId, Map<String, Object>> crudEvents = new HashMap<EventId, Map<String, Object>>();

        crudEvents.put(EventId.CREATE, createEvent);
        crudEvents.put(EventId.READ, readEvent);
        crudEvents.put(EventId.UPDATE, updateEvent);
        crudEvents.put(EventId.DELETE, deleteEvent);

        return crudEvents;
    }

    /**
     * Returns a {@link com.mongodb.BasicDBObject} that has the default structure required to mirror
     * an {@link com.streamreduce.core.model.Event}.
     *
     * @param eventId the event id
     * @param targetType the event's target type
     * @param metadata the event's metadata
     *
     * @return the base event
     */
    protected Map<String, Object> createBaseEventMock(EventId eventId, String targetType,
                                                      Map<String, Object> metadata) {
        return createBaseEventMock(eventId, null, null, null, targetType, metadata);
    }

    /**
     * Returns a {@link com.mongodb.BasicDBObject} that has the default structure required to mirror
     * an {@link com.streamreduce.core.model.Event}.
     *
     * @param eventId the {@link EventId} of the event
     * @param accountId the account id of the event
     * @param userId the user id of the event
     * @param targetId the target id of the event
     * @param targetType the target type of the event
     *
     * @return the base event
     */
    protected Map<String, Object> createBaseEventMock(EventId eventId, String accountId, String userId,
                                                      String targetId, String targetType,
                                                      Map<String, Object> metadata) {
        Map<String, Object> baseEvent = new HashMap<String, Object>();

        baseEvent.put("_id", new ObjectId().toString());
        baseEvent.put("timestamp", new Date().getTime());
        baseEvent.put("eventId", eventId);
        baseEvent.put("accountId", accountId != null ? accountId : new ObjectId().toString());
        baseEvent.put("userId", userId != null ? userId : new ObjectId().toString());
        baseEvent.put("targetId", targetId != null ? targetId : new ObjectId().toString());

        if (metadata == null) {
            metadata = new HashMap<String, Object>();
        }

        if (targetType != null) {
            metadata.put("targetType", targetType);
        }

        baseEvent.put("metadata", metadata);

        return baseEvent;
    }

    /**
     * Helper to create a full cycle of CRUD events for a connection/inventory item and an ACTIVITY event.
     *
     * @param targetType the target's type
     * @param providerType the target's provider type
     * @param providerId the target's provider id
     * @param payload the target's activity payload
     *
     * @return map where each CRUD event is the key and the object for said event is the value
     */
    protected Map<EventId, Map<String, Object>> createConnectionOrInventoryItemEvents(String targetType,
                                                                                      String providerType,
                                                                                      String providerId,
                                                                                      Map<String, Object> payload) {
        Map<EventId, Map<String, Object>> allEvents = createCRUDEvents(targetType);
        Map<String, Object> metadata = new HashMap<String, Object>();

        metadata.put("targetProviderId", providerId);
        metadata.put("targetProviderType", providerType);
        metadata.put("targetHashtags", ImmutableSet.of('#' + providerId, '#' + providerType));

        if (targetType.equals("InventoryItem")) {
            metadata.put("targetConnectionId", new ObjectId().toString());

            if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                metadata.put("targetISO3166Code", "US-CA");
                metadata.put("targetRegion", "us-west-1");
                metadata.put("targetZone", "us-west-1a");
                metadata.put("targetOS", "AMZN_LINUX");
            }
        }

        for (Map.Entry<EventId, Map<String, Object>> crudEvent : allEvents.entrySet()) {
            Map<String, Object> eventMetadata = (Map<String, Object>)crudEvent.getValue().get("metadata");

            eventMetadata.putAll(metadata);

            // Since the hashtag handling in the AbstractMetricsBolt requires a database entry to handle things
            // properly, just remove hashtags from the update event so that an update doesn't produce any unnecessary
            // hashtag-related metrics.
            if (crudEvent.getKey() == EventId.UPDATE) {
                eventMetadata.remove("targetHashtags");
            }

            crudEvent.getValue().put("metadata", eventMetadata);
        }

        Map<String, Object> createEvent = allEvents.get(EventId.CREATE);
        Map<String, Object> activityEvent = createBaseEventMock(EventId.ACTIVITY,
                                                                createEvent.get("accountId").toString(),
                                                                createEvent.get("userId").toString(),
                                                                createEvent.get("targetId").toString(),
                                                                targetType,
                                                                (Map<String, Object>)createEvent.get("metadata"));
        Map<String, Object> activityMetadata = (Map<String, Object>)activityEvent.get("metadata");

        if (payload !=  null) {
            activityMetadata.put("payload", payload);
        }

        activityEvent.put("metadata", activityMetadata);

        allEvents.put(EventId.ACTIVITY, activityEvent);

        return allEvents;
    }

}
