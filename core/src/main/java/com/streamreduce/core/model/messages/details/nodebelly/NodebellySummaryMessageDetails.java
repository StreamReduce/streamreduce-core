package com.streamreduce.core.model.messages.details.nodebelly;

import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

import java.util.Map;

/**
 * Aggregate message type
 */
public class NodebellySummaryMessageDetails extends AbstractMessageDetails implements SobaMessageDetails {

    protected Map<String, Object> structure;


    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.NODEBELLY_SUMMARY;
    }

    public static class Builder {
        NodebellySummaryMessageDetails theObject;

        public Builder() {
            theObject = new NodebellySummaryMessageDetails();
        }

        public Builder title(String title) {
            theObject.title = title.trim();
            return this;
        }

        public Builder structure(Map<String, Object> structure) {
            theObject.structure = structure;
            return this;
        }

        public NodebellySummaryMessageDetails build() {
            return theObject;
        }
    }


    public Map<String, Object> getStructure() {
        return structure;
    }

    public String toPlainText() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Object> entry : structure.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            text.append(key).append(":").append(value).append(" ");
        }
        return text.toString();
    }


}
