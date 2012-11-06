package com.streamreduce.security.token;

public class UserAuthenticationToken extends NodeableAuthenticationToken {

    private static final long serialVersionUID = -6018287160169196683L;

    public UserAuthenticationToken(final String userToken) {
        super(userToken);
    }
}
