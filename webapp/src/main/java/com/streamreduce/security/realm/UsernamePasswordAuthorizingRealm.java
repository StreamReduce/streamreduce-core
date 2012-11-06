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

package com.streamreduce.security.realm;

import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.rest.resource.ErrorMessages;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

/**
 * Users the values stored in the config DB to validate the user. Relies on UserDAO.
 */
@Component
public class UsernamePasswordAuthorizingRealm extends NodeableAuthorizingRealm {

    public UsernamePasswordAuthorizingRealm() {
        setAuthenticationTokenClass(UsernamePasswordToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
        User user = userDAO.findUser(token.getUsername());

        // can find this username in the db.
        if (user == null) {
            logger.debug("User is null from the Basic DAO lookup: " + token.getUsername());
            throw new AuthenticationException(ErrorMessages.USER_NOT_FOUND);
        }

        // account is locked!
        if (user.isUserLocked() || user.getAccount().getConfigValue(Account.ConfigKey.ACCOUNT_LOCKED)) {
            logger.debug("User is locked or account is locked: " + token.getUsername() + " in account: " + user.getAccount().getName());
            throw new AuthenticationException(ErrorMessages.INACTIVE_ACCOUNT);
        }

        return new SimpleAuthenticationInfo(user.getId(), user.getPassword(), getName());
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return super.doGetAuthorizationInfo(principals);
    }

}
