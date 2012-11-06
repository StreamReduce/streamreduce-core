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

package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.streamreduce.core.model.Metric;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository("metricDAO")
public class MetricDAO extends ValidatingDAO<Metric> {

    private Datastore messageDatastore;

    @Autowired
    protected MetricDAO(@Qualifier(value = "messageDBDatastore") Datastore datastore) {
        super(datastore);
        this.messageDatastore = datastore;
    }

    /**
     * Returns a list of metrics.
     *
     * @param accountId the account the metrics should be queried from
     * @param metricName the metric name
     * @param criteria the metric criteria
     * @param granularity the metric granularity
     * @param startTime the time of the earliest entry to return (Should be null if it doesn't matter)
     * @param endTime the time of the latest entry to return (Should be null if it doesn't matter)
     * @param pageNum the "page" number
     * @param pageSize the number of items metrics page
     *
     * @return list of metrics
     */
    public List<DBObject> getMetrics(String accountId, String metricName, Map<String, List<String>> criteria,
                                     Long granularity, Long startTime, Long endTime, int pageNum, int pageSize) {
        DB db = messageDatastore.getDB();
        DBCollection collection = db.getCollection("Metric_" + accountId);
        BasicDBObject query = new BasicDBObject();

        query.put("metricName", metricName);
        query.put("granularity", granularity);

        for (Map.Entry<String, List<String>> criteriaEntry : criteria.entrySet()) {
            String criteriaName = criteriaEntry.getKey();
            List<String> criteriaValue = criteriaEntry.getValue();
            String queryKey = "metricCriteria." + criteriaName;

            if (criteriaValue.size() == 1) {
                String value = criteriaValue.get(0);

                if (value.equals("*")) {
                    // Criteria is a wildcard so return metrics that have the metric criteria
                    query.put(queryKey, new BasicDBObject("$exists", true));
                } else {
                    // Criteria is an explicit value
                    query.put(queryKey, value);
                }
            } else {
                // Criteria is an array/list of values
                query.put(queryKey, new BasicDBObject("$in", criteriaValue));
            }
        }

        if (startTime != null || endTime != null) {
            BasicDBObject timestampQuery = new BasicDBObject();

            if (startTime != null) {
                timestampQuery.put("$gte", startTime);
            }

            if (endTime != null) {
                timestampQuery.put("$lte", endTime);
            }

            query.put("metricTimestamp", timestampQuery);
        }

        return collection.find(query)
                         .sort(new BasicDBObject("metricTimestamp", -1))
                         .skip((pageNum - 1) * pageSize)
                         .limit(pageSize)
                         .toArray();
    }

    @Override
    public Key<Metric> save(Metric entity) {
        DBCollection collection = getCollection(entity.getAccountId());
        DBObject dbObject = entity.toDBObject();
        collection.save(dbObject);
        entity.setId((ObjectId) dbObject.get("_id"));
        return new Key<Metric>(Metric.class, entity.getId());
    }

    @Override
        public Key<Metric> save(Metric entity, WriteConcern wc) {
        DBCollection collection = getCollection(entity.getAccountId());
        DBObject dbObject = entity.toDBObject();
        collection.save(dbObject, wc);
        entity.setId((ObjectId) dbObject.get("_id"));
        return new Key<Metric>(Metric.class, entity.getId());
    }

    public Metric get(ObjectId id, String accountId) {
        DBCollection collection = getCollection(accountId);
        BasicDBObject result = (BasicDBObject) collection.findOne(new BasicDBObject("_id", id));
        return new Metric(result);
    }

    private DBCollection getCollection(String accountId) {
        DB db = getDatastore().getDB();
        return db.getCollection("Metric_" + accountId);
    }

}
