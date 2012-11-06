package com.streamreduce.core.transformer;

import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.util.Pair;

public class MessageTransformerResult extends Pair<String,SobaMessageDetails> {

    public MessageTransformerResult(String transformedMessage, SobaMessageDetails messageDetails) {
        super(transformedMessage,messageDetails);
    }

    public String getTransformedMessage() {
        return super.first;
    }

    public SobaMessageDetails getMessageDetails() {
        return super.second;
    }

}
