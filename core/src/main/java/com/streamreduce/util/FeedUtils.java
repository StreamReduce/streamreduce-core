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

package com.streamreduce.util;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.StringReader;
import java.util.List;


/**
 * Utility class for working with Connection objects representing feeds
 */
public class FeedUtils {

    public static Logger LOGGER = LoggerFactory.getLogger(FeedUtils.class);

    public static List<Entry> getFeedEntries(String requestUrl, String username, String password) {
        String rawActivityResponse;

        try {
            rawActivityResponse = HTTPUtils.openUrl(requestUrl, "GET", null, MediaType.APPLICATION_ATOM_XML,
                                                    username, password, null, null);
        } catch (Exception e) {
            LOGGER.error("Error retrieving the feed activity using " + (username != null ? username : "anonymous") +
                                 " for: " + requestUrl, e);
            return null;
        }

        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        org.apache.abdera.model.Document<org.apache.abdera.model.Feed> rssDoc = parser
                .parse(new StringReader(rawActivityResponse));
        org.apache.abdera.model.Feed rssFeed = rssDoc.getRoot();

        return rssFeed.getEntries();
    }

}
