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

package com.streamreduce.rest.resource.admin;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.InvalidUserAliasException;
import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import com.streamreduce.util.JSONObjectBuilder;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;


import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminUserVerificationResourceIT extends AbstractServiceTestCase {

    @Autowired
    AdminUserVerificationResource adminUserVerificationResource;
    @Autowired
    UserDAO userDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();

    }

    @Test
    public void testCompleteUserSignupProcess_BadAliasReturnsConstraintViolationDTO() throws Exception {
        User user = new User.Builder().username("maynard@toolband.com")
                .secretKey("aaaaaaaaa")
                .password("bbbbbbbb")
                .roles(userService.getAdminRoles())
                .build();
        user.setAccountOriginator(true);


        UserService mockUserService = mock(UserService.class);
        when(mockUserService.isAliasAvailable(any(Account.class), anyString())).thenReturn(true);
        when(mockUserService.getUserFromSignupKey(anyString(),anyString())).thenReturn(user);
        when(mockUserService.createUser(user)).thenThrow(new InvalidUserAliasException("worst test ever"));

        ReflectionTestUtils.setField(adminUserVerificationResource,"userService",mockUserService);

        Response response = adminUserVerificationResource.completeUserSignupProcess("", new ObjectId().toString(),
                new JSONObjectBuilder()
                        .add("password", "password")
                        .add("alias", "maynard@toolband.com")
                        .add("fullname", "MJK")
                        .add("accountName", "TOOLBAND").build());

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),response.getStatus());
        ConstraintViolationExceptionResponseDTO dto = (ConstraintViolationExceptionResponseDTO) response.getEntity();
        Assert.assertTrue(StringUtils.isNotBlank(dto.getViolations().get("alias")));
    }

    @Test
    public void testCompleteUserInviteProcess_BadAliasReturnsConstraintViolationDTO() throws Exception {
        User user = new User.Builder().username("maynard@toolband.com")
                .secretKey("aaaaaaaaa")
                .password("bbbbbbbb")
                .roles(userService.getAdminRoles())
                .build();
        user.setAccountOriginator(true);


        UserService mockUserService = mock(UserService.class);
        when(mockUserService.isAliasAvailable(any(Account.class), anyString())).thenReturn(true);
        when(mockUserService.getUserFromInvite(anyString(),anyString())).thenReturn(user);
        when(mockUserService.createUser(user)).thenThrow(new InvalidUserAliasException("worst test ever"));

        ReflectionTestUtils.setField(adminUserVerificationResource,"userService",mockUserService);

        Response response = adminUserVerificationResource.completeUserInviteProcess("", new ObjectId().toString(),
                new JSONObjectBuilder()
                        .add("password", "password")
                        .add("alias", "maynard@toolband.com")
                        .add("fullname", "MJK")
                        .add("accountName", "TOOLBAND").build());

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),response.getStatus());
        ConstraintViolationExceptionResponseDTO dto = (ConstraintViolationExceptionResponseDTO) response.getEntity();
        Assert.assertTrue(StringUtils.isNotBlank(dto.getViolations().get("alias")));
    }
}
