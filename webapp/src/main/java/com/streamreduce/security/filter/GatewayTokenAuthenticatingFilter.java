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
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.GatewayAuthenticaionToken;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;

public class GatewayTokenAuthenticatingFilter extends NodeableAuthenticatingFilter {

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        String token = getHeaderParameter(request);
        if (token == null) {
            logger.debug("Header Authorization token is null, throw exception.");
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }
        return new GatewayAuthenticaionToken(token);
    }


    @Override
    protected String getHeaderParameter(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader(Constants.NODEABLE_API_KEY);
    }

}

