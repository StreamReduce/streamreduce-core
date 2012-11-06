package com.streamreduce.core.metric;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/27/12 14:17</p>
 */
public class MetricValue {

    private MetricModeType mode;
    private MetricValueType type;
    private String value;
    private float stddev;
    private float mean;
    private float diff;
    private float min;
    private float max;

    public MetricValue(String metricValue, Float avgy, Float stddev, Float diff, Float min, Float max) {
        this.value = metricValue;
        this.mean = avgy;
        this.stddev = stddev;
        this.diff = diff;
        this.min = min;
        this.max = max;

        if (Character.isDigit(value.charAt(0))) {
            this.type = MetricValueType.FLOAT;
        }
        else {
            this.type = MetricValueType.ENUM;
        }
    }

    public MetricModeType getMode() {
        return mode;
    }

    public void setMode(MetricModeType mode) {
        this.mode = mode;
    }

    public MetricValueType getType() {
        return type;
    }

    public void setType(MetricValueType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public float getStddev() {
        return stddev;
    }

    public void setStddev(float stddev) {
        this.stddev = stddev;
    }

    public float getMean() {
        return mean;
    }

    public void setMean(float mean) {
        this.mean = mean;
    }

    public float getDiff() {
        return diff;
    }

    public void setDiff(float diff) {
        this.diff = diff;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }
}
