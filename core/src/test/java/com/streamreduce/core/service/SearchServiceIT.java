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
