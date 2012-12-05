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

    static final String SAMPLE_EXTERNAL_ID = "ABC-DEF-123456789";

    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private UserDAO userDAO;

    private Account testAccount;

    @Before
    public void setUp() {
        User user = TestUtils.createTestUser();
        user.setExternalId(SAMPLE_EXTERNAL_ID);
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
        assertEquals(1, users.size());
    }

    @Test
    public void testGetByExternalId() {
        assertEquals(1, userDAO.getByExternalId(SAMPLE_EXTERNAL_ID).size());
    }

    @Test
    public void testGetByExternalId_EmptyExternalId() {
        assertEquals(0, userDAO.getByExternalId(" ").size());
    }

    @Test
    public void testGetByExternalId_NullExternalId() {
        assertEquals(0, userDAO.getByExternalId(null).size());
    }
}
