package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Id;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.collections.MapUtils;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/12/12 09:36</p>
 */
public class Metric implements Serializable {

    @Id
    private ObjectId id;
    private String accountId;
    private long ts;
    private String name;
    private String type;
    private long granularity;
    private float value;
    private float agv;
    private float stddev;
    private float diff;
    private float min;
    private float max;
    private boolean anomaly;
    private Map<String, String> criteria;

    public Metric() {}

    @SuppressWarnings("unchecked")
    public Metric(Map<String, Object> map) {
        if (map != null) {
            setId((ObjectId)MapUtils.getObject(map, "_id"));
            setAccountId(MapUtils.getString(map, "accountId"));
            setTs(MapUtils.getLong(map, "metricTimestamp"));
            setValue(MapUtils.getLong(map, "metricValue"));
            setGranularity(MapUtils.getLong(map, "metricGranularity"));
            setAgv(MapUtils.getLong(map, "metricAVGY"));
            setAgv(MapUtils.getLong(map, "metricAVGY"));
            setStddev(MapUtils.getLong(map, "metricAVGY"));
            setDiff(MapUtils.getLong(map, "metricAVGY"));
            setMin(MapUtils.getLong(map, "metricAVGY"));
            setMax(MapUtils.getLong(map, "metricAVGY"));
            setAnomaly(MapUtils.getBoolean(map, "metricIsAnomaly"));

            Map<String, String> criteria = (Map<String, String>) map.get("metricCriteria");
            setCriteria(criteria);
        }
    }

    public ObjectId getId() {
        return id;
    }

    public Metric setId(ObjectId id) {
        this.id = id;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public Metric setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public long getTs() {
        return ts;
    }

    public Metric setTs(long ts) {
        this.ts = ts;
        return this;
    }

    public String getName() {
        return name;
    }

    public Metric setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public Metric setType(String type) {
        this.type = type;
        return this;
    }

    public long getGranularity() {
        return granularity;
    }

    public Metric setGranularity(long granularity) {
        this.granularity = granularity;
        return this;
    }

    public float getValue() {
        return value;
    }

    public Metric setValue(float value) {
        this.value = value;
        return this;
    }

    public float getAgv() {
        return agv;
    }

    public Metric setAgv(float agv) {
        this.agv = agv;
        return this;
    }

    public float getStddev() {
        return stddev;
    }

    public Metric setStddev(float stddev) {
        this.stddev = stddev;
        return this;
    }

    public float getDiff() {
        return diff;
    }

    public Metric setDiff(float diff) {
        this.diff = diff;
        return this;
    }

    public float getMin() {
        return min;
    }

    public Metric setMin(float min) {
        this.min = min;
        return this;
    }

    public float getMax() {
        return max;
    }

    public Metric setMax(float max) {
        this.max = max;
        return this;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public Metric setAnomaly(boolean anomaly) {
        this.anomaly = anomaly;
        return this;
    }

    public Map<String, String> getCriteria() {
        if (criteria == null) {
            criteria = new HashMap<String, String>();
        }
        return criteria;
    }

    public Metric setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
        return this;
    }

    public Metric addCriteria(String name, String value) {
        getCriteria().put(name, value);
        return this;
    }

    public DBObject toDBObject() {
        BasicDBObject dbObject = new BasicDBObject()
                .append("metricName", getName())
                .append("metricType", getType())
                .append("metricTimestamp", getTs())
                .append("metricValue", getValue())
                .append("metricGranularity", getGranularity())
                .append("metricAVGY", getAgv())
                .append("metricSTDDEV", getStddev())
                .append("metricDIFF", getDiff())
                .append("metricMIN", getMin())
                .append("metricMAX", getMax())
                .append("metricIsAnomaly", isAnomaly())
                .append("metricCriteria", new BasicDBObject(getCriteria()));
        return dbObject;
    }
}
