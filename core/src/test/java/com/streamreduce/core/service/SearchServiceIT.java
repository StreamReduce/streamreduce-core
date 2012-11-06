package com.streamreduce.core.service;


import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.dao.AccountDAO;
import com.streamreduce.core.model.Account;

import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchServiceIT extends AbstractServiceTestCase {

    @Autowired
    AccountDAO accountDAO;
    @Autowired
    SearchService searchService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    @Ignore("Ignored until embedded ElasticSearch is setup")
    public void createRiverForAccount() throws Exception{
        Account account = new Account.Builder()
                .name("foo")
                .build();
        accountDAO.save(account);

        URL accountTypeUrl = searchService.createRiverForAccount(account);
        Assert.assertNotNull(accountTypeUrl);
    }
}
