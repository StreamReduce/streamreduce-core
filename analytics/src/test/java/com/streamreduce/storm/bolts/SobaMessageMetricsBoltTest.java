package com.streamreduce.storm.bolts;

import com.google.common.collect.ImmutableSet;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.event.EventId;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests that {@link SobaMessageMetricsBolt} works properly.
 */
public class SobaMessageMetricsBoltTest extends AbstractMetricsBoltTest {

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMetricsBolt getBolt() {
        return new SobaMessageMetricsBolt();
    }

    /**
     * Make sure account event handling works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventHandling() throws Exception {
        List<Map<String, Object>> events = getEventsForTesting();
        Map<String, Float> metrics = processEvents(events);

        Assert.assertEquals(metrics.get("global.MESSAGE_COUNT"), new Float(events.size()));

        for (Map.Entry<String, Float> metric : metrics.entrySet()) {
            String metricId = metric.getKey();
            Float metricValue = metric.getValue();

            if (!metricId.equals("global.MESSAGE_COUNT")) {
                Assert.assertEquals(new Float(1), metricValue);
            }
        }

        // TODO: Extend tests to validate all messages are created
    }

    /**
     * Return a list of mocked message events for testing.
     *
     * @return the list of mock events
     */
    public List<Map<String, Object>> getEventsForTesting() {
        List<Map<String, Object>> allEvents = new ArrayList<Map<String, Object>>();

        Map<String, Object> connectionMessageMetadata = new HashMap<String, Object>();

        connectionMessageMetadata.put("messageConnectionId", new ObjectId().toString());
        connectionMessageMetadata.put("messageEventTargetId", new ObjectId().toString());
        connectionMessageMetadata.put("messageEventTargetType", "compute");
        connectionMessageMetadata.put("messageProviderType", ConnectionTypeConstants.CLOUD_TYPE);
        connectionMessageMetadata.put("messageProviderId", ProviderIdConstants.AWS_PROVIDER_ID);
        connectionMessageMetadata.put("messageHashtags", ImmutableSet.of("#cloud", "#aws", "#testing"));

        Map<String, Object> messageWithoutAccount = createBaseEventMock(EventId.CREATE, "SobaMessage", null);
        Map<String, Object> messageWithoutUser = createBaseEventMock(EventId.CREATE, "SobaMessage", null);
        Map<String, Object> messageForConnection = createBaseEventMock(EventId.CREATE, "SobaMessage",
                                                                       connectionMessageMetadata);

        // Remove the account
        messageWithoutAccount.put("accountId", null);

        // Remove the user
        messageWithoutUser.put("userId", null);

        allEvents.add(messageWithoutAccount);
        allEvents.add(messageWithoutUser);
        allEvents.add(messageForConnection);

        return allEvents;
    }

}
