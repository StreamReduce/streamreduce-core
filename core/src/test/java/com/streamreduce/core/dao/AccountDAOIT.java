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

package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.AbstractDAOTest;
import com.streamreduce.core.model.Account;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/19/12 15:21</p>
 */
public class AccountDAOIT extends AbstractDAOTest {

    @Resource(name="businessDBDatastore")
    private Datastore businessDBDatastore;

    @Autowired
    private AccountDAO accountDAO;

    private Account testAccount = testAccount = new Account.Builder()
            .name("AccountDAOTest-testAccount")
            .url("http://trunk.nodeable.com")
            .build();

    @Before
    public void setUp() throws Exception {


        accountDAO.save(testAccount);
    }

    @Test
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testFindByName() {
        Account returnedAccount = accountDAO.findByName("AccountDAOTest-testAccount");
        assertEquals(returnedAccount,testAccount);
    }

}
