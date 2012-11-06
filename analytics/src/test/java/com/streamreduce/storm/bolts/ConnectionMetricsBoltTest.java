package com.streamreduce.storm.bolts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.core.event.EventId;
import com.streamreduce.util.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link ConnectionMetricsBolt} works properly.
 */
public class ConnectionMetricsBoltTest extends AbstractMetricsBoltTest {

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMetricsBolt getBolt() {
        return new ConnectionMetricsBolt();
    }

    /**
     * Make sure account event handling works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventHandling() throws Exception {
        Map<String, Float> metrics = processEvents(getEventsForTesting());
        Map<String, Integer> providerIdMap = new HashMap<String, Integer>();
        Map<String, Integer> providerTypeMap = new HashMap<String, Integer>();
        Map<String, Integer> hashtagMap = new HashMap<String, Integer>();

        for (Map.Entry<String, Float> metric : new TreeMap<String, Float>(metrics).entrySet()) {
            String metricId = metric.getKey();
            Float metricValue = metric.getValue();

            // Ensure the calculated values are as expected for CONNECTION_COUNT and CONNECTION_ACTIVITY_COUNT
            if (metricId.contains(MetricName.CONNECTION_COUNT.toString())) {
                Assert.assertEquals(new Float(0), metricValue);
            } else if (metricId.contains(MetricName.CONNECTION_ACTIVITY_COUNT.toString())) {
                if (metricId.equals("global.CONNECTION_ACTIVITY_COUNT")) {
                    Assert.assertEquals(new Float(6), metricValue);
                } else {
                    Assert.assertEquals(new Float(1), metricValue);
                }
            }

            // Calculate the number of HASHTAG, PROVIDER_ID and PROVIDER_TYPE criteria are reached for each provider
            // id and connection type to ensure the proper number of metrics are created based on provider id and
            // provider type.
            for (String providerId : ProviderIdConstants.ALL_PROVIDER_IDS) {
                if (metricId.contains(providerId)) {
                    if (metricId.contains(MetricCriteria.HASHTAG + "=#" + providerId)) {
                        hashtagMap.put(providerId, hashtagMap.get(providerId) != null ?
                                hashtagMap.get(providerId) + 1 :
                                1);
                    } else if (metricId.contains(MetricCriteria.PROVIDER_ID + "=" + providerId)) {
                        providerIdMap.put(providerId, providerIdMap.get(providerId) != null ?
                                providerIdMap.get(providerId) + 1 :
                                1);
                    }
                }
            }

            for (String providerType : ConnectionTypeConstants.ALL_CONNECTION_TYPES) {
                if (metricId.contains(providerType)) {
                    if (metricId.contains(MetricCriteria.HASHTAG + "=#" + providerType)) {
                        hashtagMap.put(providerType, hashtagMap.get(providerType) != null ?
                                hashtagMap.get(providerType) + 1 :
                                1);
                    } else if (metricId.contains(MetricCriteria.PROVIDER_TYPE + "=" + providerType)) {
                        providerTypeMap.put(providerType, providerTypeMap.get(providerType) != null ?
                                providerTypeMap.get(providerType) + 1 :
                                1);
                    }
                }
            }
        }

        for (Map<String, Integer> map : ImmutableSet.of(hashtagMap, providerIdMap, providerTypeMap)) {
            for (Map.Entry<String, Integer> mapEntry : map.entrySet()) {
                String key = mapEntry.getKey();
                Integer value = mapEntry.getValue();
                Integer expectedValue = 8;

                // AWS/Cloud connections do not keep track of CONNECTION_ACTIVITY_COUNT by provider id or provider type
                // since we initiate all activity by our polling.  Since each other provider emits 4 metrics related to
                // provider id/type, the aws/cloud counts should be 4 fewer.
                if (key.equals(ProviderIdConstants.AWS_PROVIDER_ID) || key.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
                    expectedValue = 4;
                }

                Assert.assertEquals(expectedValue, value);
            }
        }

        // TODO: Extend tests to validate CONNECTION_RESOURCE_USAGE
    }

    /**
     * Return a list of mocked connection events for testing.
     *
     * @return the list of mock events
     *
     * @throws Exception if anything goes wrong
     */
    public List<Map<String, Object>> getEventsForTesting() throws Exception {
        List<Map<String, Object>> allEvents = new ArrayList<Map<String, Object>>();

        // Connection ACTIVITY payloads

        // Create payload for IMG/custom connection activity
        Map<String, Object> customActivityPayload = new HashMap<String, Object>();
        Set<Object> metrics = new HashSet<Object>();

        for (int i = 0; i < 2; i++) {
            Map<String, Object> metric = new HashMap<String, Object>();

            metric.put("name", "Test Metric" + (i + 1));
            metric.put("type", "ABSOLUTE");
            metric.put("value", (i + 1));

            metrics.add(metric);
        }

        customActivityPayload.put("metrics", metrics);

        // Create payload for Twitter connection activity
        Map<String, Object> twitterActivityPayload = ((BasicDBObject) JSON.parse(
                JSONUtils.readJSONFromClasspath("/com/streamreduce/storm/bolts/NodeableTwitterProfile.json"))).toMap();

        // Connection events
        Map<EventId, Map<String, Object>> cloudEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.CLOUD_TYPE,
                                                      ProviderIdConstants.AWS_PROVIDER_ID, null);
        Map<EventId, Map<String, Object>> feedEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.FEED_TYPE,
                                                      ProviderIdConstants.FEED_PROVIDER_ID, null);
        Map<EventId, Map<String, Object>> monitoringEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.MONITORING_TYPE,
                                                      ProviderIdConstants.PINGDOM_PROVIDER_ID,
                                                      null);
        Map<EventId, Map<String, Object>> customEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.GATEWAY_TYPE,
                                                      ProviderIdConstants.CUSTOM_PROVIDER_ID,
                                                      customActivityPayload);
        Map<EventId, Map<String, Object>> projectHostingEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.PROJECT_HOSTING_TYPE,
                                                      ProviderIdConstants.GITHUB_PROVIDER_ID,
                                                      null);
        Map<EventId, Map<String, Object>> socialEvents =
                createConnectionOrInventoryItemEvents("Connection", ConnectionTypeConstants.SOCIAL_TYPE,
                                                      ProviderIdConstants.TWITTER_PROVIDER_ID,
                                                      twitterActivityPayload);


        allEvents.addAll(cloudEvents.values());
        allEvents.addAll(feedEvents.values());
        allEvents.addAll(monitoringEvents.values());
        allEvents.addAll(customEvents.values());
        allEvents.addAll(projectHostingEvents.values());
        allEvents.addAll(socialEvents.values());

        return allEvents;
    }

}
