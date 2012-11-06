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
import com.streamreduce.Constants;
import com.streamreduce.rest.dto.response.UserResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;

import net.sf.json.JSONObject;

import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AdminUserResourceITCase extends AbstractInContainerTestCase {

    private String authToken;

    protected String getUrl() {
        return getPrivateUrlBase() + "/admin/user";
    }

    public AdminUserResourceITCase() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // login
        authToken = login(testUsername, testUsername);
    }

    @Test
    @Ignore
    public void testUserAvailability() throws Exception {
        String str = makeRequest(getUrl() + "/available/no_user_exists@nodeable.com", "HEAD", null, authToken);
        assertEquals("200", str);

    }

    @Test
    @Ignore
    public void testUserNotAvailable() throws Exception {
        String str = makeRequest(getUrl() + "/available/" + Constants.NODEABLE_SUPER_USERNAME, "HEAD", null, authToken);
        assertEquals("409", str);
    }


    @Test
    @Ignore
    public void testGetUserById() throws Exception {
        String url = usersBaseUrl + "/" + testUser.getId();
        UserResponseDTO user = jsonToObject(makeRequest(url, "GET", null, authToken), TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertNotNull(user);
        assertEquals(testUser.getUsername(), user.getUsername());

    }

    @Test
    @Ignore
    public void testGetUserByUsername() throws Exception {
        String url = getUrl() + "/" + testUser.getUsername();
        UserResponseDTO user = jsonToObject(makeRequest(url, "GET", null, authToken), TypeFactory.defaultInstance().constructType(UserResponseDTO.class));

        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());

    }

    @Test
    @Ignore
    public void testCreateBugReport() throws Exception {
        JSONObject json = new JSONObject();

        json.put("username", testUser.getUsername());
        json.put("company", testUser.getAccount().getName());
        json.put("debugInfo", "some kind of debug info");
        json.put("summary", "This is the summary of what the bug is about");
        json.put("description", "This is the bug description, it makes me sad");

        // not such a good idea.
//        String response = makeRequest(getUrl() + "/email/issue", "POST", bug, null);
//        assertEquals("", response);

    }


    @Test
    @Ignore
    public void testNewUserRequest() throws Exception {
        String userToInvite = "test_invite_user@nodeable.com";
        String response = makeRequest(adminBaseUrl + "/user/" + userToInvite, "POST", null, authToken);

        assertEquals("201", response);

        try {
            // Send a duplicate invite
            ErrorMessage errorMessage = jsonToObject(makeRequest(adminBaseUrl + "/user/" + userToInvite, "POST",
                    null, authToken), TypeFactory.defaultInstance().constructType(ErrorMessage.class));

            assertEquals("Username is not available " + userToInvite, errorMessage.getErrorMessage());

            // No API to do this
            userService.deleteUser(userService.getUser(userToInvite));
        } catch (AssertionError ae) {
            try {
                userService.deleteUser(userService.getUser(userToInvite));
            } catch (Exception e) {
                // Do nothing
            }

            throw ae;
        }
    }

}
