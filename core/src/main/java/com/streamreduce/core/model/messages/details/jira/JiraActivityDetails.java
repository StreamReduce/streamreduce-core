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

package com.streamreduce.core.model.messages.details.jira;

import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;

public class JiraActivityDetails extends AbstractMessageDetails implements SobaMessageDetails {

    String html;

    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.JIRA_ACTIVITY;
    }

    public String getHtml() {
        return html;
    }

    public static class Builder {
        JiraActivityDetails theObject;

        public Builder() {
            theObject = new JiraActivityDetails();
        }

        public Builder html(String html) {
            theObject.html = html.trim();
            return this;
        }

        public JiraActivityDetails build() {
            return theObject;
        }
    }
}
