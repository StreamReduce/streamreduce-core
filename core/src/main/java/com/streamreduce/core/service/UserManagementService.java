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

import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/3/12 21:18</p>
 */
public interface UserManagementService {

    String getAccounts(boolean summary);

    String getAccount(String accountObjectId, boolean summary) throws AccountNotFoundException;

    String getUsers(String accountObjectId, boolean enabledUsersOnly, boolean summary) throws AccountNotFoundException;

    String getUser(String userObjectId, boolean summary) throws UserNotFoundException;

}
