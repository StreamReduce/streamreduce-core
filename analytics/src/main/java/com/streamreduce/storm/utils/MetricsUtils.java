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

import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for interacting with metrics/analytics.
 */
public class MetricsUtils {

    /**
     * Returns a string representing the metric's full "name".
     *
     * @param metricName the name of the metric
     * @param metricCriteria the criteria for the metric
     *
     * @return the generated metric name
     */
    public static String createUniqueMetricName(String metricName, Map<?, String> metricCriteria) {
        if (metricCriteria != null && metricCriteria.size() > 0) {
            Map<?, String> sortedCriteria = new TreeMap(metricCriteria);

            metricName += "{";

            for (Map.Entry<?, String> criteriaEntry : sortedCriteria.entrySet()) {
                metricName += (criteriaEntry.getKey() + "=" + criteriaEntry.getValue() + ",");
            }
            metricName = metricName.substring(0, metricName.length() - 1);
            metricName += "}";
        }

        return metricName;
    }

}
