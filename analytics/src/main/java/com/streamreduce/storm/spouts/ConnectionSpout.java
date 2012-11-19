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

package com.streamreduce.storm.spouts;

import com.mongodb.BasicDBObject;
import com.streamreduce.storm.MongoClient;

import java.util.ArrayList;
import java.util.List;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import org.apache.log4j.Logger;

/**
 * Extension of {@link AbstractScheduledDBCollectionSpout} that will
 * emit the {@link BasicDBObject} representation for each Nodeable
 */
public class ConnectionSpout extends AbstractScheduledDBCollectionSpout {

    private static final long serialVersionUID = 2126467278030837837L;
    private static MongoClient mongoClient = new MongoClient(MongoClient.BUSINESSDB_CONFIG_ID);
    private static Logger logger = Logger.getLogger(EventSpout.class);

    /**
     * Constructor.  (Calls super constructor with a sleep duration of 120s for now.)
     */
    public ConnectionSpout() {
        super(120000);
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionSpout(int sleepDuration) {
        super(sleepDuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("connection"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDBEntry(SpoutOutputCollector collector, BasicDBObject entry) {
        collector.emit(new Values(entry));
        ack(entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BasicDBObject> getDBEntries() {
        try {
            return mongoClient.getConnections();
        } catch (Exception e) {
            logger.error("0 connections emmitted due to failure in getDBEntries", e);
            return new ArrayList<BasicDBObject>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicDBObject getDBEntry(String id) {
        try {
            return mongoClient.getConnection(id);
        } catch (Exception e) {
            logger.error("Unable to retrieve DBEntry due to unexpected failure",e);
            return null;
        }
    }
}
