package com.streamreduce.storm.spouts;

import com.streamreduce.core.metric.MetricModeType;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class JuggaloaderDummySpout extends BaseRichSpout {
    private static final long serialVersionUID = 3998056343128244500L;
    private SpoutOutputCollector _collector;
    private Random _rand;
    private int count;
    private String accountId;
    
    private int[] f4 = {
    1,
    5,
    9,
    13,
    17,
    21,
    25,
    29,
    33,
    37,
    41,
    45,
    49,
    53,
    57,
    61,
    65,
    69,
    73,
    77,
    81,
    85,
    89,
    93,
    97,
    101,
    105,
    109,
    113,
    117,
    121,
    125,
    129,
    133,
    137,
    141,
    145,
    149,
    153,
    157,
    161,
    165,
    169,
    173,
    177,
    181,
    185,
    180, // anomaly
    193,
    197,
    201,
    205,
    209,
    213,
    217,
    221,
    225,
    229,
    233,
    237,
    241,
    245,
    249,
    253,
    257,
    261,
    265,
    269,
    273,
    277,
    281,
    285,
    289,
    293,
    297,
    301,
    305,
    309,
    313,
    317,
    321,
    325,
    329,
    333,
    337,
    341,
    345,
    349,
    353,
    357,
    361,
    365,
    369,
    373,
    377,
    381,
    385,
    389,
    393,
    397,
    401,
    405,
    409,
    413,
    417,
    421,
    425,
    429,
    433,
    437,
    441,
    445,
    449,
    453,
    457,
    461,
    465,
    469,
    473,
    477,
    481,
    485,
    489,
    493,
    497,
    501,
    505,
    509,
    513,
    517,
    521,
    525,
    529,
    533,
    537,
    541,
    545,
    549,
    553,
    557,
    561,
    565,
    569,
    573,
    577,
    581,
    585,
    589,
    593,
    597,
    601,
    605,
    609,
    613,
    617,
    621,
    625,
    629,
    633,
    637,
    641,
    645,
    649,
    653,
    657,
    661,
    665
    };
    

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        accountId = "4f8c34d3cea02afbc4aa8ce8";
        _collector = collector;
        _rand = new Random();
        count = 0;
    }

    public float valueRange(double amplitude, double bias) {
        return (float) ((_rand.nextFloat() * amplitude) + bias);
    }

//    @Override
//    public void nextTuple() {
//        if (count == 0)
//            _rand.setSeed(0);
//        if (count < 10) {
//            Utils.sleep(50);
//            // 10 event streams
//            long now = System.currentTimeMillis();
//
//            _collector.emit(new Values(accountId, "1000", MetricType.ABSOLUTE, now, valueRange(20.0, -10.0))); // -10 ... 10
//            _collector.emit(new Values(accountId, "2000", MetricType.ABSOLUTE, now, valueRange(1.0, -0.5))); // -0.5 ... 0.5
//            _collector.emit(new Values(accountId, "3000", MetricType.ABSOLUTE, now, valueRange(200.0, -100.0))); // -100 ... 100
//            _collector.emit(new Values(accountId, "4000", MetricType.ABSOLUTE, now, valueRange(10.0, 30.0))); // 30 ... 40
//            _collector.emit(new Values(accountId, "5000", MetricType.ABSOLUTE, now, valueRange(10.0, 90.0))); // 90 ... 100
//            _collector.emit(new Values(accountId, "6000", MetricType.ABSOLUTE, now, valueRange(20.0, -100.0))); // -100 ... -80
//            _collector.emit(new Values(accountId, "7000", MetricType.ABSOLUTE, now, valueRange(1000.0, -500.0))); // -500 ... 500
//            _collector.emit(new Values(accountId, "8000", MetricType.ABSOLUTE, now, valueRange(2000.0, -1000.0))); // -1000 ... 1000
//            _collector.emit(new Values(accountId, "9000", MetricType.ABSOLUTE, now, valueRange(1.0, 3.0))); // 3 ... 4
//            _collector.emit(new Values(accountId, "10000", MetricType.ABSOLUTE, now, valueRange(1.0, 0.0))); // 0 ... 1
//            /*
//            */
//            count += 1;
//        }
//    }
    
    @Override
    public void nextTuple() {
        if (count == 0)
            _rand.setSeed(0);
        if (count < f4.length) {
            Utils.sleep(50);
            _collector.emit(getNextTuple());
        }
    }
    
    public Values getNextTuple() {
        Map map = (HashMap<String, Object>) new HashMap<String, Object>();
        long now = System.currentTimeMillis();
        Values v = new Values(accountId, "f4", MetricModeType.ABSOLUTE.toString(), now, (float) f4[count], map);
        count += 1;
        if(count >= f4.length)
            count = 0;
        return v;
    }

    @Override
    public void ack(Object id) {
    }

    @Override
    public void fail(Object id) {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(
                "metricAccount",
                "metricName",
                "metricType",
                "metricTimestamp",
                "metricValue",
                "metaData"
        ));
    }

}
