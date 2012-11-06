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
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.mongodb.BasicDBObject;
import com.streamreduce.storm.MongoClient;
import org.apache.log4j.Logger;

/**
 * Extension of {@link backtype.storm.topology.base.BaseRichBolt} that will take output output from the Juggaloader and
 * writes to the appropriate datastore.
 */
public class PersistMetricsBolt extends NodeableUnreliableBolt {

    private static Logger logger = Logger.getLogger(PersistMetricsBolt.class);
    private static MongoClient mongoClient = new MongoClient(MongoClient.MESSAGEDB_CONFIG_ID);

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {
        String metricAccount = tuple.getStringByField("metricAccount");
        String metricName = tuple.getStringByField("metricName");
        String metricType = tuple.getStringByField("metricType");
        Long metricTimestamp = tuple.getLongByField("metricTimestamp");
        Float metricValue = tuple.getFloatByField("metricValue");
        Map<String, String> metricCriteria = (Map<String, String>)tuple.getValueByField("metricCriteria");
        Long metricGranularity = tuple.getLongByField("granularity");
        Float metricAVGY = tuple.getFloatByField("avgy");
        Float metricSTDDEV = tuple.getFloatByField("stddev");
        Float metricDIFF = tuple.getFloatByField("diff");
        Float metricMIN = tuple.getFloatByField("min");
        Float metricMax = tuple.getFloatByField("max");
        Boolean metricIsAnomaly = tuple.getBooleanByField("anomaly");
        Map<String, BasicDBObject> result = mongoClient.writeMetric(metricAccount, metricName, metricType,
                                                                    metricTimestamp, metricValue, metricGranularity,
                                                                    metricCriteria, metricAVGY,
                                                                    metricSTDDEV, metricDIFF, metricMIN, metricMax,
                                                                    metricIsAnomaly);

        Map.Entry<String, BasicDBObject> resultEntry = result.entrySet().iterator().next();

        outputCollector.emit(new Values(resultEntry.getValue().get("_id"), resultEntry.getKey()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("metricsRecordId", "metricsCollectionName"));
    }

}
