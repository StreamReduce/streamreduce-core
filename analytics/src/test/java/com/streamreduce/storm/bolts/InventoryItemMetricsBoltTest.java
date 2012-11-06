package com.streamreduce.storm.bolts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.event.EventId;
import com.streamreduce.util.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link InventoryItemMetricsBolt} works properly.
 */
public class InventoryItemMetricsBoltTest extends AbstractMetricsBoltTest {

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMetricsBolt getBolt() {
        return new InventoryItemMetricsBolt();
    }

    /**
     * Make sure account event handling works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventHandling() throws Exception {
        Map<String, Float> metrics = processEvents(getEventsForTesting());
        Map<String, Float> explicitValues = ImmutableMap.<String, Float> builder()
                .put("global.CONNECTION_ACTIVITY_COUNT", 7F)
                .put("global.CONNECTION_ACTIVITY_COUNT{HASHTAG=#monitoring}", 2F)
                .put("global.CONNECTION_ACTIVITY_COUNT{HASHTAG=#projecthosting}", 2F)
                .put("global.CONNECTION_ACTIVITY_COUNT{PROVIDER_TYPE=monitoring}", 2F)
                .put("global.CONNECTION_ACTIVITY_COUNT{PROVIDER_TYPE=projecthosting}", 2F)
                .put("global.INVENTORY_ITEM_ACTIVITY_COUNT{HASHTAG=#aws,OBJECT_TYPE=compute}", 2F)
                .put("global.INVENTORY_ITEM_ACTIVITY_COUNT{HASHTAG=#cloud,OBJECT_TYPE=compute}", 2F)
                .put("global.INVENTORY_ITEM_ACTIVITY_COUNT{HASHTAG=#projecthosting,OBJECT_TYPE=project}", 2F)
                .put("global.INVENTORY_ITEM_ACTIVITY_COUNT{OBJECT_TYPE=compute}", 2F)
                .put("global.INVENTORY_ITEM_ACTIVITY_COUNT{OBJECT_TYPE=project}", 2F)
                .build();

        for (Map.Entry<String, Float> metric : new TreeMap<String, Float>(metrics).entrySet()) {
            String metricId = metric.getKey();
            Float metricValue = metric.getValue();

            // Ensure the calculated values are as expected for CONNECTION_COUNT and CONNECTION_ACTIVITY_COUNT
            try {
                if (metricId.contains(MetricName.CONNECTION_COUNT.toString()) ||
                        metricId.contains(MetricName.INVENTORY_ITEM_COUNT.toString())) {
                    Assert.assertEquals(new Float(0), metricValue);
                } else if (metricId.contains(MetricName.CONNECTION_ACTIVITY_COUNT.toString()) ||
                        metricId.contains(MetricName.INVENTORY_ITEM_ACTIVITY_COUNT.toString())) {
                    if (explicitValues.keySet().contains(metricId)) {
                        Assert.assertEquals(explicitValues.get(metricId), metricValue);
                    } else {
                        Assert.assertEquals(new Float(1), metricValue);
                    }
                }
            } catch (AssertionError ae) {
                System.err.println("Value of " + metricValue + " for " + metricId);
                throw ae;
            }
        }

        // TODO: Extend tests to validate INVENTORY_ITEM_RESOURCE_USAGE and criteria like ConnectionMetricsBoltTest
    }

    /**
     * Return a list of mocked inventory item events for testing.
     *
     * @return the list of mock events
     *
     * @throws Exception if anything goes wrong
     */
    public List<Map<String, Object>> getEventsForTesting() throws Exception {
        List<Map<String, Object>> allEvents = new ArrayList<Map<String, Object>>();

        Map<String, Object> awsInventoryItemCloudWatchActivityPayload =
                ((BasicDBObject)JSON.parse(
                        JSONUtils.readJSONFromClasspath("/com/streamreduce/storm/bolts/CloudWatchMetrics.json"))).toMap();
        Map<String, Object> awsInventoryItemAgentActivityPayload =
                ((BasicDBObject)JSON.parse(
                        JSONUtils.readJSONFromClasspath("/com/streamreduce/storm/bolts/AgentMetrics.json"))).toMap();
        Map<String, Object> imgInventoryItemActivityPayload = new HashMap<String, Object>();
        Set<Object> imgMetrics = new HashSet<Object>();

        for (int i = 0; i < 2; i++) {
            Map<String, Object> metric = new HashMap<String, Object>();

            metric.put("name", "Test Metric" + (i + 1));
            metric.put("type", "DELTA");
            metric.put("value", (i + 1));

            imgMetrics.add(metric);
        }

        imgInventoryItemActivityPayload.put("metrics", imgMetrics);

        Map<EventId, Map<String, Object>> cloudEventsWithAgentActivity =
                createConnectionOrInventoryItemEvents("InventoryItem", ConnectionTypeConstants.CLOUD_TYPE,
                                                      ProviderIdConstants.AWS_PROVIDER_ID,
                                                      awsInventoryItemAgentActivityPayload);
        Map<String, Object> agentActivityEvent = cloudEventsWithAgentActivity.get(EventId.ACTIVITY);
        Map<String, Object> agentActivityEventMetadata = (Map<String, Object>)agentActivityEvent.get("metadata");

        agentActivityEventMetadata.put("isAgentActivity", true);

        agentActivityEvent.put("metadata", agentActivityEventMetadata);
        cloudEventsWithAgentActivity.put(EventId.ACTIVITY, agentActivityEvent);

        allEvents.addAll(cloudEventsWithAgentActivity.values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem", ConnectionTypeConstants.CLOUD_TYPE,
                                                               ProviderIdConstants.AWS_PROVIDER_ID,
                                                               awsInventoryItemCloudWatchActivityPayload).values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem",
                                                               ConnectionTypeConstants.PROJECT_HOSTING_TYPE,
                                                               ProviderIdConstants.GITHUB_PROVIDER_ID, null).values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem",
                                                               ConnectionTypeConstants.PROJECT_HOSTING_TYPE,
                                                               ProviderIdConstants.JIRA_PROVIDER_ID, null).values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem", ConnectionTypeConstants.GATEWAY_TYPE,
                                                               ProviderIdConstants.CUSTOM_PROVIDER_ID,
                                                               imgInventoryItemActivityPayload).values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem", ConnectionTypeConstants.MONITORING_TYPE,
                                                               ProviderIdConstants.NAGIOS_PROVIDER_ID, null).values());
        allEvents.addAll(createConnectionOrInventoryItemEvents("InventoryItem", ConnectionTypeConstants.MONITORING_TYPE,
                                                               ProviderIdConstants.PINGDOM_PROVIDER_ID, null).values());

        return allEvents;
    }

}
