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
