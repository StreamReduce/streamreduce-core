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
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.dto.response.AccountResponseDTO;
import com.streamreduce.rest.dto.response.RoleResponseDTO;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;
import com.streamreduce.security.Roles;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UserResourceITCase extends AbstractInContainerTestCase {

    private String authToken;
    private UserService userService;
    private Account testAccount2;
    private User testUser2;
    private User testUser3;

    public UserResourceITCase() {
        super();
    }

    protected String getUrl() {
        return getPublicApiUrlBase() + "/user";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // login
        authToken = login(testUsername, testUsername);
    }

    @Override
    public void tearDown() throws Exception {
        if (testUser3 != null) {
            userService.deleteUser(testUser3);
        }

        if (testUser2 != null) {
            userService.deleteUser(testUser2);
        }

        if (testAccount2 != null) {
            userService.deleteAccount(testAccount2.getId());
        }

        super.tearDown();
    }

    @Test
    @Ignore
    public void testUpdateUser() throws Exception {

        String newName = UUID.randomUUID().toString();
        String newAlias = UUID.randomUUID().toString();
        JSONObject json = new JSONObject();

        json.put("fullname", newName);
        json.put("alias", newAlias);

        makeRequest(getUrl() + "/profile", "PUT", json, authToken);

        UserResponseDTO userDTO = jsonToObject(makeRequest(getUrl(), "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertEquals(newName, userDTO.getFullname());
        assertEquals(newAlias, userDTO.getAlias());
    }

    @Test
    @Ignore
    public void testUpdateAccount() throws Exception {

        String newName = UUID.randomUUID().toString();
        JSONObject json = new JSONObject();

        json.put("name", newName);

        makeRequest(getUrl() + "/account/profile", "PUT", json, authToken);

        AccountResponseDTO responseDTO = jsonToObject(makeRequest(adminBaseUrl + "/account/" +
                testAccount.getId(), "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(AccountResponseDTO.class));

        assertEquals(newName, responseDTO.getName());
    }

    @Test
    @Ignore
    public void testGetUser() throws Exception {
        // Create a new account and user
        testAccount2 = new Account.Builder()
                .name("" + System.currentTimeMillis() + "")
                .url("http://nodeable.com")
                .build();

        testAccount2 = userService.createAccount(testAccount2);

        String testUser2Username = System.currentTimeMillis() + "@nodeable.com";
        testUser2 = userService.createUser(new User.Builder()
                .username(testUser2Username)
                .account(testAccount2)
                .accountLocked(false)
                .userStatus(User.UserStatus.ACTIVATED)
                .accountOriginator(true)
                .alias("TheHoneyBadger")
                .fullname("Honey Badger")
                .password(testUser2Username)
                .build());
        String testUser3Username = System.currentTimeMillis() + "@nodeable.com";
        testUser3 = userService.createUser(new User.Builder()
                .username(testUser3Username)
                .account(testAccount)
                .accountLocked(false)
                .userStatus(User.UserStatus.ACTIVATED)
                .alias("TheHoneyBadger2")
                .fullname("Honey Badger2")
                .password(testUser3Username)
                .build());

        String authn2Token = login(testUser2Username, testUser2Username);

        // Make sure a user can can get their own information
        String req = makeRequest(getUrl() + "/" + testUser.getId(), "GET", null,
                authToken);
        UserResponseDTO responseDTO = jsonToObject(req, TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertEquals(testUser.getAlias(), responseDTO.getAlias());
        assertEquals(testUser.getUsername(), responseDTO.getUsername());

        // Make sure a user in the same account can get another user in the same account
        responseDTO = jsonToObject(makeRequest(getUrl() + "/" + testUser3.getId(), "GET", null, authToken),
                TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertEquals(testUser3.getAlias(), responseDTO.getAlias());
        assertEquals(testUser3.getUsername(), responseDTO.getUsername());

        // Make sure a user in one account cannot get a user in another account
        jsonToObject(makeRequest(getUrl() + "/" + testUser3.getId(), "GET", null, authn2Token),
                TypeFactory.defaultInstance().constructType(ErrorMessage.class));
    }


    @Test
    @Ignore
    public void testDisableUser() throws Exception {

        String testUser2Username = System.currentTimeMillis() + "@nodeable.com";

        testUser2 = userService.createUser(new User.Builder()
                .username(testUser2Username)
                .account(testUser.getAccount())
                .accountLocked(false)
                .userStatus(User.UserStatus.ACTIVATED)
                .accountOriginator(true)
                .alias("TheHoneyBadger")
                .fullname("Honey Badger")
                .password(testUser2Username)
                .build());

        User user = applicationManager.getUserService().getUser(testUser2.getUsername());
        assertFalse(user.isUserLocked());

        String url = getUrl() + "/" + testUser2.getId() + "/disable/";
        makeRequest(url, "PUT", null, authToken);


        user = applicationManager.getUserService().getUser(testUser2.getUsername());
        assertTrue(user.isUserLocked());

        // user should not be returned in the user account list now.
        String response = makeRequest(getPublicApiUrlBase() + "/account/users", "GET", null, authToken);
        List<UserResponseDTO> users = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                UserResponseDTO.class));

        assertNotNull(users);
        boolean exists = false;
        for (UserResponseDTO usr : users) {
            if (usr.getUsername().equals(testUser2.getUsername())) {
                exists = true;
            }
        }
        assertFalse(exists);

        url = getUrl() + "/" + testUser2.getId() + "/enable/";
        makeRequest(url, "PUT", null, authToken);

        user = applicationManager.getUserService().getUser(testUser2.getUsername());
        assertFalse(user.isUserLocked());

        // user should be returned in the user account list now.
        // user should not be returned in the user account list now.
        response = makeRequest(getPublicApiUrlBase() + "/account/users", "GET", null, authToken);
        users = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                UserResponseDTO.class));

        assertNotNull(users);
        exists = false;
        for (UserResponseDTO usr : users) {
            if (usr.getUsername().equals(testUser2.getUsername())) {
                exists = true;
            }
        }
        assertFalse(!exists);

    }

    @Test
    @Ignore
    public void testToggleVisibility() throws Exception {

        JSONObject json = new JSONObject();

        json.put("visibility", "SELF");

        makeRequest(getUrl() + "/profile", "PUT", json, authToken);

        String req = makeRequest(getUrl(), "GET", null, authToken);
        UserResponseDTO userDTO = jsonToObject(req, TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertEquals("SELF", String.valueOf(userDTO.getVisibility()));

        // set it back...
        User user = userService.getUserById(testUser.getId());
        user.setVisibility(SobaObject.Visibility.ACCOUNT);
        userService.updateUser(user);
    }

    @Test
    @Ignore
    public void testDeleteUser() throws Exception {

        // sometimes we fail to delete this... so don't just try it willy nilly.
        User user = null;
        try {
            user = userService.getUser("foobar@nodeable.com");

        } catch (UserNotFoundException unfe) {
        }

        if (user == null) {
            user = new User.Builder()
                    .username("foobar@foo.com")
                    .password("foobar@foo.com")
                    .accountLocked(false)
                    .fullname("Bogus user")
                    .userStatus(User.UserStatus.ACTIVATED)
                    .account(testAccount)
                    .roles(userService.getUserRoles())
                    .accountOriginator(true)
                    .alias(UUID.randomUUID().toString())
                    .build();
            user = userService.createUser(user);
        }
        // create bogus token
        applicationManager.getSecurityService().issueAuthenticationToken(user);

        //TODO:  NFI what this call used to do
//        assertNotNull(applicationManager.getSecurityService().findUserTokens(user));

        // kill user
        String url = getUrl() + "/" + user.getId();
        makeRequest(url, "DELETE", null, authToken);

        try {
            applicationManager.getUserService().getUser(user.getUsername());
            fail();
        } catch (UserNotFoundException e) {
        }

        //TODO:  NFI what this call used to do
//        assertEquals(applicationManager.getSecurityService().findUserTokens(user), Collections.emptyList());
    }

    @Test
    @Ignore
    public void testInviteUser() throws Exception {
        String userToInvite = "test_invite_user@nodeable.com";
        String response = makeRequest(usersBaseUrl + "/invite/" + userToInvite, "POST", null, authToken);

        assertFalse(response == null || response.equals(""));

        try {
            // Create an invalid invite
            ErrorMessage errorMessage = jsonToObject(makeRequest(usersBaseUrl + "/invite/" + userToInvite, "POST",
                    null, authToken), TypeFactory.defaultInstance().constructType(ErrorMessage.class));

            assertEquals("A user with that email address has already been invited.", errorMessage.getErrorMessage());

            // Resend the invite
            response = makeRequest(usersBaseUrl + "/invite/resend/" + userToInvite, "GET", null, authToken);

            assertEquals("200", response);

            // Resend the invalid invite
            errorMessage = jsonToObject(makeRequest(usersBaseUrl + "/invite/resend/fake_" + userToInvite, "GET",
                    null, authToken), TypeFactory.defaultInstance().constructType(ErrorMessage.class));

            assertEquals("User not found.", errorMessage.getErrorMessage());

            // Delete the invalid invite
            errorMessage = jsonToObject(makeRequest(usersBaseUrl + "/invite/fake_" + userToInvite, "DELETE",
                    null, authToken), TypeFactory.defaultInstance().constructType(ErrorMessage.class));

            assertEquals("No user found with the following id: fake_" + userToInvite, errorMessage.getErrorMessage());

            // Delete the valid invite
            response = makeRequest(usersBaseUrl + "/invite/" + userToInvite, "DELETE", null, authToken);

            assertEquals("200", response);
        } catch (AssertionError ae) {
            try {
                userService.deleteUser(userService.getUser(userToInvite));
            } catch (Exception e) {
                // Do nothing
            }

            throw ae;
        }
    }


    @Test
    @Ignore
    public void testAddRoleToUser() throws Exception {

        String url = getUrl() + "/" + testUser.getUsername();
        String req = makeRequest(url, "GET", null, authToken);
        UserResponseDTO user = jsonToObject(req, TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        int baseline = user.getRoles().size();

        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
        assertTrue(baseline > 0);

        Set<Role> roles = userService.getAccountRoles(user.getAccountId());
        Role devRole = null;
        for (Role role : roles) {
            if (role.getName().equals(Roles.DEVELOPER_ROLE)) {
                devRole = role;
                break;
            }
        }
        assertNotNull(devRole);

        // make user admin
        makeRequest(getUrl() + "/" + user.getId() + "/roles/" + devRole.getId(), "POST", null, authToken);

        // get user again
        url = getUrl() + "/" + testUser.getUsername();
        user = jsonToObject(makeRequest(url, "GET", null, authToken), TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertEquals(baseline + 1, user.getRoles().size());
    }


    @Test
    @Ignore
    public void testRemoveRoleFromUser() throws Exception {

        String url = getUrl() + "/" + testUser.getUsername();
        String req = makeRequest(url, "GET", null, authToken);
        UserResponseDTO user = jsonToObject(req, TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        int baseline = user.getRoles().size();

        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
        assertTrue(baseline > 0);


        Set<RoleResponseDTO> roles = user.getRoles();
        RoleResponseDTO userRole = null;
        for (RoleResponseDTO role : roles) {
            if (role.getName().equals(Roles.USER_ROLE)) {
                userRole = role;
                break;
            }
        }
        assertNotNull(userRole);

        // remove user role
        makeRequest(getUrl() + "/" + user.getId() + "/roles/" + userRole.getId(), "DELETE", null, authToken);

        // get user again
        url = getUrl() + "/" + testUser.getUsername();
        user = jsonToObject(makeRequest(url, "GET", null, authToken), TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertTrue(user.getRoles().size() > 0);

        assertEquals(baseline - 1, user.getRoles().size());

    }


    @Test
    @Ignore
    public void testUserEventLog() throws Exception {

        List<JSONObject> jsonObjectList = new ArrayList<>();

        JSONObject json = new JSONObject();
        json.put("tag", "user-click-tab");
        json.put("ts", new Date().getTime());

        JSONObject payload = new JSONObject();
        payload.put("name", "foo");
        payload.put("value", "bar");
        payload.put("etc", "yo");
        json.put("json", payload);

        jsonObjectList.add(json);
        jsonObjectList.add(json);
        jsonObjectList.add(json);

        String url = getPrivateUrlBase() + "/admin/user/eventlog";
        String req = makeRequest(url, "POST", jsonObjectList, authToken);

        assertEquals(req, "200");
    }

}
