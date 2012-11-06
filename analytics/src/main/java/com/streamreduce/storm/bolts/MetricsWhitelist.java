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

import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.analytics.MetricName;

import java.util.Map;

public class MetricsWhitelist {

    public static boolean whitelist(String metricName, Map<String, String> criteria) {
        // allow projecthosting stuff
        if ("projecthosting".equals(criteria.get(MetricCriteria.PROVIDER_TYPE.toString()))) {
            return !criteria.containsKey(MetricCriteria.HASHTAG.toString()); // mute the ones generated for each hashtag
        }
        if ("googleanalytics".equals(criteria.get(MetricCriteria.PROVIDER_ID.toString()))) {
            return true;
        }
        // allow IMG activity counts
        if (metricName.equals(MetricName.CONNECTION_ACTIVITY_COUNT.toString()) && "gateway".equals(criteria.get(MetricCriteria.PROVIDER_TYPE.toString())))
            return !criteria.containsKey(MetricCriteria.HASHTAG.toString()); // mute the ones generated for each hashtag
        // allow IMG metric values
        if (metricName.equals(MetricName.CONNECTION_RESOURCE_USAGE.toString()) && criteria.containsKey(MetricCriteria.RESOURCE_ID.toString()))
            return true;

        if (metricName.equals(MetricName.INVENTORY_ITEM_RESOURCE_USAGE.toString())) {
            // Pingdom events
            if ("time".equals(criteria.get(MetricCriteria.METRIC_ID.toString()))) {
                return true;
            }
            // allow it if it's a cloudwatch average
            else if ("average".equals(criteria.get(MetricCriteria.METRIC_ID.toString()))) {
                return true;
            }
            // Nagios hosts or services
            else if ("hosts".equals(criteria.get(MetricCriteria.RESOURCE_ID.toString())) ||
                    "services".equals(criteria.get(MetricCriteria.RESOURCE_ID.toString()))) {
                return true;
            }
            // Appcelerator Proof of concept. Ask @NJH if you can remove this.
            else if (criteria.get(MetricCriteria.RESOURCE_ID.toString()).contains("cloud.") ||
                    criteria.get(MetricCriteria.RESOURCE_ID.toString()).contains("ti.")) {
                return true;
            }
        }

        // reject .minimums and .maximums
        if (isMinOrMax(criteria))
            return false;
        if (metricName.equals(MetricName.CONNECTION_ACTIVITY_COUNT.toString()) && criteria.containsKey(MetricCriteria.OBJECT_ID.toString()))
            return true;
        if (metricName.equals(MetricName.INVENTORY_ITEM_COUNT.toString()) && criteria.containsKey(MetricCriteria.OBJECT_ID.toString()))
            return true;
        if (metricName.equals(MetricName.USER_COUNT.toString()))
            return true;
        if (metricName.startsWith(MetricName.TEST_STREAM.toString()))
            return true;
        if (metricName.equals(MetricName.MESSAGE_COUNT.toString()))
            return true;
        return false;
    }

    /**
     * The metrics for which to generate anomalies on used to be a superset of
     * the ones to generate status/summaries on. But this now calls whitelist()
     * above (SOBA-1622) so that units/friendly names are available. Keeping
     * this as a wrapper for now in case we change our minds again.
     *
     */
    public static boolean whiteListedForAnomalies(String metricName, Map<String, String> criteria) {
        // hack to quiet square waves that make many anomalies
        if (isMinOrMax(criteria))
            return false;
        if (criteria.containsKey(MetricCriteria.RESOURCE_ID.toString()) && "CPUUtilization".endsWith(criteria.get(MetricCriteria.RESOURCE_ID.toString()))) {
            return false;
        }
        return whitelist(metricName, criteria);
    }

    private static boolean isMinOrMax(Map<String, String> criteria) {
        if (criteria.containsKey(MetricCriteria.METRIC_ID.toString())) {
            if ("maximum".equals(criteria.get(MetricCriteria.METRIC_ID.toString())))
                return true;
            if ("minimum".equals(criteria.get(MetricCriteria.METRIC_ID.toString())))
                return true;
        }
        return false;
    }
}
