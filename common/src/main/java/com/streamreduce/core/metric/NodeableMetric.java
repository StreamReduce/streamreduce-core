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

package com.streamreduce.core.metric;

import com.streamreduce.analytics.MetricName;
import com.streamreduce.util.NodeableMetricUtils;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/27/12 14:14</p>
 */
public class NodeableMetric implements Serializable {

    private ObjectId id;
    private MetricStream stream;
    private MetricName name;
    private long timestamp;
    private MetricValue value;
    private MetricGranularity granularity;
    private boolean anomaly;
    private MetricType type;

    public NodeableMetric() {}

    public NodeableMetric(Map m) {
        this.anomaly = NodeableMetricUtils.getBoolean("anomaly", m, false);
        this.granularity = MetricGranularity.fromValue(NodeableMetricUtils.getLong("granularity", m, MetricGranularity.MINUTE.getMillis()));
        this.stream = new MetricStream(
                NodeableMetricUtils.getString("metricAccount", m),
                NodeableMetricUtils.getString("metricConnection", m),
                NodeableMetricUtils.getString("metricInventoryItem", m));
        this.name = MetricName.fromValue(NodeableMetricUtils.getString("metricName", m));
        this.timestamp = NodeableMetricUtils.getLong("metricTimestamp", m);
        this.value = new MetricValue(NodeableMetricUtils.getString("metricValue", m),
                NodeableMetricUtils.getFloat("avgy", m),
                NodeableMetricUtils.getFloat("stddev", m),
                NodeableMetricUtils.getFloat("diff", m),
                NodeableMetricUtils.getFloat("min", m),
                NodeableMetricUtils.getFloat("max", m));
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public MetricStream getStream() {
        return stream;
    }

    public void setStream(MetricStream stream) {
        this.stream = stream;
    }

    public MetricName getName() {
        return name;
    }

    public void setName(MetricName name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public MetricValue getValue() {
        return value;
    }

    public void setValue(MetricValue value) {
        this.value = value;
    }

    public MetricGranularity getGranularity() {
        return granularity;
    }

    public void setGranularity(MetricGranularity granularity) {
        this.granularity = granularity;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public void setAnomaly(boolean anomaly) {
        this.anomaly = anomaly;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }
}
