package com.streamreduce.core.service;

import com.google.common.collect.Lists;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.dao.EventDAO;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.model.messages.details.feed.FeedEntryDetails;
import org.apache.shiro.authc.AuthenticationException;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventoryServiceImplTest {

    private static final String SAMPLE_FEED_FILE_PATH = InventoryServiceImplTest.class.getResource(
            "/com/streamreduce/rss/sample_EC2.rss").toString();

    Connection sampleFeedConnection;
    InventoryServiceImpl inventoryService;
    MessageService mockMessageService;

    @Before
    public void setUp() throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String feb282012TimeStamp = Long.toString(sdf.parse("2012-02-28").getTime());
        User sampleUser = new User.Builder().account(new Account.Builder().name("ABC").build()).username("sampleUser").build();
        sampleFeedConnection = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url(SAMPLE_FEED_FILE_PATH)
                .alias("EC2")
                .user(sampleUser)
                .authType(AuthType.NONE)
                .build();
        Map<String, String> metadata = new HashMap<String, String>();

        metadata.put("last_activity_poll", feb282012TimeStamp);

        sampleFeedConnection.setMetadata(metadata);
        sampleFeedConnection.setId(new ObjectId());

        inventoryService = new InventoryServiceImpl();

        ConnectionProviderFactory cpf = mock(ConnectionProviderFactory.class);
        when(cpf.externalIntegrationConnectionProviderFromId(sampleFeedConnection.getProviderId()))
                .thenReturn(ConnectionProvidersForTests.RSS_PROVIDER);
        ReflectionTestUtils.setField(inventoryService, "connectionProviderFactory", cpf);

        SecurityService ssMock = Mockito.mock(SecurityService.class);
        EventDAO edMock = Mockito.mock(EventDAO.class);
        EventServiceImpl esImpl = new EventServiceImpl();

        // Return null for the current user
        Mockito.when(ssMock.getCurrentUser()).thenThrow(new AuthenticationException("A user must be logged in!"));

        // Use reflection to set the EventDAO in EventServiceImpl
        ReflectionTestUtils.setField(esImpl, "eventDAO", edMock);

        mockMessageService = mock(MessageService.class);
    }

    @Test
    public void testRefreshFeedMessagesProperOrder() throws Exception {
        final List<Long> pubDates = new ArrayList<Long>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Long pubDate = (Long) invocation.getArguments()[2];
                pubDates.add(pubDate);
                return null;
            }
        }).when(mockMessageService).sendActivityMessage(any(Event.class),
                any(Connection.class),
                anyLong(),
                Matchers.any(SobaMessageDetails.class));

        inventoryService.eventService = mock(EventService.class);

        ReflectionTestUtils.setField(inventoryService, "messageService", mockMessageService);

        inventoryService.pullInventoryItemActivity(sampleFeedConnection);

        assertEquals(2, pubDates.size());
        assertTrue(pubDates.get(1) > pubDates.get(0)); // messages should  appear in chronological order, not reverse
    }

    @Test
    public void testRefreshFeedMessagesIncludesFeedEntryDetails() throws Exception {
        final List<FeedEntryDetails> feedEntryDetailList = Lists.newArrayList();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FeedEntryDetails feedEntryDetails = (FeedEntryDetails) invocation.getArguments()[3];
                feedEntryDetailList.add(feedEntryDetails);
                return null;
            }
        }).when(mockMessageService).sendActivityMessage(any(Event.class),
                any(Connection.class),
                anyLong(),
                any(SobaMessageDetails.class));

        inventoryService.eventService = mock(EventService.class);

        ReflectionTestUtils.setField(inventoryService, "messageService", mockMessageService);

        inventoryService.pullInventoryItemActivity(sampleFeedConnection);

        assertEquals(2,feedEntryDetailList.size());

        FeedEntryDetails degradedPerformanceDetails = feedEntryDetailList.get(0);
        assertEquals("Informational message: Investigating degraded performance of EBS volumes",degradedPerformanceDetails.getTitle());
        assertEquals("http://status.aws.amazon.com/#EC2_1330560778", degradedPerformanceDetails.getUrl());

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        assertEquals(sdf.parse("Wed, 29 Feb 2012 16:12:58 PST"),degradedPerformanceDetails.getPublishedDate());
        assertTrue(degradedPerformanceDetails.getDescription().startsWith("We are investigating"));
    }

}
