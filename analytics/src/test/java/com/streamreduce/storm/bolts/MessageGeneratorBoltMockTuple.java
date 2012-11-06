package com.streamreduce.storm.bolts;

import com.streamreduce.core.metric.MetricModeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MessageGeneratorBoltMockTuple {
    private List<List<Object>> tuples = null;
    private int pointer;
    private static final int NROWS = 10;

    public MessageGeneratorBoltMockTuple() {

        Random random = new Random();

        tuples = new ArrayList<List<Object>>();
        for (int i = 0; i < NROWS; i++) {
            List<Object> row = new ArrayList<Object>();

            row.add("account12" + (i % 3)); // accountId
            row.add("metric34" + (i % 4)); // metricName
            row.add(MetricModeType.DELTA); // metricType
            row.add(System.currentTimeMillis()); // timestamp
            row.add((random.nextFloat() * 3.0)); // value
            row.add("some debug string" + i); // debugString
            row.add((float) 0.4); // dydt
            row.add((float) 2.4); // avgy
            row.add((float) 0.2); // stddev
            row.add((float) 0.1); // diff
            row.add((float) 0.002); // min
            row.add((float) 8.23); // max
            row.add(true); // anomaly

            tuples.add(row);
        }
        pointer = 0;
    }

    public void next() {
        this.pointer = (this.pointer + 1) % NROWS;
    }

    public String getString(int n) {
        return (String) (this.tuples.get(this.pointer)).get(n);
    }

    public Object getValue(int n) {
        return (this.tuples.get(this.pointer)).get(n);
    }

    public Long getLong(int n) {
        return (Long) (this.tuples.get(this.pointer)).get(n);
    }

    public Float getFloat(int n) {
        return (Float) (this.tuples.get(this.pointer)).get(n);
    }
}
