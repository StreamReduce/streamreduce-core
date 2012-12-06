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
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.PrePersist;
import com.streamreduce.core.event.EventId;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple object to describe an event in Nodeable.
 */
@Entity(value = "eventStream", noClassnameStored = true)
@Index("accountId, targetId, timestamp")
public class Event {

    @Id
    private ObjectId id;
    @NotNull
    private Long timestamp;
    @NotNull
    private EventId eventId;
    private ObjectId accountId;
    private ObjectId userId;
    private ObjectId targetId;
    private Map<String, Object> metadata;

    @PrePersist
    public void defaultTimeStamp() {
        // Set the timestamp if missing
        if (timestamp == null) {
            timestamp = new Date().getTime();
        }

        // Remove metadata entries with null values
        if (metadata != null) {
            Set<String> badKeys = new HashSet<>();

            // Simple fix for SOBA-1281
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (entry.getValue() == null) {
                    badKeys.add(entry.getKey());
                }
            }

            for (String key : badKeys) {
                metadata.remove(key);
            }
        }
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public EventId getEventId() {
        return eventId;
    }

    public void setEventId(EventId eventId) {
        this.eventId = eventId;
    }

    public ObjectId getAccountId() {
        return accountId;
    }

    public void setAccountId(ObjectId accountId) {
        this.accountId = accountId;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ObjectId getTargetId() {
        return targetId;
    }

    public void setTargetId(ObjectId targetId) {
        this.targetId = targetId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        StringBuilder userText = new StringBuilder();
        StringBuilder targetText = new StringBuilder();
        StringBuilder accountText = new StringBuilder();
        Map<String, Object> eventMetadata = getMetadata();

        if (eventMetadata != null) {
            if (getUserId() != null) {
                userText.append(getUserId())
                        .append(" (")
                        .append(eventMetadata.get("sourceUsername"))
                        .append(": ")
                        .append(eventMetadata.get("sourceAlias"))
                        .append(")");
            } else {
                userText.append("null (Automated event)");
            }

            if (getTargetId() != null) {
                String targetType = (String)getMetadata().get("targetType");

                targetText.append(getTargetId())
                          .append(" (")
                          .append(targetType)
                          .append(": ")
                          .append(targetType.equals(Account.class.getSimpleName()) ?
                                          eventMetadata.get("targetName") :
                                          eventMetadata.get("targetAlias"))
                          .append(")");
            } else {
                targetText.append("null (Automated event)");
            }

            if (getAccountId() != null) {
                accountText.append(getAccountId())
                           .append(" (")
                           .append(eventMetadata.get("accountName"))
                           .append(")");
            } else {
                accountText.append("null (Automated event)");
            }

            return "[EVENT] " + getEventId() + " triggered by " + userText.toString() + " on " +
                    targetText.toString() + " in account " + accountText.toString() + " at " +
                    (getTimestamp() != null ? new Date(getTimestamp()) : new Date());
        } else {
            return super.toString();
        }
    }

    public static class Builder {
        private boolean isBuilt = false;
        private Event event;

        public Builder() {
            this.event = new Event();
            event.setMetadata(new HashMap<String, Object>());
        }

        public Builder accountId(ObjectId accountId) {
            checkIsBuilt();
            event.setAccountId(accountId);
            return this;
        }

        public Builder actorId(ObjectId actorId) {
            checkIsBuilt();
            event.setUserId(actorId);
            return this;
        }

        public Builder context(Map<String, Object> context) {
            checkIsBuilt();
            if (context != null) {
                event.setMetadata(context);
            }
            return this;
        }

        public Builder targetId(ObjectId targetId) {
            checkIsBuilt();
            event.setTargetId(targetId);
            return this;
        }

        public Builder eventId(EventId eventId) {
            checkIsBuilt();
            event.setEventId(eventId);
            return this;
        }

        private void checkIsBuilt() {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified/rebuilt after built.");
            }
        }

        public Event build() {
            checkIsBuilt();
            isBuilt = true;
            event.setTimestamp(new Date().getTime());
            return event;
        }
    }

}
