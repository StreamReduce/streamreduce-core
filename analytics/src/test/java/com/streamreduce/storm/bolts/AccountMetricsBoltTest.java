package com.streamreduce.storm.bolts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link AccountMetricsBolt} works properly.
 */
public class AccountMetricsBoltTest extends AbstractMetricsBoltTest {

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMetricsBolt getBolt() {
        return new AccountMetricsBolt();
    }

    /**
     * Make sure account event handling works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEventHandling() throws Exception {
        Map<String, Float> metrics = processEvents(getEventsForTesting());

        // Only CREATE/DELETE should be emitted
        Assert.assertEquals(2, outputCollector.getEmittedValues().size());
        Assert.assertEquals(1, metrics.size());
        Assert.assertEquals(new Float(0), metrics.get("global.ACCOUNT_COUNT"));
    }

    /**
     * Return a list of mocked account events for testing.
     *
     * @return the list of mock events
     */
    public List<Map<String, Object>> getEventsForTesting() {
        return new ArrayList<Map<String, Object>>(createCRUDEvents("Account").values());
    }

}
