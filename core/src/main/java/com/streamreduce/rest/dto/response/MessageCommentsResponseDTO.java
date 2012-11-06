package com.streamreduce.rest.dto.response;

import java.util.List;

public class MessageCommentsResponseDTO {

    List<MessageCommentResponseDTO> comments;

    public List<MessageCommentResponseDTO> getComments() {
        return comments;
    }

    public void setComments(List<MessageCommentResponseDTO> comments) {
        this.comments = comments;
    }

}
