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
import com.streamreduce.core.model.User;
import com.streamreduce.test.service.TestUtils;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class UserServiceIT extends AbstractServiceTestCase {

    @Autowired
    UserService userService;

    @Test
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
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
