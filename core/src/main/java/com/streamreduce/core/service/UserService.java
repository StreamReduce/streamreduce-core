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

import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.core.service.exception.UsernameUnavailableException;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Set;

public interface UserService {

    boolean isUsernameAvailable(String name);

    boolean isAliasAvailable(Account account, String alias);

    Account getAccount(ObjectId accountId) throws AccountNotFoundException;

    Set<Role> getAccountRoles(ObjectId accountId) throws AccountNotFoundException;

    void deleteAccount(ObjectId acountId);

    Account createAccount(Account account);

    void updateAccount(Account account);

    User createUser(User user);

    User createUserRequest(User user) throws UsernameUnavailableException;

    void recreateUserRequest(User user) throws UserNotFoundException;

    void deleteUserInvite(User user) throws UserNotFoundException;

    User getUserById(ObjectId userId) throws UserNotFoundException;

    List<User> getUsersById(Set<ObjectId> userIds);

    User getUserById(ObjectId userId, Account account) throws UserNotFoundException;

    User getUserByAuthenticationToken(String authToken) throws UserNotFoundException;

    User getUser(String username) throws UserNotFoundException;

    User getUser(String username, Account account) throws UserNotFoundException;

    // will search by username and alias (so you can pass in either)
    User getTargetUser(Account account, String name) throws UserNotFoundException;

    User getUserFromInvite(String inviteKey, String accountId) throws UserNotFoundException;

    User getUserFromSignupKey(String signupKey, String userId) throws UserNotFoundException;

    void deleteUser(User user);

    void updateUser(User user);

    void resetUserPassword(User user, boolean mobile);

    void deleteUsersForAccount(Account account);

    List<User> allUsersForAccount(Account account);

    List<User> allEnabledUsersForAccount(Account account);

    Set<Role> getUserRoles();

    Set<Role> getAdminRoles();

    List<Account> getAccounts();

    void addRole(User user, ObjectId roleId) throws UserNotFoundException;

    void removeRole(User user, ObjectId roleId) throws UserNotFoundException;

    ObjectId addToEventLog(User user, String name, JSONObject value, Long timestamp);

    void deletePendingUser(User user);

    // nodebelly user
    User getSuperUser();

    // nodeable user
    User getSystemUser();

    /**
     * Returns the user object for the account administrator.
     * @param account account
     * @return administrator or null if no admin is found.
     */
    User getAccountAdmin(Account account);

    void handleInitialInsightForAccount(Account account);

}
