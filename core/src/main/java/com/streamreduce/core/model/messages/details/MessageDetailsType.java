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
