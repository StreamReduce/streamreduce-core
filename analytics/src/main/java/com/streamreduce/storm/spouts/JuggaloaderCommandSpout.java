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

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.metric.MetricModeType;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Extension of {@link BaseRichSpout} that embeds a Jetty server to accept
 * commands injected by HTTP. These commands are spread through the topology
 * and used for debugging and monitoring.
 *
 * Here's some examples that all do the same thing, they cause the "debug.clear" to reset
 * the stream in JuggaloaderTimeBase.
 *  curl -i http://localhost:8194/?metricType=debug.clear --data "metricType=debug.clear"
 *  curl -i http://localhost:8194/?metricType=debug.clear
 *  curl -i http://localhost:8194/ --data "metricType=debug.clear"
 *  curl -i http://localhost:8194/?metricType=debug.clear --data "metricType=debug.clearx"
 *  # all above work, but this doesn't. the qs overrides the postdata
 *  curl -i http://localhost:8194/?metricType=debug.clearx --data "metricType=debug.clear"
 *
 *  # send values, check state
 *  curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricValue=8.0"
 *  curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.state"
 *  curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.clear"
 *  curl -i "http://localhost:8194/?metricName=metricType=debug.numstates"
 *  curl -i "http://localhost:8194/?metricName=metricType=debug.clearall"
 *
 * # trigger anomaly:
 * curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.set&name=n&value=100"
 * curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.set&name=mean&value=45.6"
 * curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.set&name=stddev&value=1.6"
 * curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.state"
 * curl -i "http://localhost:8194/?metricName=TEST_STREAM.foo&metricType=debug.state&metricValue=500.0" # send a sample
 *
 * Commands supported:
 *  debug.clear - clears the state of the specified stream from the bolt
 *  debug.clearall - clears all states from the bolt
 *  debug.state - dumps the state (mean, stddev, etc) of the specified stream
 *  debug.numstates - dumps the number of states in memory for the bolt
 *  debug.set - sets name ("mean", "stddev", "min", "max", "n") for the stream to number given by value
 *
 * Other commands need to be added to Juggaloader to be sent from here.
 */
public class JuggaloaderCommandSpout extends BaseRichSpout {

    private static final Logger logger = Logger.getLogger(JuggaloaderCommandSpout.class);

    static final ResourceBundle topologyProps = ResourceBundle.getBundle("juggaloader-topology");
    private static final int JL_HTTP_COMMAND_PORT = Integer.parseInt(topologyProps.getString("juggaloader.commandspout.port"));;
    private static int seq = 0;
    private static ConcurrentLinkedQueue queue;
    private SpoutOutputCollector collector;

    /**
     * Only one instance of the Jetty HTTP server will be created per JVM
     */
    static {
        startEmbeddedHttpServer();
    }

    public JuggaloaderCommandSpout() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        collector = spoutOutputCollector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nextTuple() {
        if (queue == null || queue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                logger.error("Exception during sleep", e);
            }
        } else {
            Map<String, String> params = (Map<String, String>)queue.remove();
            Map<String, String> nullCriteria = new HashMap<String, String>();

            collector.emit(
                new Values(
                    params.containsKey("metricAccount") ? params.get("metricAccount") : "global",
                    params.containsKey("metricName") ? params.get("metricName") : MetricName.TEST_STREAM.toString(),
                    params.containsKey("metricType") ? params.get("metricType") : MetricModeType.ABSOLUTE.toString(),
                    params.containsKey("metricTimestamp") ? Long.parseLong(params.get("metricTimestamp")) : System.currentTimeMillis(),
                    params.containsKey("metricValue") ? Float.parseFloat(params.get("metricValue")) : 0.0f,
                    nullCriteria,
                    params
                )
            );
            // ack(entry);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        // added the default stream because storm complained
        outputFieldsDeclarer.declare(
            new Fields(
                "metricAccount", // The account the metric's value should be credited/stored in
                "metricName", // The metric's name
                "metricType", // The metric's type
                "metricTimestamp", // The metric's timestamp
                "metricValue", // The metric's value
                "metricCriteria", // key/value pairs used for querying and uniquing of the metric entry
                "metaData" // Metadata used downstream for generating nodebellys
            )
        );
    }

    /**
     * This is synchronized and statically called so only 1 worker
     * per node running this spout will create the Server.
     */
    public static synchronized void startEmbeddedHttpServer() {
        try {
            // Only the first thread that gets here will create the server and queue.
            if(queue != null) {
                return;
            }
            queue = new ConcurrentLinkedQueue<Map<String, String>>();

            Server server = new Server(JL_HTTP_COMMAND_PORT);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(String target, HttpServletRequest request,
                                   HttpServletResponse response, int dispatch)
                        throws IOException, ServletException {

                    Map<String, String> params = new HashMap<String, String>();

                    Enumeration names = request.getParameterNames();
                    while(names.hasMoreElements()) {
                        String key = (String) names.nextElement();
                        params.put(key, (String) request.getParameter(key));
                    }
                    queue.add(params);

                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(JuggaloaderCommandSpout.class.getName() + "." + Thread.currentThread().getId() + ", v0.1, " + seq);
                    ((Request) request).setHandled(true);
                    seq += 1;
                }
            });
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
