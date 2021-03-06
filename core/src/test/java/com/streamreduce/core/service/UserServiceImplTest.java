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

import com.streamreduce.InvalidUserAliasException;
import com.streamreduce.core.model.User;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Random;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

    @Mock
    @SuppressWarnings("unused")
    private User mockUser;

    private UserServiceImpl userService = new UserServiceImpl();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateUserAlias_CorrectUserAlias() {
        when(mockUser.getAlias()).thenReturn("nodebelly");
        userService.validateUserAlias(mockUser);
    }

    @Test(expected = InvalidUserAliasException.class)
    public void testValidateUserAlias_leadingWhitespace() {
        when(mockUser.getAlias()).thenReturn("\tnodebelly");
        userService.validateUserAlias(mockUser);
    }

    @Test(expected = InvalidUserAliasException.class)
    public void testValidateUserAlias_trailingWhitespace() {
        when(mockUser.getAlias()).thenReturn("nodebelly\n");
        userService.validateUserAlias(mockUser);
    }

    @Test(expected = InvalidUserAliasException.class)
    public void testValidateUserAlias_WhitespaceInName() {
        when(mockUser.getAlias()).thenReturn("node belly");
        userService.validateUserAlias(mockUser);
    }

    @Test
    public void testValidateUserAlias_NonAlphaCharacters() {
        //Test only typical special chars on a US layout keyboard in a dumb way.

        Random random = new Random();
        String userName = "nodebelly";
        for (char c : "!@#$%^&*()+{}[]=;'<,.>/?~`".toCharArray()) {
            int index = random.nextInt(userName.length());;
            CharSequence begin = userName.subSequence(0,index);
            CharSequence end = userName.subSequence(index,userName.length());
            String userNameWithNonAlphaChar = new StringBuilder().append(begin).append(c).append(end).toString();
            when(mockUser.getAlias()).thenReturn("@nodebelly");
            try {
                userService.validateUserAlias(mockUser);
            } catch (InvalidUserAliasException e) {
                continue;
                //expected
            }
            fail(userNameWithNonAlphaChar + " is an invalid user alias but was allowed by " +
                    "UserServiceImpl.validateUserAlias ");
        }
    }
}
