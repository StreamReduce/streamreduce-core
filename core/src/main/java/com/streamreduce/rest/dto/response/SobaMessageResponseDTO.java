package com.streamreduce.rest.dto.response;

import com.streamreduce.Constants;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.dto.ObjectIdDeserializer;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.model.dto.ObjectIdSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class SobaMessageResponseDTO extends ObjectWithIdResponseDTO {

    protected static int MAX_DEFAULT_MESSAGE_SIZE_BEFORE_READMORE = 500;

    private ObjectId messageId;
    private MessageType messageType;
    private SobaObject.Visibility visibility;
    private String senderName;
    private String senderConnectionName;
    private ObjectId senderId;
    private ObjectId senderAccountId;
    private ObjectId connectionId;
    private String providerId;
    private String payload;
    private boolean moreText;
    private Long created;
    private Long modified;
    private Long generated;
    private List<MessageCommentResponseDTO> comments;
    private Set<String> hashtags;
    private SobaMessageDetails details;

    public static SobaMessageResponseDTO fromSobaMessage(SobaMessage sobaMessage) {
        return fromSobaMessage(sobaMessage, true);
    }

    public static SobaMessageResponseDTO fromSobaMessage(SobaMessage sobaMessage, boolean fullText) {
        SobaMessageResponseDTO dto = new SobaMessageResponseDTO();
        List<MessageCommentResponseDTO> allComments = new ArrayList<MessageCommentResponseDTO>();

        for (MessageComment comment : sobaMessage.getComments()) {
            MessageCommentResponseDTO mDTO = new MessageCommentResponseDTO();
            mDTO.setComment(comment.getComment());
            mDTO.setCreated(comment.getCreated());
            mDTO.setSenderId(comment.getSenderId());
            mDTO.setSenderName(comment.getSenderName());
            allComments.add(mDTO);
        }

        dto.setHashtags(sobaMessage.getHashtags());
        dto.setComments(allComments);
        dto.setConnectionId(sobaMessage.getConnectionId());
        dto.setProviderId(sobaMessage.getProviderId());
        dto.setCreated(sobaMessage.getCreated());
        dto.setGenerated(sobaMessage.getDateGenerated());
        dto.setMessageId(sobaMessage.getId());
        dto.setMessageType(sobaMessage.getType());
        dto.setModified(sobaMessage.getModified());
        dto.setSenderId(sobaMessage.getSenderId());
        dto.setSenderName(sobaMessage.getSenderName());
        dto.setSenderConnectionName(sobaMessage.getSenderConnectionName());
        dto.setSenderAccountId(sobaMessage.getSenderAccountId());
        dto.setId(sobaMessage.getId());
        dto.setPayload(abbrMesssage(dto, sobaMessage.getTransformedMessage(), fullText));
        dto.setDetails(sobaMessage.getDetails());
        return dto;
    }

    public static List<SobaMessageResponseDTO> fromSobaMessages(List<SobaMessage> sobaMessages, boolean fullText) {
        List<SobaMessageResponseDTO> sobaMessageDTOs = new ArrayList<SobaMessageResponseDTO>();
        for (SobaMessage sobaMessage : sobaMessages) {
            SobaMessageResponseDTO dto = fromSobaMessage(sobaMessage, fullText);
            sobaMessageDTOs.add(dto);
        }
        return sobaMessageDTOs;
    }

    /**
     * start at the max char and go backwards until you find a space. end there,
     * so we don't cut words in half...
     * this does NOT support HTML, only raw text
     *
     * @param messageText -
     * @return -
     */
    private static String abbrMesssage(SobaMessageResponseDTO dto, String messageText, boolean fullText) {
        if (fullText) {
            //Not really fullText since we want to cut the messageText off from being hellishly long.
            return trimMessageWithoutCuttingWords(messageText, Constants.MAX_MESSAGE_LENGTH);
        }
        if (messageText != null && messageText.length() >= MAX_DEFAULT_MESSAGE_SIZE_BEFORE_READMORE) {
            messageText = trimMessageWithoutCuttingWords(messageText, MAX_DEFAULT_MESSAGE_SIZE_BEFORE_READMORE);
            dto.setMoreText(true);
        }
        return messageText;
    }

    private static String trimMessageWithoutCuttingWords(String messageText, int endPosition) {
        if (messageText == null) {
            return null;
        }
        if (messageText.length() < endPosition) {
            return messageText;
        }

        int stop = 0;
        for (int i = endPosition; endPosition > 1; i--) {
            if (messageText.charAt(i - 1) == ' ') {
                stop = i;
                break;
            }
        }
        messageText = messageText.substring(0, stop).trim();
        return messageText;
    }

    private SobaMessageResponseDTO() {
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<String> hashtags) {
        this.hashtags = hashtags;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderConnectionName() {
        return senderConnectionName;
    }

    public void setSenderConnectionName(String senderConnectionName) {
        this.senderConnectionName = senderConnectionName;
    }


    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getSenderAccountId() {
        return senderAccountId;
    }

    @JsonDeserialize(using = ObjectIdDeserializer.class)
    public void setSenderAccountId(ObjectId senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getModified() {
        return modified;
    }

    public void setModified(Long modified) {
        this.modified = modified;
    }

    public Long getGenerated() {
        return generated;
    }

    public void setGenerated(Long generated) {
        this.generated = generated;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public List<MessageCommentResponseDTO> getComments() {
        return comments;
    }

    public void setComments(List<MessageCommentResponseDTO> comments) {
        this.comments = comments;
    }

    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getMessageId() {
        return messageId;
    }

    @JsonDeserialize(using = ObjectIdDeserializer.class)
    public void setMessageId(ObjectId messageId) {
        this.messageId = messageId;
    }

    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getSenderId() {
        return senderId;
    }

    @JsonDeserialize(using = ObjectIdDeserializer.class)
    public void setSenderId(ObjectId senderId) {
        this.senderId = senderId;
    }

    public SobaObject.Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SobaObject.Visibility visibility) {
        this.visibility = visibility;
    }

    @JsonSerialize(using = ObjectIdSerializer.class)
    public ObjectId getConnectionId() {
        return connectionId;
    }

    @JsonDeserialize(using = ObjectIdDeserializer.class)
    public void setConnectionId(ObjectId connectionId) {
        this.connectionId = connectionId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isMoreText() {
        return moreText;
    }

    public void setMoreText(boolean moreText) {
        this.moreText = moreText;
    }

    public SobaMessageDetails getDetails() {
        return details;
    }

    public void setDetails(SobaMessageDetails details) {
        this.details = details;
    }

    /**
     * Converts this SobaMessageResponseDTO to a JSON String representation.
     * @return JSON String for this SobaMessageResponseDTO
     * @throws RuntimeException if this SobaMessageResponseDTO can't be converted to a JSON String.
     */
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts this SobaMessageResponseDTO to a JSON document encapsulated in a byte array.
     * @return a byte[] representing the JSON for this SobaMessageResponseDTO
     * @throws RuntimeException if this SobaMessageResponseDTO can't be converted to a byte[]
     */
    public byte[] toByteArray() {
        try {
            return new ObjectMapper().writeValueAsBytes(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
