package com.streamreduce.storm.bolts;

import com.streamreduce.Constants;
import com.streamreduce.storm.JuggaloaderStreamState;
import com.streamreduce.storm.MongoClient;

import java.util.HashMap;
import java.util.Map;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.log4j.Logger;

public class JuggaloaderTimeBaseBolt extends NodeableUnreliableBolt {

    private static final long serialVersionUID = 4682438377554490493L;
    private static final MongoClient mongoClient = new MongoClient(MongoClient.MESSAGEDB_CONFIG_ID);
    private static Logger logger = Logger.getLogger(JuggaloaderTimeBaseBolt.class);

    private long periodMillis = Constants.PERIOD_MINUTE;
    private Map<String, JuggaloaderStreamState> states = new HashMap<String, JuggaloaderStreamState>();

    public JuggaloaderTimeBaseBolt(int period) {
        this.setPeriodMillis((long) period);
    }

    public JuggaloaderTimeBaseBolt(long periodMillis) {
        this.setPeriodMillis(periodMillis);
    }

    public void setPeriodMillis(long periodMillis) {
        this.periodMillis = periodMillis;
    }

    public void resetStreamState(String name) {
        states.remove(name);
    }

    public void resetAllStreams() {
        states.clear();
    }

    private boolean whiteListedForAnomalies(String metricName) {
        // hack to quiet square waves that make many anomalies
        if (metricName.endsWith("CPUUtilization.average")) {
            return false;
        }
        if (metricName.endsWith(".maximum")) {
            return false;
        }
        if (metricName.endsWith(".minimum")) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {
        try {
            Values values = JuggaloaderTimeBase.process(tuple, states, periodMillis, mongoClient);
            if (values != null) {
                outputCollector.emit(values); // we are not anchoring this, do we care?
            }
        } catch (Exception e) {
            logger.error("Unknown exception type in JuggaloaderTimeBaseBolt " + e.getMessage(), e);
            //outputCollector.fail(tuple);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(
                "metricAccount", // The account the metric's value should be credited/stored in
                "metricName", // The metric's name
                "metricType", // The metric's type
                "metricTimestamp", // The metric's timestamp
                "metricValue", // The metric's value
                "metricCriteria", // key/value pairs used for querying and uniquing of the metric entry
                "metaData", // Metadata used downstream for generating nodebellys
                "granularity", // the granularity of time this sample represents (in ms)
                "avgy", // mean
                "stddev", // standard deviation
                "diff", // diff from last value (maintained for both ABSOLUTE and DELTA types)
                "min", // minimum seen so far       F
                "max", // maximum seen so far
                "anomaly" // is this sample considered an anomaly
        ));
    }

}
