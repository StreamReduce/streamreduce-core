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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.apache.log4j.Logger;

/**
 * Unreliable Storm bolt that will automatically ack.
 */
public abstract class NodeableUnreliableBolt extends BaseRichBolt {

    private static final long serialVersionUID = -1905452255590790074L;
    private static final Logger LOGGER = Logger.getLogger(NodeableUnreliableBolt.class);

    protected Map stormConfiguration;
    protected OutputCollector outputCollector;
    protected TopologyContext topologyContext;

    @Override
    public void prepare(Map stormConfiguration, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.stormConfiguration = stormConfiguration;
        this.topologyContext = topologyContext;
        this.outputCollector = outputCollector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Tuple tuple) {
        outputCollector.ack(tuple);
        try {
            realExecute(tuple);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * The real executor/handler for the tuple.
     *
     * @param tuple the tuple to execute/handler
     */
    public abstract void realExecute(Tuple tuple);

}
