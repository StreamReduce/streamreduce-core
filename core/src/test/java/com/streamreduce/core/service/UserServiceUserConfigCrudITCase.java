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
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test that exercises end to end usage of User.userConfig for CRUD operations on
 * a User.
 */
public class UserServiceUserConfigCrudITCase extends AbstractServiceTestCase {

    @Test
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testSaveAndReadUserObjectWithNullValueInConfig() throws Exception {
        //Test that a JSONArray with a null value doesn't cause deserialization problems when going back/forth
        //from mongo
        JSONArray arr = new JSONArray();
        arr.add(JSONNull.getInstance());

        testUser.setConfigValue("watchlist",arr);
        userService.updateUser(testUser);
        userService.getUser(testUser.getUsername());


    }
}
