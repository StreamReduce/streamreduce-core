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
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.rest.dto.response.AccountResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;

import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class AdminAccountResourceITCase extends AbstractInContainerTestCase {

    private UserService userservice;
    private String authToken;

    public AdminAccountResourceITCase() {
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
        return getPrivateUrlBase() + "/admin/account";
    }

    @Test
    @Ignore
    public void testGetAccount() throws Exception {
        String url = getUrl() + "/" + testUser.getAccount().getId();
        AccountResponseDTO account = jsonToObject(makeRequest(url, "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(AccountResponseDTO.class));

        assertNotNull(account);
        assertEquals(testUser.getAccount().getName(), account.getName());
        assertEquals(testUser.getAccount().getFuid(), account.getFuid());

    }

    @Test
    @Ignore
    public void testDisableAccount() throws Exception {
        Account account = applicationManager.getUserService().getAccount(testAccount.getId());
        assertFalse(account.getConfigValue(Account.ConfigKey.ACCOUNT_LOCKED));

        String url = getUrl() + "/" + testUser.getAccount().getId() + "/disable/";
        makeRequest(url, "PUT", null, authToken);


        account = applicationManager.getUserService().getAccount(testAccount.getId());
        assertTrue(account.getConfigValue(Account.ConfigKey.ACCOUNT_LOCKED));

        url = getUrl() + "/" + testUser.getAccount().getId() + "/enable/";
        makeRequest(url, "PUT", null, authToken);

        account = applicationManager.getUserService().getAccount(testAccount.getId());
        assertFalse(account.getConfigValue(Account.ConfigKey.ACCOUNT_LOCKED));

    }

    @Test
    @Ignore
    public void testDeleteAccount() throws Exception {

        // we have a bootstrapped user and account
        String url = getUrl() + "/" + testUser.getAccount().getId();
        AccountResponseDTO account = jsonToObject(makeRequest(url, "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(AccountResponseDTO.class));

        assertNotNull(account);

        url = accountBaseUrl + "/users";
        String response = makeRequest(url, "GET", null, authToken);
        List<UserResponseDTO> users = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                UserResponseDTO.class));

        assertNotNull(users);

//        List<SobaMessage> messages = applicationManager.getMessageService().getAllMessages(testUser, (long) 0, (long) 0, 100, true, null, null, null,false);
//        assertNotNull(messages);

        // test the delete method
        url = adminBaseUrl + "/account/" + testUser.getAccount().getId();
        response = makeRequest(url, "DELETE", null, authToken);
        assertEquals(response, "200");

        // test that resources are cleaned up.
        List<User> allUsers = applicationManager.getUserService().allUsersForAccount(testAccount);
        assertEquals(allUsers, Collections.emptyList());

        // test that connections are removed
        List<Connection> connectionList = applicationManager.getConnectionService().getAccountConnections(testAccount);
        assertEquals(connectionList, Collections.emptyList());

        try {
            applicationManager.getUserService().getAccount(testAccount.getId());
            fail();
        } catch (AccountNotFoundException e) {
        }

    }


}
