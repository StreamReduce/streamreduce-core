package com.streamreduce.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.test.service.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service tests for the hashtags feature of {@link SobaObject}.
 */
public class HashTagServiceITCase extends AbstractServiceTestCase {

    @Autowired
    ConnectionService connectionService;

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    Connection connection;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        connection = TestUtils.createFeedConnectionWithSpecificOutboundDatatypes();
        connection.setUser(testUser);
        connection.setAccount(testAccount);
        connection.setHashtags(ImmutableSet.of("#RSS", "#jclouds", "#GitHub"));
        connectionService.createConnection(connection);
    }


    @Test
    public void testConnectionHashtagsLowerCased() throws Exception {
        for (String hashtag : connection.getHashtags()) {
            Assert.assertEquals(hashtag.toLowerCase(), hashtag);
        }

        connectionService.addHashtag(connection, testUser, "TeStInG");

        for (String hashtag : connection.getHashtags()) {
            Assert.assertEquals(hashtag.toLowerCase(), hashtag);
        }
    }

    @Test
    public void testRemoveTagsFromConnectionMixedCase() throws Exception {
        connectionService.removeHashtag(connection,connection,"#JclOuDs");
        Assert.assertEquals(Sets.newTreeSet(Sets.newHashSet("#rss","#github")),connection.getHashtags());
    }

    @Test
    public void testRemoveTagsFromConnectionNoLeadingPound() throws Exception {
        connectionService.createConnection(connection);
        connectionService.removeHashtag(connection,connection,"jclouds");

        Assert.assertEquals(Sets.newTreeSet(Sets.newHashSet("#rss", "#github")),connection.getHashtags());
    }



}
