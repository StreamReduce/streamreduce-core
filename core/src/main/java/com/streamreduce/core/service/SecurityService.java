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

import com.streamreduce.ValidationException;
import com.streamreduce.core.model.APIAuthenticationToken;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.UserNotFoundException;

import java.util.Set;

public interface SecurityService {

    /**
     * Returns a the User associated with the request. If it's an IMG connection, it returns the User from that Connection,
     * based upon the API Key.
     *
     * @return - a valid User or AuthenticationException
     */
    User getCurrentUser();

    /**
     * If it's an IMG session, this returns the Connection associated with the API Key in Shiro
     *
     * @return - a valid Connection  or AuthenticationException
     */
    Connection getCurrentGatewayConnection();

    /**
     * Invalidate the Shiro session and remove the token from the local cache
     *
     * @param token - the API Auth Token
     */
    void logoutCurrentUser(String token);

    /**
     * Replace the current APIAuthenticationToken on the User object with a new one. This also persists the change to the User
     *
     * @param user - the user you want to persist the new token on
     * @return - the newly generated token.
     * @throws ValidationException
     * @throws UserNotFoundException
     */
    APIAuthenticationToken issueAuthenticationToken(User user) throws ValidationException, UserNotFoundException;

    boolean hasRole(String roleName);

    Set<User> getActiveUsers(Account account, Long maxInactivity);

    Role findRole(String role);

    /**
     * Find the User object associated with the API Authorization token
     *
     * @param token - the API Auth Token
     * @return - a valid User or null
     */
    User getUserFromAuthenticationToken(String token);

    /**
     * Find the Connection object by Type associated with the API Key
     *
     * @param token - the API Auth Token
     * @param type  - ?
     * @return a valid Connection or null
     */
    Connection getByApiKey(String token, String type);

}
