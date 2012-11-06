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

import java.util.Map;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import com.streamreduce.core.event.EventId;
import com.streamreduce.queue.CamelFacade;
import org.apache.log4j.Logger;

/*
 * This is what remains of the JuggaloaderMessageGeneratorBolt but is responsible
 * only for the real-time generation of Anomaly messages. It does not need to do
 * any aggregation in memory. As soon as one of the time bolts detects an anomaly
 * and emits a tuple this JuggaloaderAnomalyGeneratorBolt gets it and creates
 * queues the proper info to the core server.
 */
public class JuggaloaderAnomalyGeneratorBolt extends NodeableUnreliableBolt {

    private static Logger logger = Logger.getLogger(JuggaloaderAnomalyGeneratorBolt.class);
    private int count = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {

        try {
            this.count += 1;

            boolean isAnomaly = tuple.getBooleanByField("anomaly");

            if (isAnomaly) {
                Map<String, Object> metadata = (Map<String, Object>) tuple.getValueByField("metaData");
                Map<String, String> criteria = (Map<String, String>) tuple.getValueByField("metricCriteria");

                metadata.put("granularity", tuple.getLongByField("granularity"));
                metadata.put("account", tuple.getStringByField("metricAccount"));
                metadata.put("name", tuple.getStringByField("metricName"));
                metadata.put("timestamp", tuple.getLongByField("metricTimestamp"));
                metadata.put("criteria", criteria);
                metadata.put("value", tuple.getFloatByField("metricValue"));
                metadata.put("mean", tuple.getFloatByField("avgy"));
                metadata.put("stddev", tuple.getFloatByField("stddev"));
                metadata.put("diff", tuple.getFloatByField("diff"));
                metadata.put("min", tuple.getFloatByField("min"));
                metadata.put("max", tuple.getFloatByField("max"));

                metadata.put("type", EventId.NODEBELLY_ANOMALY.toString());

                produce(metadata);
            }
        } catch (Exception e) {
            logger.error("Unknown exception type in JuggaloaderAnomalyGeneratorBolt " + e.getMessage(), e);
        }
    }

    private void produce(Map<String, Object> map) {
        CamelFacade.sendInsightMessage(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // none
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        logger.info("JuggaloaderAnomalyGeneratorBolt saw: " + this.count);
    }

}
