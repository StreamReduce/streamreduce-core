package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.User;
import com.streamreduce.test.service.TestUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class UserServiceIT extends AbstractServiceTestCase {

    @Autowired
    UserService userService;

    @Test
    public void testCreateUser_PersistDefaultConfig() throws Exception {
        //Test that createUser persists a default config

        User user = TestUtils.createTestUser();
        user.setAccount(testAccount);
        ReflectionTestUtils.setField(user,"userConfig",null);
        ObjectId userId =  userService.createUser(user).getId();
        User retrievedUser = userService.getUserById(userId);
        Map<String,Object> config = retrievedUser.getConfig();
        assertTrue(Boolean.valueOf(config.get(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS).toString()));
        assertTrue(Boolean.valueOf(config.get(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS).toString()));
    }
}
