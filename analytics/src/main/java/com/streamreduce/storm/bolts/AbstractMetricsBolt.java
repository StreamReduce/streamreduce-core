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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.Constants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.metric.MetricModeType;
import com.streamreduce.storm.MongoClient;
import com.streamreduce.storm.utils.MetricsUtils;
import org.apache.log4j.Logger;

/**
 * Class that all metrics bolts will extend.
 */
public abstract class AbstractMetricsBolt extends NodeableUnreliableBolt {

    private static final MongoClient MESSAGE_DB_MONGO_CLIENT = new MongoClient(MongoClient.MESSAGEDB_CONFIG_ID);
    private static final Logger LOGGER = Logger.getLogger(AbstractMetricsBolt.class);
    private static final String GLOBAL_ACCOUNT_ID = "global";

    protected static final Map<MetricCriteria, String> EMPTY_CRITERIA = Collections.emptyMap();

    /**
     * Handler for customized event handling (above and beyond the built-in handling).
     *
     * @param id the event's unique id
     * @param timestamp the event's timestamp
     * @param eventId the event's {@link EventId}
     * @param accountId the event's account id
     * @param userId the event's user id
     * @param targetId the event's target id
     * @param metadata the event's metadata
     */
    public abstract void handleEvent(String id, Long timestamp, EventId eventId, String accountId, String userId,
                                     String targetId, Map<String, Object> metadata);

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields(
                "metricAccount", // The account id the metric is associated with
                "metricName", // The low-level name of the metric
                "metricMode", // The metric type
                "metricCriteria", // The metric criteria
                "metricTimestamp", // The timestemp of the metric
                "metricValue", // The value of the metric
                "metricId" // The unique identifier for the metric
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {
        Map<String, Object> event = (Map<String, Object>) tuple.getValue(0);

        String id = event.get("_id") != null ? event.get("_id").toString() : null;
        Long timestamp = event.get("timestamp") != null ? (Long) event.get("timestamp") : null;
        EventId eventId = event.get("eventId") != null ? EventId.valueOf(event.get("eventId").toString()) : null;
        String accountId = event.get("accountId") != null ?  event.get("accountId").toString() : null;
        String userId = event.get("userId") != null ? event.get("userId").toString() : null;
        String targetId = event.get("targetId") != null ? event.get("targetId").toString() : null;
        Map<String, Object> metadata = event.get("metadata") != null ?
                (Map<String, Object>) event.get("metadata") :
                new HashMap<String, Object>();
        String targetType = metadata.get("targetType") != null ? metadata.get("targetType").toString() : null;

        // Handle type-based counts
        handleObjectCounts(id, timestamp, eventId, accountId, userId, targetId, targetType, metadata);

        // Send down stream to allow for deeper level metrics
        handleEvent(id, timestamp, eventId, accountId, userId, targetId, metadata);
    }

    /**
     * Handler for built-in event handling.  This handler will do CRUD/activity counts based on the event type and
     * event id.
     *
     * @param id the event's unique id
     * @param timestamp the event's timestamp
     * @param eventId the event's {@link EventId}
     * @param accountId the event's account id
     * @param userId the event's user id
     * @param targetId the event's target id
     * @param targetType the event's target type
     * @param metadata the event's metadata
     */
    private void handleObjectCounts(String id, Long timestamp, EventId eventId, String accountId, String userId,
                                    String targetId, String targetType, Map<String, Object> metadata) {
        Float eventValue = getEventValue(eventId);
        MetricName metricName = getEventMetricName(eventId, targetType);

        // Nothing we can do if we cannot figure out the event type or there is no value
        if (Float.isNaN(eventValue) || metricName == null) {
            LOGGER.debug("Unable to calculate built-in metrics: (eventValue=" + eventValue +
                                 ", metricName=" + metricName + ")");
            return;
        }

        List<Values> metrics = new ArrayList<>();
        String providerId = metadata.get("targetProviderId") != null ?
                metadata.get("targetProviderId").toString() :
                null;
        String providerType = metadata.get("targetProviderType") != null ?
                metadata.get("targetProviderType").toString() :
                null;

        switch(metricName) {
            case ACCOUNT_COUNT:
                metrics.add(createBuiltInMetric(GLOBAL_ACCOUNT_ID, metricName, EMPTY_CRITERIA, timestamp,
                                                eventValue));
                break;
            case CONNECTION_COUNT:
            case CONNECTION_ACTIVITY_COUNT:
                if (providerId != null && providerType != null) {
                    for (String account : ImmutableSet.of( GLOBAL_ACCOUNT_ID, accountId )) {
                        metrics.add(createBuiltInMetric(account, metricName, EMPTY_CRITERIA, timestamp,
                                                        eventValue));

                        // For AWS connections, our polling creates activity and is not a useful metric
                        if (!(providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) ||
                                metricName != MetricName.CONNECTION_ACTIVITY_COUNT) {
                            metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                    MetricCriteria.PROVIDER_TYPE, providerType
                            ), timestamp, eventValue));
                        }

                        // For AWS connections, our polling creates activity and is not a useful metric
                        if (!(providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) ||
                                metricName != MetricName.CONNECTION_ACTIVITY_COUNT) {
                            metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                    MetricCriteria.PROVIDER_ID, providerId
                            ), timestamp, eventValue));
                        }

                        // Only emit the connection activity count for a connection id in the account
                        if (account.equals(accountId) && metricName == MetricName.CONNECTION_ACTIVITY_COUNT) {
                            // For AWS connections, our polling creates activity and is not a useful metric
                            if (!providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                                metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                        MetricCriteria.CONNECTION_ID, targetId
                                ), timestamp, eventValue));
                            }
                        }
                    }
                } else {
                    LOGGER.warn(getClass() + " is unable to create built-in metrics for Connection events due to " +
                            "the providerId and/or providerType being null: {providerId=" + providerId + "," +
                            "providerType=" + providerType + "}");
                }

                break;

            case INVENTORY_ITEM_COUNT:
            case INVENTORY_ITEM_ACTIVITY_COUNT:
                if (providerId != null && providerType != null) {
                    // Temporarily set the metricName to CONNECTION_ACTIVITY_COUNT, if necessary, to get all the
                    // necessary CONNECTION_ACTIVITY_COUNT metrics created.
                    metricName = MetricName.CONNECTION_ACTIVITY_COUNT;

                    for (String account : ImmutableSet.of( GLOBAL_ACCOUNT_ID, accountId )) {
                        // For AWS connections, our polling creates activity and is not a useful metric
                        if (!(providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) ||
                                metricName != MetricName.CONNECTION_ACTIVITY_COUNT) {
                            metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                    MetricCriteria.PROVIDER_TYPE, providerType
                            ), timestamp, eventValue));
                        }

                        // For AWS connections, our polling creates activity and is not a useful metric
                        if (!(providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) ||
                                metricName != MetricName.CONNECTION_ACTIVITY_COUNT) {
                            metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                    MetricCriteria.PROVIDER_ID, providerId
                            ), timestamp, eventValue));
                        }

                        // Only emit the connection activity count for a connection id in the account
                        if (account.equals(accountId) && metricName == MetricName.CONNECTION_ACTIVITY_COUNT) {
                            // For AWS connections, our polling creates activity and is not a useful metric
                            if (!providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                                metrics.add(createBuiltInMetric(account, metricName, ImmutableMap.of(
                                        MetricCriteria.CONNECTION_ID, targetId
                                ), timestamp, eventValue));
                            }
                        }
                    }

                    // Set the metricName back to INVENTORY_ITEM_ACTIVITY_COUNT if necessary
                    metricName = MetricName.INVENTORY_ITEM_ACTIVITY_COUNT;

                    // Make sure to emit the inventory item specific count
                    metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                            MetricCriteria.OBJECT_ID, targetId
                    ), timestamp, eventValue));

                    String objectType = getExternalObjectType(metadata);
                    String connectionId = metadata.get("targetConnectionId") != null ?
                            metadata.get("targetConnectionId").toString() :
                            null;

                    // Emit object type values, if available
                    if (objectType != null) {
                        // Global
                        metrics.add(createBuiltInMetric(GLOBAL_ACCOUNT_ID, metricName, ImmutableMap.of(
                                MetricCriteria.OBJECT_TYPE, objectType
                        ), timestamp, eventValue));

                        // Account
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.OBJECT_TYPE, objectType
                        ), timestamp, eventValue));

                        // Connection
                        if (connectionId != null) {
                            metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                    MetricCriteria.CONNECTION_ID, "",
                                    MetricCriteria.OBJECT_TYPE, objectType
                            ), timestamp, eventValue));
                        }
                    }
                } else {
                    LOGGER.warn(getClass() + " is unable to create built-in metrics for InventoryItem events due to " +
                                        "the providerId and/or providerType being null: {providerId=" + providerId +
                                        ",providerType=" + providerType + "}");
                }

                break;

            case MESSAGE_COUNT:
                providerType = metadata.get("messageProviderType") != null ?
                        (String)metadata.get("messageProviderType") :
                        null;
                providerId = metadata.get("messageProviderId") != null ?
                        (String)metadata.get("messageProviderId") :
                        null;

                String originatingEventTargetId = metadata.get("messageEventTargetId") != null ?
                        (String)metadata.get("messageEventTargetId") :
                        null;
                String originatingEventTargetType = metadata.get("messageEventTargetType") != null ?
                        (String)metadata.get("messageEventTargetType") :
                        null;
                String connectionId = metadata.get("messageConnectionId") != null ?
                        (String)metadata.get("messageConnectionId") :
                        null;

                // Global message count
                metrics.add(createBuiltInMetric(GLOBAL_ACCOUNT_ID, metricName, EMPTY_CRITERIA, timestamp, eventValue));

                if (accountId != null) {
                    // Global message count
                    metrics.add(createBuiltInMetric(accountId, metricName, EMPTY_CRITERIA, timestamp, eventValue));

                    if (userId != null) {
                        // Message count per user
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.USER_ID, userId
                        ), timestamp, eventValue));
                    }

                    if (originatingEventTargetId != null &&
                            !originatingEventTargetId.equals(accountId) &&
                            (userId != null && !originatingEventTargetId.equals(userId))) {
                        // Message count per object and type (if this wasn't already counted above)
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.OBJECT_TYPE, originatingEventTargetType,
                                MetricCriteria.OBJECT_ID, originatingEventTargetId
                        ), timestamp, eventValue));
                    }

                    if (connectionId != null && (originatingEventTargetId != null &&
                                connectionId.equals(originatingEventTargetId))) {
                        // Connection specific counts (if this wasn't already counted above)
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.CONNECTION_ID, connectionId
                        ), timestamp, eventValue));
                    }

                    if (providerType != null) {
                        // Global message count per provider type
                        metrics.add(createBuiltInMetric(GLOBAL_ACCOUNT_ID, metricName, ImmutableMap.of(
                                MetricCriteria.PROVIDER_TYPE, providerType
                        ), timestamp, eventValue));
                        // Account message count per provider type
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.PROVIDER_TYPE, providerType
                        ), timestamp, eventValue));
                    }

                    if (providerId != null) {
                        // Global message count per provider id
                        metrics.add(createBuiltInMetric(GLOBAL_ACCOUNT_ID, metricName, ImmutableMap.of(
                                MetricCriteria.PROVIDER_ID, providerId
                        ), timestamp, eventValue));
                        // Account message count per provider id
                        metrics.add(createBuiltInMetric(accountId, metricName, ImmutableMap.of(
                                MetricCriteria.PROVIDER_ID, providerId
                        ), timestamp, eventValue));
                    }
                }

                break;

            case USER_COUNT:
                boolean duplicateUserRequest = (metadata.containsKey("userRequestIsNew") &&
                        !(Boolean)metadata.get("userRequestIsNew"));

                if (!duplicateUserRequest) {
                    for (String account : ImmutableSet.of(GLOBAL_ACCOUNT_ID, accountId)) {
                        metrics.add(createBuiltInMetric(account, metricName, EMPTY_CRITERIA, timestamp, eventValue));

                        if (ImmutableSet.of(EventId.CREATE_USER_INVITE_REQUEST, EventId.CREATE_USER_REQUEST,
                                            EventId.DELETE_USER_INVITE_REQUEST).contains(eventId)) {
                            metrics.add(createBuiltInMetric(account, MetricName.PENDING_USER_COUNT, EMPTY_CRITERIA,
                                                            timestamp, eventValue));
                        }
                    }
                }

                break;
        }

        emitMetricsAndHashagMetrics(metrics, eventId, targetId, targetType, metadata);
    }

    protected void emitMetricsAndHashagMetrics(List<Values> metrics, EventId eventId, String targetId,
                                               String targetType, Map<String, Object> metadata) {
        for (Values metric : metrics) {
            Float metricValue = (Float)metric.get(5);

            // Do not emit the actual 'UPDATE' metrics as they are only useful for the hashtag handling below
            if (metricValue != 0.0f) {
                // Emit the metric
                emitMetric(metric);
            }
        }

        // For all but the 'Account' target type, emit hashtag versions of each metric
        if (!targetType.equals("Account")) {
            String hashtagsKey = targetType.equals("SobaMessage") ? "messageHashtags" : "targetHashtags";
            Map<String, Float> hashtagChanges = getHashtagChanges(eventId, targetId, metadata, hashtagsKey);

            for (Map.Entry<String, Float> hashtagChange : hashtagChanges.entrySet()) {
                for (Values metric : metrics) {
                    MetricName metricName = MetricName.valueOf(metric.get(1).toString());
                    Values metricWithHashtag = (Values)metric.clone();
                    Map<String, String> criteria = (Map<String, String>)metric.get(3);
                    String theHashtag = hashtagChange.getKey();

                    if (criteria.keySet().contains(MetricCriteria.CONNECTION_ID.toString())) {
                        // Hashtag metrics for anything specific to a connection makes no sense
                        continue;
                    } else if (criteria.keySet().contains(MetricCriteria.PROVIDER_ID.toString()) &&
                            ('#' + criteria.get(MetricCriteria.PROVIDER_ID.toString())).equals(theHashtag)) {
                        // We automatically add hashtags for the provider id and storing its hashtag variant is
                        // redundant.  Example: {PROVIDER_ID=github,HASHTAG=#github}
                        continue;
                    } else if (criteria.keySet().contains(MetricCriteria.PROVIDER_TYPE.toString()) &&
                            ('#' + criteria.get(MetricCriteria.PROVIDER_TYPE.toString())).equals(theHashtag)) {
                        // We automatically add hashtags for the provider type and storing its hashtag variant is
                        // redundant.  Example: {PROVIDER_TYPE=cloud,HASHTAG=#cloud}
                        continue;
                    } else if (metricName == MetricName.CONNECTION_ACTIVITY_COUNT &&
                            ImmutableSet.of('#' + ConnectionTypeConstants.CLOUD_TYPE,
                                            '#' + ProviderIdConstants.AWS_PROVIDER_ID).contains(theHashtag)) {
                        // For AWS connections, our polling creates activity and is not a useful metric
                        continue;
                    }

                    criteria.put(MetricCriteria.HASHTAG.toString(), hashtagChange.getKey());

                    metricWithHashtag.set(3, criteria);
                    metricWithHashtag.set(5, hashtagChange.getValue());
                    metricWithHashtag.set(6, MetricsUtils.createUniqueMetricName(metric.get(1).toString(), criteria));

                    emitMetric(metricWithHashtag);
                }
            }
        }
    }

    /**
     * Emits a global metric.
     *
     * @param metricName the metric's name
     * @param metricCriteria the metric's criteria
     * @param metricMode the metric's mode
     * @param timestamp the metric's timestamp
     * @param metricValue the metric's value
     */
    protected void emitGlobalMetric(MetricName metricName, Map<MetricCriteria, String> metricCriteria,
                                    MetricModeType metricMode, Long timestamp, Float metricValue) {
        emitAccountMetric(GLOBAL_ACCOUNT_ID, metricName, metricCriteria, metricMode, timestamp, metricValue);
    }

    /**
     * Emits an account metric.
     *
     * @param accountId
     * @param metricName
     * @param metricCriteria
     * @param metricMode
     * @param timestamp
     * @param metricValue
     */
    protected void emitAccountMetric(String accountId, MetricName metricName,
                                     Map<MetricCriteria, String> metricCriteria, MetricModeType metricMode,
                                     Long timestamp, Float metricValue) {
        emitMetric(createMetric(accountId, metricName, metricCriteria, metricMode, timestamp, metricValue));
    }

    /**
     * Returns a map containing hashtag changes and their respective values.
     *
     * * Map key: The hashtag added/removed
     * * Map value: 1.0 for added hashtags and -1.0 for deleted hashtags
     *
     * @param eventId the event's id
     * @param targetId the event's target id
     * @param metadata the event's metadata
     * @param hashtagsKey the metadata key containing the event's hashtags
     *
     * @return the map
     */
    protected Map<String, Float> getHashtagChanges(EventId eventId, String targetId, Map<String, Object> metadata,
                                                   String hashtagsKey) {
        Float eventValue = getEventValue(eventId);
        Map<String, Float> hashtagChanges = new TreeMap<>();
        Set<String> hashtags = metadata.get(hashtagsKey) != null ?
                (Set<String>)metadata.get(hashtagsKey) :
                Collections.EMPTY_SET;

        if (!Float.isNaN(eventValue)) {
            if (Math.abs(eventValue) == 1.0f) {
                // If the event value is 1.0 or -1.0 (CREATE/DELETE), just process as all added/deleted
                for (String hashtag : hashtags) {
                    hashtagChanges.put(hashtag, eventValue);
                }
            } else if (eventValue == 0.0f) {
                // If the event value is 0.0 (UPDATE), figure out the added/deleted hashtags and process as such
                Integer targetVersion = metadata.get("targetVersion") != null ?
                        (Integer)metadata.get("targetVersion") :
                        0;
                Map<String, Object> previousEvent =
                        MESSAGE_DB_MONGO_CLIENT.getEventForTargetAndVersion(targetId, --targetVersion);
                Map<String, Object> previousMetadata = previousEvent != null && previousEvent.get("metadata") != null ?
                        (Map<String, Object>) previousEvent.get("metadata") :
                        Collections.<String, Object> emptyMap();
                Set<String> previousHashtags = previousMetadata.get(hashtagsKey) != null ?
                        (Set<String>) previousMetadata.get(hashtagsKey) :
                        Collections.EMPTY_SET;
                Set<String> differences = Sets.symmetricDifference(hashtags, previousHashtags);

                for (String hashtag : differences) {
                    if (hashtags.contains(hashtag)) {
                        // Added
                        hashtagChanges.put(hashtag, 1.0f);
                    } else {
                        // Deleted
                        hashtagChanges.put(hashtag, -1.0f);
                    }
                }
            }
        }

        return hashtagChanges;
    }

    /**
     * (USED FOR BUILT-IN METRICS ONLY) Returns a {@link com.streamreduce.core.metric.MetricName} based on the event id and
     * target type.
     *
     * @param eventId the event's id
     * @param targetType the event's target type
     *
     * @return the metric name or null if one cannot be found
     */
    protected MetricName getEventMetricName(EventId eventId, String targetType) {
        // Just in case
        if (eventId == null || targetType == null) {
            return null;
        }

        switch (eventId) {
            case CREATE:
            case UPDATE:
            case DELETE:
                if (targetType.equals("Account")) {
                    return MetricName.ACCOUNT_COUNT;
                } else if (targetType.equals("Connection")) {
                    return MetricName.CONNECTION_COUNT;
                } else if (targetType.equals("InventoryItem")) {
                    return MetricName.INVENTORY_ITEM_COUNT;
                } else if (targetType.equals("SobaMessage")) {
                    return MetricName.MESSAGE_COUNT;
                } else if (targetType.equals("User")) {
                    return MetricName.USER_COUNT;
                }
            case ACTIVITY:
                if (targetType.equals("Connection")) {
                    return MetricName.CONNECTION_ACTIVITY_COUNT;
                } else if (targetType.equals("InventoryItem")) {
                    return MetricName.INVENTORY_ITEM_ACTIVITY_COUNT;
                }
            case CREATE_USER_INVITE_REQUEST:
            case CREATE_USER_REQUEST:
            case DELETE_USER_INVITE_REQUEST:
                return MetricName.USER_COUNT;
        }

        return null;
    }

    /**
     * (USED FOR BUILT-IN METRICS ONLY) Returns a float representing the value of the event.
     *
     * @param eventId the event id
     *
     * @return float value representing the value of the event of {@link Float#NaN} if it's an unhandleable event id
     */
    protected Float getEventValue(EventId eventId) {
        // Just in case
        if (eventId == null) {
            return Float.NaN;
        }

        switch (eventId) {
            case CREATE:
            case ACTIVITY:
            case CREATE_USER_INVITE_REQUEST:
            case CREATE_USER_REQUEST:
                return 1.0f;
            case UPDATE:
                return 0.0f;
            case DELETE:
            case DELETE_USER_INVITE_REQUEST:
                return -1.0f;
            default:
                return Float.NaN;
        }
    }

    /**
     * Returns the external object type or attempts to default to one if one isn't present.
     *
     * @param metadata the metadata used to find or default to the appropriate object type
     *
     * @return the object type found/defaulted to or null if one couldn't be found
     */
    protected String getExternalObjectType(Map<String, Object> metadata) {
        String externalType = metadata.containsKey("targetExternalType") ?
                (String)metadata.get("targetExternalType") :
                null;
        String providerId = (String) metadata.get("targetProviderId");

        if (externalType == null) {
            if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                // Default to compute because prior to having the 'targetExternalType' attribute, everything was compute
                externalType = Constants.COMPUTE_INSTANCE_TYPE;
            } else if (providerId.equals(ProviderIdConstants.GITHUB_PROVIDER_ID) ||
                    providerId.equals(ProviderIdConstants.JIRA_PROVIDER_ID)) {
                externalType = Constants.PROJECT_TYPE;
            } else if (providerId.equals(ProviderIdConstants.CUSTOM_PROVIDER_ID)) {
                externalType = Constants.CUSTOM_TYPE;
            }
        }

        return externalType;
    }

    /**
     * Creates a global metric.
     *
     * @param metricName the metrics's name
     * @param metricCriteria the metric's criteria
     * @param metricMode the metrics mode
     * @param timestamp the metric's timestamp
     * @param metricValue the metrics value
     *
     * @return the metric
     */
    protected Values createGlobalMetric(MetricName metricName, Map<MetricCriteria, String> metricCriteria,
                                        MetricModeType metricMode, Long timestamp, Float metricValue) {
        return createMetric(GLOBAL_ACCOUNT_ID, metricName, metricCriteria, metricMode, timestamp, metricValue);
    }

    /**
     * Creates an account metric.
     *
     * @param accountId the metric's account id
     * @param metricName the metrics's name
     * @param metricCriteria the metric's criteria
     * @param metricMode the metrics mode
     * @param timestamp the metric's timestamp
     * @param metricValue the metrics value
     *
     * @return the metric
     */
    protected Values createAccountMetric(String accountId, MetricName metricName,
                                         Map<MetricCriteria, String> metricCriteria, MetricModeType metricMode,
                                         Long timestamp, Float metricValue) {
        return createMetric(accountId, metricName, metricCriteria, metricMode, timestamp, metricValue);
    }

    /**
     * Emits the metric.
     *
     * @param values the metric
     */
    private void emitMetric(Values values) {
        outputCollector.emit(values);
    }

    /**
     * Specialized helper that prepares a metric to be sent down stream.
     *
     * @param accountId the metric's account id
     * @param metricName the metrics's name
     * @param metricCriteria the metric's criteria
     * @param metricMode the metrics mode
     * @param timestamp the metric's timestamp
     * @param metricValue the metrics value
     *
     * @return the prepared metric
     */
    private Values createMetric(String accountId, MetricName metricName, Map<MetricCriteria, String> metricCriteria,
                                MetricModeType metricMode, Long timestamp, Float metricValue) {
        // Convert the map keys to string to avoid serialization/deserialization issues in Storm
        Map<String, String> massagedCriteria = new LinkedHashMap<>();

        for (Map.Entry<MetricCriteria, String> mapEntry : metricCriteria.entrySet()) {
            massagedCriteria.put(mapEntry.getKey().toString(), mapEntry.getValue());
        }

        // All built-in metrics will be deltas, since they are counts that increment/decrement based on the event
        return new Values(accountId, metricName.toString(), metricMode, massagedCriteria,
                          timestamp, metricValue,
                          MetricsUtils.createUniqueMetricName(metricName.toString(), massagedCriteria));
    }

    /**
     * (USED FOR BUILT-IN METRICS ONLY) Just like {@link #createMetric(String, com.streamreduce.core.metric.MetricName,
     * java.util.Map, com.streamreduce.core.metric.MetricModeType, Long, Float)} but it defaults to
     * {@link MetricModeType#DELTA} since all built-in metrics are counters.
     *
     * @param accountId the metric's account
     * @param metricName the metric's name
     * @param metricCriteria the metric's criteria
     * @param timestamp the metric's timestamp
     * @param metricValue the metric's value
     *
     * @return the prepared built-in metric
     */
    private Values createBuiltInMetric(String accountId, MetricName metricName,
                                       Map<MetricCriteria, String> metricCriteria, Long timestamp, Float metricValue) {
        return createMetric(accountId, metricName, metricCriteria, MetricModeType.DELTA, timestamp, metricValue);
    }

}
