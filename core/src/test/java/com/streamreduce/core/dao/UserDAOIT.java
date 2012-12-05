package com.streamreduce.core.dao;

import com.streamreduce.AbstractDAOTest;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.test.service.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UserDAOIT extends AbstractDAOTest {

    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private UserDAO userDAO;

    private Account testAccount;

    @Before
    public void setUp() {
        User user = TestUtils.createTestUser();
        User otherUser = TestUtils.createTestUser();
        otherUser.setUsername("danny@toolband.com");

        testAccount = user.getAccount();

        accountDAO.save(user.getAccount());
        accountDAO.save(otherUser.getAccount());

        userDAO.save(user);
        userDAO.save(otherUser);
    }

    @Test
    public void testForAccount() {
        List<User> users = userDAO.forAccount(testAccount);
        assertEquals(1,users.size());
    }
}
