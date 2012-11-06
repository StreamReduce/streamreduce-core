package com.streamreduce.service;


import com.google.common.collect.ImmutableList;
import com.streamreduce.core.dao.SobaMessageDAO;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.MessageServiceImpl;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageServiceImplTest {

    @Test
    public void testGetAllMessagesWithHashTagsMissingPounds() {
        //Tests that MessageService.getAllMessages properly adds # in front of any hashtag values we
        //wish to search for before hitting the data layers.

        //Set up mock for SobaMessageDAO and inject into MessageServiceImpl.
        SobaMessageDAO sobaMessageDAO = mock(SobaMessageDAO.class);
        when(sobaMessageDAO.getMessagesFromInbox(any(User.class),anyLong(),anyLong(),anyInt(),
                anyBoolean(), anyString(), anyListOf(String.class),anyString(),anyBoolean()))
                .thenReturn(null);

        MessageServiceImpl messageService = new MessageServiceImpl();
        ReflectionTestUtils.setField(messageService,"sobaMessageDAO",sobaMessageDAO);

        //Now verify that MessageServiceImpl normalizes the hashtags passing them into the DAO.
        List<String> passedInHashtags = ImmutableList.of("foo","#bar","baz");
        List<String> expectedHashTags = ImmutableList.of("#foo","#bar","#baz");
        messageService.getAllMessages(null,null,null,5,true,null, passedInHashtags,null,false);
        verify(sobaMessageDAO).getMessagesFromInbox(null, null, null, 5, true, null, expectedHashTags, null,false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendNodebellyMessage_NullEvent() {
        MessageServiceImpl messageService = new MessageServiceImpl();
        messageService.sendNodebellyInsightMessage(null,System.currentTimeMillis(),new HashSet<String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendNodebellyMessage_NullDate() {
        MessageServiceImpl messageService = new MessageServiceImpl();
        messageService.sendNodebellyInsightMessage(mock(Event.class),null,new HashSet<String>());
    }
}
