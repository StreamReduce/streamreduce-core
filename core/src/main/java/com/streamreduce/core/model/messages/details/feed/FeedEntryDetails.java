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
