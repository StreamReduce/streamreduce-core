package com.streamreduce.core.model;

import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;

public class APIAuthenticationTokenEncrypter extends CredentialsEncrypter {

    @PrePersist
    @SuppressWarnings("unused")
    public void encrypt(APIAuthenticationToken token) {
        byte[] key = loadPasswordEncryptionKey();
        if (token != null) {
            token.setToken(internalEncrypt(token.getToken(), key));
        }
    }

    @PostLoad
    public void decrypt(APIAuthenticationToken token) {
        byte[] key = loadPasswordEncryptionKey();
        if (token != null) {
            token.setToken(internalDecrypt(token.getToken(), key));
        }
    }

}
