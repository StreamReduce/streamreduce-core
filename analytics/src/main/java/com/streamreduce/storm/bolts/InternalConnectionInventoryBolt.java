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
import java.util.List;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.mongodb.BasicDBObject;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.storm.MongoClient;
import org.apache.log4j.Logger;

/**
 * Extension of {@link backtype.storm.topology.base.BaseRichBolt} that will
 * query Nodeable's datastore for the inventory items associated with the
 * {@link #execute(backtype.storm.tuple.Tuple)}.
 */
public class InternalConnectionInventoryBolt extends BaseRichBolt {

    private static final long serialVersionUID = -5069430729552819142L;
    private static Logger logger = Logger.getLogger(InternalConnectionInventoryBolt.class);
    private static final MongoClient mongoClient = new MongoClient(MongoClient.BUSINESSDB_CONFIG_ID);

    private OutputCollector outputCollector;

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Tuple tuple) {
        try {
            BasicDBObject connection = (BasicDBObject) tuple.getValue(0);
            String id = connection.getString("_id");
            String type = connection.getString("type");
            List<BasicDBObject> inventoryItems = new ArrayList<>();

            if (type.equals(ConnectionTypeConstants.FEED_TYPE)) {
                // Do nothing, just here for posterity
            } else if (type.equals(ConnectionTypeConstants.PROJECT_HOSTING_TYPE)) {
                inventoryItems = mongoClient.getProjectHostingInventoryItems(id);
            } else if (type.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
                inventoryItems = mongoClient.getCloudInventoryItems(id);
            } else {
                // TODO: We need to figure out how we want to handle this
                logger.warn(type + " is an unsupported connection type.");
            }

            for (BasicDBObject inventoryItem : inventoryItems) {
                outputCollector.emit(new Values("internal", connection, inventoryItem));
            }
        } catch (Exception e) {
            logger.error("Unknown exception type in InternalConnectionInventoryBolt " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("type", "connection", "inventoryItem"));
    }

}
