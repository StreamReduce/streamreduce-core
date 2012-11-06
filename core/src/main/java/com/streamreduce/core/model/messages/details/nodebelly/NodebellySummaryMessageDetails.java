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
