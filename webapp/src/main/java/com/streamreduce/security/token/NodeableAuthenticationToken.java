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
