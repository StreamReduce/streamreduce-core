package com.streamreduce.core.dao;


import com.streamreduce.AbstractDAOTest;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.test.service.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class ConnectionDAOIT extends AbstractDAOTest {

    @Autowired
    private ConnectionDAO connectionDAO;
    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private UserDAO userDAO;

    private Account testAccount;

    @Before
    public void setUp() {
        Connection c = TestUtils.createTestFeedConnection();
        testAccount = c.getAccount();
        accountDAO.save(testAccount);
        userDAO.save(c.getUser());
        connectionDAO.save(c);
    }

    @Test
    public void testForAccount() {
        assertEquals(1, connectionDAO.forAccount(testAccount).size());
    }
}
