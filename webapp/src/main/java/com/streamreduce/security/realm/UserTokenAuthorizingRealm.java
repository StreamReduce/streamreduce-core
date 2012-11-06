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

import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.NodeableAuthenticationToken;
import com.streamreduce.security.token.UserAuthenticationToken;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public final class UserTokenAuthorizingRealm extends NodeableAuthorizingRealm {

    @Autowired
    private SecurityService securityService;

    public UserTokenAuthorizingRealm() {
        setAuthenticationTokenClass(UserAuthenticationToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {

        NodeableAuthenticationToken token = (UserAuthenticationToken) authcToken;

        logger.debug("Attempting to get authentication info for" + ((UserAuthenticationToken) authcToken).getToken());

        User theUser = securityService.getUserFromAuthenticationToken(token.getToken());

        if (theUser == null) {
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }

        logger.debug("UserId is set to " + theUser.getUser().getId());

        // token is expired
//        if (userToken.getExpirationDate() < System.currentTimeMillis()) {
//            throw new AuthenticationException(ErrorMessages.EXPIRED_CREDENTIAL);
//        }
        // all is well so far...

        return new SimpleAuthenticationInfo(theUser.getId(), "", getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return super.doGetAuthorizationInfo(principals);
    }

}

