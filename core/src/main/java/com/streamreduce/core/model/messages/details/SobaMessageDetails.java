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

import com.google.code.morphia.annotations.Embedded;
import com.streamreduce.core.model.messages.details.feed.FeedEntryDetails;
import com.streamreduce.core.model.messages.details.jira.JiraActivityDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellyMessageDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellySummaryMessageDetails;
import com.streamreduce.core.model.messages.details.pingdom.PingdomEntryDetails;
import com.streamreduce.core.model.messages.details.twitter.TwitterActivityDetails;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import static org.codehaus.jackson.annotate.JsonSubTypes.Type;

/**
 * Marker interface that denotes message details that should be embedded in a SobaMessage.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "messageDetailsType")
@JsonSubTypes({
        @Type(value = FeedEntryDetails.class, name = "FEED_ENTRY"),
        @Type(value = NodebellyMessageDetails.class, name = "NODEBELLY"),
        @Type(value = NodebellySummaryMessageDetails.class, name = "NODEBELLY_SUMMARY"),
        @Type(value = PingdomEntryDetails.class, name = "PINGDOM_ACTIVITY"),
        @Type(value = TwitterActivityDetails.class, name = "TWITTER_ACTIVITY"),
        @Type(value = JiraActivityDetails.class, name = "JIRA_ACTIVITY") })
@Embedded //For Morphia
public interface SobaMessageDetails {
    public MessageDetailsType getMessageDetailsType();
}
