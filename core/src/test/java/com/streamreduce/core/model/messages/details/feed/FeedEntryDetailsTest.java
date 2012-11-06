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

import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class FeedEntryDetailsTest {

    @Test
    public void testFeedEntryDetails() {
        //Happy Path
        Date date = new Date();
        FeedEntryDetails feedMessageDetails = new FeedEntryDetails.Builder()
                .url("http://www.slashdot.com/rss") //make believe url
                .title("Nodebelly has just destroyed Tokyo")
                .description("for real, he just did")
                .publishedDate(date)
                .build();

        Assert.assertEquals(feedMessageDetails.getUrl(), "http://www.slashdot.com/rss");
        Assert.assertEquals(feedMessageDetails.getTitle(),"Nodebelly has just destroyed Tokyo");
        Assert.assertEquals(feedMessageDetails.getDescription(),"for real, he just did");
        Assert.assertEquals(feedMessageDetails.getPublishedDate(), date);

        SobaMessageDetails sobaMessageDetails = feedMessageDetails;
        Assert.assertEquals(sobaMessageDetails.getMessageDetailsType(),MessageDetailsType.FEED_ENTRY);
    }
}
