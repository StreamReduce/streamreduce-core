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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import com.streamreduce.Constants;
import com.streamreduce.core.event.EventId;
import com.streamreduce.queue.CamelFacade;
import org.apache.log4j.Logger;

/*
 * The aggregation algorithm in JuggaloaderMessageGeneratorBolt groups things
 * together by "targetConnectionId". But when these show up as "insight"
 * messages in the stream, different types of metrics can appear in the same
 * table, the "friendly name" will be the first column.
 *
 * Anomaly tuples don't get buffered, they get passed in to become messages
 * right away.
 *
 * Other tuples are checked to ensure they have a targetConnectionId, if not they
 * also get passed on.
 *
 * Otherwise they are bucketted by accountId + targetConnectionId and type (whether
 * it's a status or summary). checkBuckets() decide when to flush them in which
 * case it aggregates the tuples in the bucket and passes them to be "message'ed".
 */
public class JuggaloaderMessageGeneratorBolt extends NodeableUnreliableBolt {

    private static Logger logger = Logger.getLogger(JuggaloaderMessageGeneratorBolt.class);
    private static final long serialVersionUID = 476003176010598454L;
    private int count = 0;

    private Map<String, Object> buckets = new ConcurrentHashMap<>();
    private Map<String, Long> accountState = new ConcurrentHashMap<>();

    private Map<String, Object> aggregate(Map<String, Object> metric, MessageAggregationBucket bucket) {
        ArrayList<Map<String, Object>> items = new ArrayList<>();

        float total = 0.0f;
        float diff = 0.0f;
        for (Object object : bucket) {
            Map<String, Object> item = (Map<String, Object>) object;
            Map<String, Object> row = new HashMap<>();
            row.put("metricCriteria", item.get("metricCriteria"));
            row.put("name", item.get("name")); // ie, CONNECTION_ACTIVITY
            if (item.containsKey("targetConnectionAlias")) {
                row.put("targetConnectionAlias", item.get("targetConnectionAlias")); // ie, Nodeable Cloud
            }
            if (item.containsKey("targetAlias")) {
                row.put("targetAlias", item.get("targetAlias")); // ie, i-a35034c7
            }
            //row.put("timestamp", item.get("timestamp"));
            row.put("value", item.get("value"));
            row.put("mean", item.get("mean"));
            row.put("stddev", item.get("stddev"));
            row.put("diff", item.get("diff"));
            row.put("min", item.get("min"));
            row.put("max", item.get("max"));

            total += (Float) item.get("value");
            diff += (Float) item.get("diff");

            // insertion sort by stddev value
            int idx = 0;
            while (idx < items.size() && ((Float) (items.get(idx)).get("stddev") > (Float) row.get("stddev"))) {
                idx += 1;
            }
            items.add(idx, row);
            // items.add(row);
        }
        metric.put("created", bucket.getCreated());
        metric.put("items", items);
        metric.put("diff", diff); // across all the items
        metric.put("total", total); // across all the items
        return metric;
    }

    private void checkBuckets() {
        Set<String> keys = buckets.keySet();
        for (String key : keys) {
            MessageAggregationBucket bucket = (MessageAggregationBucket) buckets.get(key);
            if (bucket.isReady()) {
                /*
                 * The first item in the bucket will be the metric used as the
                 * aggregated one. This has to be done here so no large ugly
                 * blob of redundant metadata is persisted to mongodb.
                 */
                Map<String, Object> first = (Map<String, Object>) bucket.get(0);
                produce(aggregate(first, bucket));
                buckets.remove(key);
            }
        }
    }

    private void bucketItem(String account, Map<String, Object> item) {
        if (item.get("targetConnectionId") != null) {
            String key = account + item.get("type") + item.get("targetConnectionId");
            MessageAggregationBucket bucket = (MessageAggregationBucket) buckets.get(key);
            if (bucket == null) {
                bucket = new MessageAggregationBucket(account);
                buckets.put(key, bucket);
            }
            bucket.add(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {

        try {
            this.count += 1;
            long granularity = tuple.getLongByField("granularity");
            boolean isAnomaly = tuple.getBooleanByField("anomaly");
            String account = tuple.getStringByField("metricAccount");
            String metricName = tuple.getStringByField("metricName");
            float diff = tuple.getFloatByField("diff");

            boolean acceptMinutes = false;
            long now = System.currentTimeMillis();

            Long accountBirth = (Long) accountState.get(account);
            if (accountBirth == null) {
                accountBirth = new Long(now);
                accountState.put(account, accountBirth);
            }

            if ((now - accountBirth.longValue()) < (Constants.PERIOD_MINUTE * 15)) {
                acceptMinutes = true;
            }

            if (
                    isAnomaly ||
                    !account.equals("global")
                    && ( MetricsWhitelist.whitelist(metricName, (HashMap<String, String>) tuple.getValueByField("metricCriteria")) )
                    && ( Math.abs(diff) > 0.001f )
                    && (
                        (granularity == Constants.PERIOD_MINUTE && acceptMinutes) // when starting up, we dont want to wait for hourlies
                        || granularity == Constants.PERIOD_HOUR // hourly status
                        || granularity == Constants.PERIOD_DAY // daily summary
                    )
            ) {
                Map<String, Object> metadata = (Map<String, Object>) tuple.getValueByField("metaData");
                Map<String, String> criteria = (Map<String, String>) tuple.getValueByField("metricCriteria");

                metadata.put("granularity", granularity);
                metadata.put("account", account);
                metadata.put("name", metricName);
                metadata.put("timestamp", tuple.getLongByField("metricTimestamp"));
                metadata.put("metricCriteria", criteria);
                metadata.put("value", tuple.getFloatByField("metricValue"));
                metadata.put("mean", tuple.getFloatByField("avgy"));
                metadata.put("stddev", tuple.getFloatByField("stddev"));
                metadata.put("diff", diff);
                metadata.put("min", tuple.getFloatByField("min"));
                metadata.put("max", tuple.getFloatByField("max"));

                EventId mType = EventId.NODEBELLY_STATUS;
                if (isAnomaly) {
                    mType = EventId.NODEBELLY_ANOMALY;
                }
                else if (granularity == Constants.PERIOD_DAY) {
                    mType = EventId.NODEBELLY_SUMMARY;
                }
                metadata.put("type", mType.toString());

                if (isAnomaly) {
                    produce(metadata);
                } else {
                    if (metadata.get("targetConnectionId") != null) {
                        bucketItem(account, metadata);
                    } /* causes SOBA-1962 else {
                        produce(metadata);
                    }
                    */
                }
            }

            // checked each time execute() is called, even if
            // no new tuple was bucketed
            checkBuckets();

        } catch (Exception e) {
            logger.error("Unknown exception type in JuggaloaderMessageGeneratorBolt " + e.getMessage(), e);
        }

    }

    protected void produce(Map<String, Object> map) {
        // SOBA-1663 - we used a minute-granularity above for a fake status
        // nodebelly, change it back to an hourly
        Long granularity = (Long) map.get("granularity");
        String mType = (String) map.get("type");
        if (! "NODEBELLY_ANOMALY".equals(mType) && granularity != null && granularity.longValue() == Constants.PERIOD_MINUTE ) {
            map.put("granularity", Constants.PERIOD_HOUR);
        }
        logger.info("JuggaloaderMessageGeneratorBolt produce insight: " + mType);
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
        logger.info("JuggaloaderMessageGeneratorBolt saw: " + this.count);
    }

}
