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

import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.springframework.util.Assert;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

/**
 * FeedClient provides necessary methods for interacting with feeds.
 */
public class FeedClient extends ExternalIntegrationClient {

    private String feedUrl = null;

    public FeedClient(Connection connection) {
        super(connection);

        this.feedUrl = connection.getUrl();

        Assert.isTrue(connection.getProviderId().equals(ProviderIdConstants.FEED_PROVIDER_ID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        URL url = new URL(feedUrl);

        try (XmlReader xmlReader = new XmlReader(url)) {
            SyndFeed rssFeed = new SyndFeedInput().build(xmlReader);
            rssFeed.getEntries();
        } catch (FeedException e) {
            throw new RuntimeException("Unable to read syndication from " + url.toString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Encountered exception reading feed from " + url.toString(), e);
        }

    }

    /**
     * Returns the available feed entries.
     *
     * @return the feed entries
     */
    public List<Entry> getFeedEntries() {
        String rawActivityResponse;
        String username = getConnectionCredentials().getIdentity();
        String password = getConnectionCredentials().getCredential();

        try {
            rawActivityResponse = HTTPUtils.openUrl(feedUrl, "GET", null, MediaType.APPLICATION_ATOM_XML, username,
                                                    password, null, null);
        } catch (Exception e) {
            LOGGER.error("Error retrieving the feed activity using " +
                                 (username != null ? username : "anonymous") +
                                 " for: " + feedUrl, e);
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
