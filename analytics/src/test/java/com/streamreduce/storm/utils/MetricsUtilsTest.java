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
