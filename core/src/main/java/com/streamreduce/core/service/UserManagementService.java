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
