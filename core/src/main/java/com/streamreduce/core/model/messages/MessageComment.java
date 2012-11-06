package com.streamreduce.core.model.messages;

import com.google.code.morphia.annotations.Embedded;
import com.streamreduce.core.model.User;
import com.streamreduce.rest.dto.response.MessageCommentResponseDTO;

import java.util.Date;

import org.bson.types.ObjectId;

@Embedded
public class MessageComment {

    private ObjectId senderId; // User.getId();
    private String senderName; // alias
    private String fullName;
    private Long created;
    private String comment;

    public MessageComment() {
        created = new Date().getTime();
    }

    // just for search
    public MessageComment(String comment) {
        this.comment = comment;
    }

    public MessageComment(User user, String comment) {
        this();
        this.senderId = user.getId();
        this.senderName = user.getAlias();
        this.fullName = user.getFullname();
        this.comment = comment;
    }

    public String getSenderName() {
        return senderName;
    }

    public Long getCreated() {
        return created;
    }

    public String getComment() {
        return comment;
    }

    public ObjectId getSenderId() {
        return senderId;
    }

    public String getFullName() {
        return fullName;
    }

    public MessageCommentResponseDTO toDTO() {
        MessageCommentResponseDTO dto = new MessageCommentResponseDTO();

        dto.setComment(getComment());
        dto.setCreated(getCreated());
        dto.setSenderName(getSenderName());
        dto.setSenderId(getSenderId());

        return dto;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
