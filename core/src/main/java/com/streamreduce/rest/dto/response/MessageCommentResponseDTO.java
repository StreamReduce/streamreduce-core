package com.streamreduce.rest.dto.response;

import com.streamreduce.core.model.dto.ObjectIdSerializer;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class MessageCommentResponseDTO {

    private ObjectId senderId;
    private String senderName;
    private Long created;
    private String comment;

    @JsonSerialize(using=ObjectIdSerializer.class)
    public ObjectId getSenderId() {
        return senderId;
    }

    public void setSenderId(ObjectId senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
