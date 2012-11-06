package com.streamreduce.core.model.messages.details.nodebelly;

import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

import java.util.Map;

public class NodebellyMessageDetails extends NodebellySummaryMessageDetails implements SobaMessageDetails {

    protected String details;

    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.NODEBELLY;
    }

    public static class Builder {
        NodebellyMessageDetails theObject;

        public Builder() {
            theObject = new NodebellyMessageDetails();
        }

        public Builder title(String title) {
            theObject.title = title.trim();
            return this;
        }

        public Builder details(String details) {
            theObject.details = details.trim();
            return this;
        }

        public Builder structure(Map<String, Object> structure) {
            theObject.structure = structure;
            return this;
        }

        public NodebellyMessageDetails build() {
            return theObject;
        }
    }

    public String getDetails() {
        return details;
    }

}
