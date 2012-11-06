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
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceGetSuperUserITCase extends AbstractServiceTestCase {

    @Autowired
    UserService userService;

    @Test
    public void testGetSuperUserReturnsNewUserInstance() {
        //Tests that UserServer.getSuperUser() returns different references to equal SuperUser objects.
        User superUserA = userService.getSuperUser();
        User superUserB = userService.getSuperUser();

        Assert.assertEquals(superUserA,superUserB);
        Assert.assertNotSame(superUserA,superUserB);
    }
}
