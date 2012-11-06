package com.streamreduce.core.model;

import com.streamreduce.util.SecurityUtil;

// TODO: store these ecrypted
//@EntityListeners(APIAuthenticationTokenEncrypter.class)
public class APIAuthenticationToken {

    private String token;

    public APIAuthenticationToken() {
        token = SecurityUtil.issueRandomAPIToken();
    }

    public APIAuthenticationToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
