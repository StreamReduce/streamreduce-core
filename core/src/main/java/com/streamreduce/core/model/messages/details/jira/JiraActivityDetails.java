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
