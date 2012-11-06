package com.streamreduce.storm.utils;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Class used to test that {@link MetricsUtils} works as expected.
 */
public class MetricsUtilsTest {

    @Test
    public void testCreateUniqueMetricName() throws Exception {
        String metricName = "TEST_METRIC_NAME";

        // Test empty criteria
        Assert.assertEquals(metricName, MetricsUtils.createUniqueMetricName(metricName, null));
        Assert.assertEquals(metricName, MetricsUtils.createUniqueMetricName(metricName, Collections.EMPTY_MAP));

        // Test non-empty criteria
        Assert.assertEquals(metricName + "{age=31,name=Jeremy}",
                            MetricsUtils.createUniqueMetricName(metricName, ImmutableMap.of(
                                    "age", "31",
                                    "name", "Jeremy"
                            )));

        // Test non-empty criteria with different order
        Assert.assertEquals(metricName + "{age=31,name=Jeremy}",
                            MetricsUtils.createUniqueMetricName(metricName, ImmutableMap.of(
                                    "name", "Jeremy",
                                    "age", "31"
                            )));
    }

}
