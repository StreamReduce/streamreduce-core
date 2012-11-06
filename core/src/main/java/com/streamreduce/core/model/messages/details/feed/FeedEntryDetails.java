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

package com.streamreduce.core.model.messages.details.feed;

import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

/**
 * <p>>A SobaMessageDetails implementation that describes elements of a SobaMessage that were created by parsing individual
 * feed entries generated from polling Feed connections.</p
 * <p/>
 * <p>This allows additional details about the feed entry to be included in the SobaMessage without breaking the
 * general structure of SobaMessages.</p>
 */
public class FeedEntryDetails extends AbstractMessageDetails implements SobaMessageDetails {

    private String url;
    private String description;
    private Date publishedDate;

    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.FEED_ENTRY;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Date getPublishedDate() {
        return publishedDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(url)
                .append(description)
                .append(publishedDate)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FeedEntryDetails) {
            FeedEntryDetails that = (FeedEntryDetails) o;
            return new EqualsBuilder()
                    .appendSuper(super.equals(o))
                    .append(this.url,that.url)
                    .append(this.description,that.description)
                    .append(this.publishedDate,that.publishedDate)
                    .isEquals();
        }
        return false;
    }

    public static class Builder {

        private FeedEntryDetails theObject;

        public Builder() {
            theObject = new FeedEntryDetails();
        }

        public Builder url(String url) {
            theObject.url = url;
            return this;
        }

        public Builder title(String title) {
            theObject.title = title;
            return this;
        }

        public Builder description(String description) {
            theObject.description = description;
            return this;
        }

        public Builder publishedDate(Date publishedDate) {
            theObject.publishedDate = publishedDate;
            return this;
        }

        public FeedEntryDetails build() {
            return theObject;
        }
    }
}
