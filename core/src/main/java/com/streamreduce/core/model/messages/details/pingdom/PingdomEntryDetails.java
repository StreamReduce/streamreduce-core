package com.streamreduce.core.model.messages.details.pingdom;

import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/27/12 09:54</p>
 */
public class PingdomEntryDetails extends AbstractMessageDetails implements SobaMessageDetails {

    /** Timestamp of when the check was created in Pingdom. */
    private int checkCreated;
    private int lastTestTime;
    private int lastErrorTime;
    private int lastResponseTime;
    private int resolution;
    private String status;

    public int getCheckCreated() {
        return checkCreated;
    }

    public int getLastTestTime() {
        return lastTestTime;
    }

    public int getLastErrorTime() {
        return lastErrorTime;
    }

    public int getLastResponseTime() {
        return lastResponseTime;
    }

    public int getResolution() {
        return resolution;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.PINGDOM_ACTIVITY;
    }

    public static class Builder {

        private PingdomEntryDetails theObject;

        public Builder() {
            theObject = new PingdomEntryDetails();
        }

        public Builder checkCreated(int checkCreated) {
            theObject.checkCreated = checkCreated;
            return this;
        }

        public Builder lastTestTime(int lastTestTime) {
            theObject.lastTestTime = lastTestTime;
            return this;
        }

        public Builder lastErrorTime(int lastErrorTime) {
            theObject.lastErrorTime = lastErrorTime;
            return this;
        }

        public Builder lastResponseTime(int lastResponseTime) {
            theObject.lastResponseTime = lastResponseTime;
            return this;
        }

        public Builder resolution(int resolution) {
            theObject.resolution = resolution;
            return this;
        }

        public Builder status(String status) {
            theObject.status = status;
            return this;
        }

        public PingdomEntryDetails build() {
            return theObject;
        }
    }
}
