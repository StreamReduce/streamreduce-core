package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.AbstractDAOTest;
import com.streamreduce.core.model.Account;

import javax.annotation.Resource;

import org.junit.Before;
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
    public void testFindByName() {
        Account returnedAccount = accountDAO.findByName("AccountDAOTest-testAccount");
        assertEquals(returnedAccount,testAccount);
    }

}
