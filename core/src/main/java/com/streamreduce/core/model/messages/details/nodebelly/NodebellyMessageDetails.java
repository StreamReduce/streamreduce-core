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
