package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.streamreduce.AbstractDAOTest;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.metric.MetricModeType;
import com.streamreduce.core.model.Metric;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/12/12 11:06</p>
 */
public class MetricDAOIT extends AbstractDAOTest {

    @Resource(name = "messageDBDatastore")
    private Datastore messageDBDatastore;

    public static final String ACCOUNT_ID = "MetricDAOITtestSave";
    public ObjectId objectId;

    @Before
    public void setUp() throws Exception {
        MetricDAO metricDao = new MetricDAO(messageDBDatastore);
        Key<Metric> key = metricDao.save(new Metric()
                .setAccountId(ACCOUNT_ID)
                .setName(MetricName.INVENTORY_ITEM_RESOURCE_USAGE.toString())
                .setType(MetricModeType.ABSOLUTE.toString())
                .setTs(System.currentTimeMillis())
                .setValue(48.12f)
                .setGranularity(6000)
                .setAgv(30.00f)
                .setStddev(20.0f)
                .setDiff(18.12f)
                .setMin(12f)
                .setMax(78f)
                .setAnomaly(false)
                .addCriteria("OBJECT_ID", "504e4807fa5a8e3ab7791ef5")
                .addCriteria("RESOURCE_ID", "DiskReadOps")
                .addCriteria("METRIC_ID", "average"));
        objectId = (ObjectId) key.getId();
    }

    @Test
    public void testGet() throws Exception {
        Assert.assertNotNull(objectId);
        MetricDAO metricDao = new MetricDAO(messageDBDatastore);
        Metric metric = metricDao.get(objectId, ACCOUNT_ID);
        Assert.assertNotNull(metric);
        Assert.assertEquals(metric.getId(), objectId);
    }

}
