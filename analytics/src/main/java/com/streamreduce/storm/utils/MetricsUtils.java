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
