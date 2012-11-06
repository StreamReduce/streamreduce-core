package com.streamreduce.storm.bolts;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import org.apache.log4j.Logger;

public class JuggaloaderPersistenceBolt extends NodeableUnreliableBolt {

    private static Logger logger = Logger.getLogger(JuggaloaderPersistenceBolt.class);
    private static final long serialVersionUID = 3276456838351297774L;
    private int count = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void realExecute(Tuple tuple) {
        try {
            this.count += 1;

            String account = tuple.getStringByField("metricAccount");
            String id = tuple.getStringByField("metricName");
            String mtype = tuple.getStringByField("metricType");
            long ts = tuple.getLongByField("metricTimestamp");
            long granularity = tuple.getLongByField("granularity");
            float y = tuple.getFloatByField("metricValue");
            float avgy = tuple.getFloatByField("avgy");
            float stddev = tuple.getFloatByField("stddev");
            float diff = tuple.getFloatByField("diff");
            float min = tuple.getFloatByField("min");
            float max = tuple.getFloatByField("max");

            System.out.println("vasil: type: " + mtype +
                    ", granularity: " + granularity +
                    ", count: " + this.count +
                    ", account: " + account +
                    ", id: " + id +
                    ", ts: " + ts +
                    ", y: " + y +
                    ", avgy: " + avgy +
                    ", stddev: " + stddev +
                    ", diff: " + diff +
                    ", min: " + min +
                    ", max: " + max);

            //collector.emit(new Values(this.count));
        } catch (Exception e) {
            logger.error("Unknown exception type in JuggaloaderPersistenceBolt " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("count"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        System.out.println("JuggaloaderPersistenceBolt saw: " + this.count);
    }

}
