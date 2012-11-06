package com.streamreduce.core.model.messages.details;


import com.streamreduce.core.model.messages.details.feed.FeedEntryDetails;
import com.streamreduce.core.model.messages.details.jira.JiraActivityDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellyMessageDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellySummaryMessageDetails;
import com.streamreduce.core.model.messages.details.pingdom.PingdomEntryDetails;
import com.streamreduce.core.model.messages.details.twitter.TwitterActivityDetails;

/**
 * Provides a mapping to a type identifier for MessageDetails and a class that represents the structure of that type.
 */
public enum MessageDetailsType {

    FEED_ENTRY(FeedEntryDetails.class),
    JIRA_ACTIVITY(JiraActivityDetails.class),
    NODEBELLY(NodebellyMessageDetails.class), // default type
    NODEBELLY_SUMMARY(NodebellySummaryMessageDetails.class), // aggregate type
    PINGDOM_ACTIVITY(PingdomEntryDetails.class),
    TWITTER_ACTIVITY(TwitterActivityDetails.class);

    final public Class<? extends SobaMessageDetails> detailsClass;

    MessageDetailsType(Class<? extends SobaMessageDetails> clazz) {
        this.detailsClass = clazz;
    }
}
