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

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.TupleImpl;
import backtype.storm.tuple.Values;
import com.streamreduce.Constants;
import com.streamreduce.core.metric.MetricModeType;
import com.streamreduce.storm.JuggaloaderStreamState;
import com.streamreduce.storm.MongoClient;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class JuggaloaderTimeBase {

    private static Logger logger = Logger.getLogger(JuggaloaderTimeBase.class);

    static final ResourceBundle topologyProps = ResourceBundle.getBundle("juggaloader-topology");
    static final int W = Integer.parseInt(topologyProps.getString("juggaloader.window"));
    static final float alpha = Float.parseFloat(topologyProps.getString("juggaloader.alpha"));
    static final float beta = Float.parseFloat(topologyProps.getString("juggaloader.beta"));
    static final float A = Float.parseFloat(topologyProps.getString("juggaloader.a"));
    static final int SNOOZE = Integer.parseInt(topologyProps.getString("juggaloader.snooze"));
    static final float STDDEVMIN = Float.parseFloat(topologyProps.getString("juggaloader.stddevmin"));

    public static Values process(Tuple tuple, Map<String, JuggaloaderStreamState> states, long periodMillis, MongoClient mongoClient) {

        try {

            float diff, avgy, sValue, stddev;

            boolean anomaly = false; // whether this sample is detected to be an anomaly
            String accountId = tuple.getStringByField("metricAccount");
            String metricName = tuple.getStringByField("metricName");
            String mtype = tuple.getStringByField("metricType");
            long ts = tuple.getLongByField("metricTimestamp");
            float y = tuple.getFloatByField("metricValue");
            Map<String, Object> metaData = (Map<String, Object>) tuple.getValueByField("metaData");
            Map<String, String> metricCriteria = (Map<String, String>)tuple.getValueByField("metricCriteria");
            String key = accountId + metricName + metricCriteria.toString();

            // for now, filter out things we dont display, no sense storing them
            if (! MetricsWhitelist.whitelist(metricName, (HashMap<String, String>) metricCriteria)) {
                return null;
            }

            // TODO in storm 0.8.0 the backtype.storm.tuple.Tuple became an interface that doesn't
            // support .get() .containsKey() etc.. so had to cast to a TupleImpl to test for the
            // "anomaly" field's existence below

            // filter out incoming anomalies from upstream time-bolts
            if(((TupleImpl)tuple).containsKey("anomaly") && tuple.getBooleanByField("anomaly")) {
                return null;
            }

            // a metricType "clear" will remove all state for the stream
            // specified by metricName (for the metricAccount)
            if(mtype.equals("debug.clear")) {
                states.remove(key);
            }
            if(mtype.equals("debug.clearall")) {
                states.clear();
            }
            if(mtype.equals("debug.numstates")) {
                logger.error("JuggaloaderTimeBase(" + periodMillis + "), numstates: " + states.size());
            }

            JuggaloaderStreamState state = states.get(key);

            if(mtype.equals("debug.state")) {
                logger.error("JuggaloaderTimeBase(" + periodMillis + "), states: " + state);
            }


            // initialize the stream state variables if they aren't already there
//System.out.println("vasil: state null: " + (state == null));
            if (state == null) {
                if (false && mongoClient != null) { // TODO
                    List<Map<String, Object>> snapShots = mongoClient.getLastTwoTuples(accountId, metricName, periodMillis);
//System.out.println("vasil: snapShots null: " + (snapShots == null));
                    if (snapShots != null && snapShots.size() == 2) {
//System.out.println("vasil: snapShots 2");
                        //state = new JuggaloaderStreamState(snapShots.get(0), snapShots.get(1));
                        states.put(key, state);
                        if (tuple.getLongByField("metricTimestamp") <= Long.parseLong((String)snapShots.get(0).get("metricTimestamp"))) {
                            return null;
                        }
                    }
                    else {
//System.out.println("vasil: snapShots length not 2");
                    }
                }
                if (state == null) {
//System.out.println("vasil: state clean init");
                    state = new JuggaloaderStreamState(y, ts);
                    states.put(key, state);
                }
            }

            if(mtype.equals("debug.set")) {
                String name = (String) metaData.get("name");
                float value = Float.parseFloat((String)metaData.get("value"));
                logger.error("JuggaloaderTimeBase(" + periodMillis + "), setting: " + name + " = " + value);
                if("mean".equals(name)) {
                    state.yAvgLast = value;
                }
                if("stddev".equals(name)) {
                    state.yStdDevLast = value;
                }
                if("min".equals(name)) {
                    state.min = value;
                }
                if("max".equals(name)) {
                    state.max= value;
                }
                if("n".equals(name)) {
                    state.n = (int)value;
                }
            }

            // If this sample is a diff, fix y to be the
            // absolute value by applying the diff to ylast.
            if (mtype.equals(MetricModeType.DELTA.toString())) {
                y = state.ylast + y;
            }

            diff = y - state.ylast;

            // calculate decaying windowed average
            avgy = (
                (JuggaloaderTimeBase.alpha * state.yAvgLast)
                + (
                    (
                        (JuggaloaderTimeBase.beta * y) - (JuggaloaderTimeBase.alpha * state.yAvgLast)
                    )
                    / Math.min(JuggaloaderTimeBase.W, state.n)
                )
            );

            // calculate decaying windowed standard deviation
            // note state.yStdDevLast is actually the last value of sValue
            sValue = (
                (JuggaloaderTimeBase.alpha * state.yStdDevLast)
                + (JuggaloaderTimeBase.beta * (y - state.yAvgLast) * (y - avgy))
            );
            stddev = (float) Math.sqrt(Math.abs(sValue / Math.min(state.n, JuggaloaderTimeBase.W)));

            // Check if this sample is considered an anomaly
            // Make sure this stream has seen W samples already
            // And don't report anything for a few samples after a previous anomaly
            anomaly = false;
            if (state.n > JuggaloaderTimeBase.W &&
                    periodMillis > 0 &&
                    MetricsWhitelist.whiteListedForAnomalies(metricName, metricCriteria) &&
                    state.anomalyReset == 0 &&
                    stddev > JuggaloaderTimeBase.STDDEVMIN &&
                    Math.abs(JuggaloaderTimeBase.A * stddev) < Math.abs(y - avgy)) {

                anomaly = true;
                state.anomalyReset = (byte)JuggaloaderTimeBase.SNOOZE; // don't report more anomalies for next 3 samples
            }

            state.yAvgLast = avgy;
            state.yStdDevLast = sValue;
            state.anomalyReset = (byte) Math.max(0, state.anomalyReset - 1); // decrement the snooze timer on the anomaly
            state.n = state.n + 1; // update running tally of the # of samples

            // update the stream state variables
            state.ylast = y;
            state.tslast = ts;
            state.min = Math.min(y, state.min);
            state.max = Math.max(y, state.max);

            // Only emit a value if enough time has elapsed since the
            // last emitted sample based on this bolt's period.
            Values retValue = null;
            if (anomaly || ((ts - state.tsLastEmitted) >= periodMillis)) {
                if(state.tsLastEmitted > 0 || periodMillis < Constants.PERIOD_HOUR) {
                    retValue = new Values(
                            accountId,
                            metricName,
                            MetricModeType.ABSOLUTE.toString(),
                            ts,
                            y,
                            metricCriteria,
                            metaData,
                            periodMillis,
                            avgy,
                            stddev,
                            diff,
                            state.min,
                            state.max,
                            anomaly
                        );
                }
                if (!anomaly) {
                    state.tsLastEmitted = ts;
                }
            }
            return retValue;
        } catch (Exception e) {
            logger.error("Unknown exception type in JuggaloaderTimeBase " + e.getMessage(), e);
        }
        return null;
    }
}
