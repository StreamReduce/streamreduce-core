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

import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.GatewayAuthenticaionToken;
import com.streamreduce.security.token.NodeableAuthenticationToken;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GatewayTokenAuthorizingRealm extends NodeableAuthorizingRealm {

    @Autowired
    private SecurityService securityService;
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    public GatewayTokenAuthorizingRealm() {
        setAuthenticationTokenClass(GatewayAuthenticaionToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {

        NodeableAuthenticationToken token = (GatewayAuthenticaionToken) authcToken;

        logger.debug("Attempting to get gateway api authentication info for" + ((GatewayAuthenticaionToken) authcToken).getToken());

        Connection connection = securityService.getByApiKey(token.getToken(), GatewayProvider.TYPE);

        if (connection == null) {
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }

        logger.debug("ConnectionId is set to " + connection.getId());

        // all is well so far...
        return new SimpleAuthenticationInfo(connection.getCredentials().getIdentity(), "", getName());
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

}
