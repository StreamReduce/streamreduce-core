package com.streamreduce.core.metric;

import org.bson.types.ObjectId;

import java.io.Serializable;

/**
 * Represents a specific type of metric and encapsulates all of the associated messaging.
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/29/12 09:11</p>
 */
public class MetricType implements Serializable {

    private ObjectId id;
    private String label;
    private String explanation;
    private String anomalyLabel;
    private String anomalyExtremeLabel;
    private String anomalyExplanation;
    private String unitsShort;
    private String unitsLong;
    private String unitsShortPlural;
    private String unitsLongPlural;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getAnomalyLabel() {
        return anomalyLabel;
    }

    public void setAnomalyLabel(String anomalyLabel) {
        this.anomalyLabel = anomalyLabel;
    }

    public String getAnomalyExtremeLabel() {
        return anomalyExtremeLabel;
    }

    public void setAnomalyExtremeLabel(String anomalyExtremeLabel) {
        this.anomalyExtremeLabel = anomalyExtremeLabel;
    }

    public String getAnomalyExplanation() {
        return anomalyExplanation;
    }

    public void setAnomalyExplanation(String anomalyExplanation) {
        this.anomalyExplanation = anomalyExplanation;
    }

    public String getUnitsShort() {
        return unitsShort;
    }

    public void setUnitsShort(String unitsShort) {
        this.unitsShort = unitsShort;
    }

    public String getUnitsLong() {
        return unitsLong;
    }

    public void setUnitsLong(String unitsLong) {
        this.unitsLong = unitsLong;
    }

    public String getUnitsShortPlural() {
        return unitsShortPlural;
    }

    public void setUnitsShortPlural(String unitsShortPlural) {
        this.unitsShortPlural = unitsShortPlural;
    }

    public String getUnitsLongPlural() {
        return unitsLongPlural;
    }

    public void setUnitsLongPlural(String unitsLongPlural) {
        this.unitsLongPlural = unitsLongPlural;
    }
}
