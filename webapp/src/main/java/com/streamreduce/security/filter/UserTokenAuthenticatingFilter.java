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

package com.streamreduce.security.filter;

import com.streamreduce.Constants;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.NodeableAuthenticationToken;
import com.streamreduce.security.token.UserAuthenticationToken;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;

public class UserTokenAuthenticatingFilter extends NodeableAuthenticatingFilter {

    @Autowired
    private UserService userService;

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        String token = getHeaderParameter(request);
        if (token == null) {
            logger.debug("Header Authorization token is null, throw exception ");
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }
        return new UserAuthenticationToken(token);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        User user = userService.getUserByAuthenticationToken(((NodeableAuthenticationToken) token).getToken());
        user.setLastActivity(new Date());
        userService.updateUser(user);
        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected String getHeaderParameter(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader(Constants.NODEABLE_AUTH_TOKEN);
    }

}
