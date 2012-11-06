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

package com.streamreduce.core.model.messages;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.annotations.PrePersist;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ObjectWithId;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.Taggable;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.util.HashtagUtil;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("rawtypes")
@Entity(value = "messages", noClassnameStored = true)
@Indexes({
        @Index("senderId, ownerId, visibility, hashtags, modified") // regular queries
})
public final class SobaMessage extends ObjectWithId implements Taggable, Comparable {

    private static final long serialVersionUID = -839370297341737935L;

    @NotNull
    protected MessageType type; // SOBA-1050 (types changed)
    @NotNull
    protected SobaObject.Visibility visibility = SobaObject.Visibility.ACCOUNT;
    @NotNull
    protected ObjectId ownerId;
    @NotNull
    protected ObjectId senderId;
    protected String senderName; // set name for posterity and for the DTO later on.
    protected String senderConnectionName; // set name for posterity and for the DTO later on, may be null for User message
    protected ObjectId senderAccountId;
    protected ObjectId connectionId; // ie, cloudId, projectId - may be null
    protected String providerId; // jira, github, aws, etc...  - may be null
    @Embedded
    protected List<MessageComment> comments = new ArrayList<MessageComment>();
    protected String transformedMessage;
    protected Long dateGenerated;
    protected Set<String> hashtags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    @Embedded
    protected SobaMessageDetails details;

    private SobaMessage() {
    }

    @PrePersist
    public void generateCreatedDateIfMissing() {
        // if the user doesn't pass this, set it to be equal to creation date
        if (dateGenerated == null) {
            dateGenerated = created;
        }
    }

    /**
     * Adds a comment to the first position of the array. Also updates the modified date of this object
     *
     * @param comment - valid comment object
     */
    public void addComment(MessageComment comment) {
        // always add to the first position.
        comments.add(0, comment);
        modified = comment.getCreated();
    }

    public List<MessageComment> getComments() {
        return comments;
    }

    public void setHashtags(Set<String> hashtags) {
        if (hashtags != null) {
            for (String tag : hashtags) {
                addHashtag(tag);
            }
        }
    }

    @Override
    public void addHashtags(Set<String> hashtags) {
        setHashtags(hashtags);
    }

    /**
     * Adds, but also normalizes the tag and updates the modified date of this object
     *
     * @param tag - the tag
     */
    @Override
    public void addHashtag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            tag = HashtagUtil.normalizeTag(tag);
            hashtags.add(tag);
            modified = new Date().getTime();
        }
    }

    /**
     * Remoevs and updates the modified date of this object
     *
     * @param tag - the tag
     */
    @Override
    public void removeHashtag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            tag = HashtagUtil.normalizeTag(tag);
            hashtags.remove(tag);
            modified = new Date().getTime();
        }
    }


    public MessageType getType() {
        return type;
    }

    public ObjectId getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderConnectionName() {
        return senderConnectionName;
    }

    public ObjectId getSenderAccountId() {
        return senderAccountId;
    }

    public String getTransformedMessage() {
        return transformedMessage;
    }

    public Long getCreated() {
        return created;
    }

    public Long getModified() {
        return modified;
    }

    public Long getDateGenerated() {
        return dateGenerated;
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    public ObjectId getConnectionId() {
        return connectionId;
    }

    public String getProviderId() {
        return providerId;
    }

    public SobaObject.Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SobaObject.Visibility visibility) {
        this.visibility = visibility;
    }

    public ObjectId getOwnerId() {
        return ownerId;
    }

    public SobaMessageDetails getDetails() {
        return details;
    }

    @Override
    public int compareTo(Object o) {
        long result = modified.compareTo(((SobaMessage) o).modified);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SobaMessage message = (SobaMessage) o;

        if (connectionId != null ? !connectionId.equals(message.connectionId) : message.connectionId != null) {
            return false;
        }
        if (ownerId != null ? !ownerId.equals(message.ownerId) : message.ownerId != null) {
            return false;
        }
        if (providerId != null ? !providerId.equals(message.providerId) : message.providerId != null) {
            return false;
        }
        if (visibility != message.visibility) {
            return false;
        }
        if (senderConnectionName != null ? !senderConnectionName.equals(message.senderConnectionName) : message.senderConnectionName != null) {
            return false;
        }
        if (senderId != null ? !senderId.equals(message.senderId) : message.senderId != null) {
            return false;
        }
        if (transformedMessage != null ? !transformedMessage.equals(message.transformedMessage) : message.transformedMessage != null) {
            return false;
        }
        if (type != message.type) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (visibility != null ? visibility.hashCode() : 0);
        result = 31 * result + (senderId != null ? senderId.hashCode() : 0);
        result = 31 * result + (senderConnectionName != null ? senderConnectionName.hashCode() : 0);
        result = 31 * result + (connectionId != null ? connectionId.hashCode() : 0);
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (transformedMessage != null ? transformedMessage.hashCode() : 0);
        return result;
    }

    public static class Builder {

        private SobaMessage message;
        private Connection connection;

        @SuppressWarnings("unchecked")
        public Builder() {
            message = new SobaMessage();
        }

        public Builder type(MessageType type) {
            message.type = type;
            return this;
        }

        public Builder connection(Connection connection) {
            if (connection != null) {
                message.connectionId = connection.getId();
                message.providerId = connection.getProviderId();
                message.senderConnectionName = connection.getAlias();
                // we'll use this in the build method to check for visibility overrides
                this.connection = connection;
            }
            return this;
        }

        public Builder providerId(String providerId) {
            message.providerId = providerId;
            return this;
        }

        /**
         * Sets the senderId, senderName, ownverId and Visibity to the sender values
         *
         * @param sender
         * @return
         */
        public Builder sender(SobaObject sender) {
            if (sender != null) {
                message.senderId = sender.getId();
                message.senderName = sender.getAlias();
                message.senderAccountId = sender.getAccount().getId();
                message.ownerId = sender.getUser().getId();
                // defaults to sender visibility
                message.visibility = sender.getVisibility();
            }
            return this;
        }

        // you shouldn't really need this except to override sender and/or connection values
        public Builder visibility(SobaObject.Visibility visibility) {
            message.visibility = visibility;
            return this;
        }

        public Builder dateGenerated(Long dateGenerated) {
            message.dateGenerated = dateGenerated;
            return this;
        }

        public Builder hashtags(Set<String> hashTags) {
            message.setHashtags(hashTags);
            return this;
        }

        public Builder transformedMessage(String transformedMessage) {
            message.transformedMessage = transformedMessage;
            return this;
        }

        public Builder details(SobaMessageDetails details) {
            message.details = details;
            return this;
        }

        public SobaMessage build() {
            //verify required fields are non-null
            if (this.message.type == null || this.message.visibility == null || this.message.ownerId == null ||
                    this.message.senderId == null) {
                throw new IllegalStateException("SobaMessage must have a non-null type, visibility, ownerId, and senderId.");
            }
            // SOBA-1885: override and use connection for the fringe case of pubic types
            if (this.connection != null && this.connection.getVisibility().equals(SobaObject.Visibility.PUBLIC)) {
                message.visibility = this.connection.getVisibility();
            }

            return this.message;
        }
    }

    public void setTransformedMessage(String transformedMessage) {
        this.transformedMessage = transformedMessage;
    }

    public void setDetails(SobaMessageDetails details) {
        this.details = details;
    }
}
