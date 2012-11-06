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

package com.streamreduce.security.token;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Each realm consumes a specific token type
 */
public abstract class NodeableAuthenticationToken implements AuthenticationToken {

    private static final long serialVersionUID = -3385055099647647626L;
    private final String userToken;

    public NodeableAuthenticationToken(final String userToken) {
        this.userToken = userToken;
    }

    public String getToken() {
        return getPrincipal().toString();
    }

    @Override
    public Object getPrincipal() {
        return userToken;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
}
