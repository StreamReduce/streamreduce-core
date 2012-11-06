package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;

import javax.validation.constraints.NotNull;

import net.sf.json.JSONObject;
import org.bson.types.ObjectId;

@SuppressWarnings("rawtypes")
@Entity(value = "eventLog", noClassnameStored = true)
public class EventLog extends ObjectWithId {

    private static final long serialVersionUID = 7334803153089210633L;
    @NotNull
    private ObjectId accountId;
    @NotNull
    private ObjectId userId;
    @NotNull
    private String eventName;
    @NotNull
    private JSONObject payload;
    @NotNull
    private Long timestamp;

    public static class Builder {
        private EventLog eventLog;

        public Builder() {
            eventLog = new EventLog();
        }

        public Builder user(User user) {
            eventLog.userId = user.getId();
            eventLog.accountId = user.getAccount().getId();
            return this;
        }

        public Builder keyValue(String eventName, JSONObject payload, Long timestamp) {
            eventLog.eventName = eventName;
            eventLog.payload = payload;
            eventLog.timestamp = timestamp;
            return this;
        }


        public EventLog build() {
            return this.eventLog;
        }

    }

    public ObjectId getAccountId() {
        return accountId;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public String getEventName() {
        return eventName;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
