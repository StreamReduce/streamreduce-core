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

package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.core.service.UserService;
import com.streamreduce.rest.dto.response.AccountResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AccountResourceITCase extends AbstractInContainerTestCase {

    private UserService userservice;
    private String authToken;

    public AccountResourceITCase() {
        super();
    }


    @Override
    public void tearDown() throws Exception {
        logout(authToken);
        super.tearDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // login
        authToken = login(testUsername, testUsername);
    }

    protected String getUrl() {
        return getPublicApiUrlBase() + "/account";
    }

    @Test
    @Ignore
    public void testGetAccount() throws Exception {
        String url = getUrl();
        AccountResponseDTO account = jsonToObject(makeRequest(url, "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(AccountResponseDTO.class));

        assertNotNull(account);
        assertEquals(testUser.getAccount().getName(), account.getName());
        assertEquals(testUser.getAccount().getFuid(), account.getFuid());

    }

    @Test
    @Ignore
    public void testGetAccountUsers() throws Exception {

        String url = getUrl() + "/users";
        String response = makeRequest(url, "GET", null, authToken);
        List<UserResponseDTO> users = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                UserResponseDTO.class));

        assertNotNull(users);
        assertEquals(userservice.allUsersForAccount(testUser.getAccount()).size(), users.size());

    }

    @Test
    @Ignore
    public void testGetLoggedInUsers() throws Exception {

        String url = getUrl() + "/users/loggedIn";
        String response = makeRequest(url, "GET", null, authToken);
        List<UserResponseDTO> users = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                UserResponseDTO.class));

        assertNotNull(users);
        assertEquals(applicationManager.getSecurityService().getActiveUsers(testUser.getAccount(),TimeUnit.DAYS.toMillis(1)).size(), users.size());

    }


}
