package com.streamreduce.service;


import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.SearchService;
import com.streamreduce.core.service.SearchServiceImpl;
import com.streamreduce.util.JSONUtils;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchServiceImplTest {

    /**
     * Test that exercises proper serialization of an elasticSearch for searching messages to a
     * List&lt;SobaMessage&gt;
     * @throws Exception
     */
    @Test
    public void testSearchMessages() throws Exception {
        String json = JSONUtils.readJSONFromClasspath("/com/streamreduce/search/elastic_search_raw_payload.json");
        JSONObject elasticSearchPayload = JSONObject.fromObject(json);

        //Set these fields through reflection (mock prevents setters from working) to dummy values.
        SearchServiceImpl searchService = mock(SearchServiceImpl.class);
        ReflectionTestUtils.setField(searchService,"elasticSearchHost","doesNotMatter");
        ReflectionTestUtils.setField(searchService,"elasticSearchPort",666);
        ReflectionTestUtils.setField(searchService,"messageDatabaseName","nodeablemsgdb");
        ReflectionTestUtils.setField(searchService,"enabled",true);


        //Make sure calls to searchMessages uses the real implementation
        when(searchService.searchMessages(any(Account.class), anyString(), anyMap(), any(JSONObject.class))).thenCallRealMethod();
        //Make call to makeRequest so that it returns the elasticSearchPayload
        when(searchService.makeRequest(anyString(),any(JSONObject.class),anyMap(),anyString())).thenReturn(elasticSearchPayload);


        Account a = new Account.Builder().name("testAccount").build();
        a.setId(new ObjectId());

        List<SobaMessage> sobaMessages = searchService.searchMessages(a,"resource", null,null);

        //Expected number of hits in json:
        int expectedSize = elasticSearchPayload.getJSONObject("hits").getJSONArray("hits").size();
        Assert.assertEquals(expectedSize,sobaMessages.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRiverForNullAccount() {
        SearchService searchService = new SearchServiceImpl();
        ReflectionTestUtils.setField(searchService,"enabled",true);
        searchService.createRiverForAccount(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRiverForAccountWithoutId() {
        Account acctWithoutId = new Account.Builder()
                .name("foo")
                .build();
        acctWithoutId.setId(null);

        SearchService searchService = new SearchServiceImpl();
        ReflectionTestUtils.setField(searchService,"enabled",true);
        searchService.createRiverForAccount(acctWithoutId);
    }
}
