package com.streamreduce.core.model;

import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;

public class ConnectionCredentialsEncrypter extends CredentialsEncrypter{

    @PrePersist
    @SuppressWarnings("unused")
    public void encrypt(ConnectionCredentials credentials) {
        byte[] key = loadPasswordEncryptionKey();
        credentials.setCredential(internalEncrypt(credentials.getCredential(), key));
        credentials.setApiKey(internalEncrypt(credentials.getApiKey(), key));
        credentials.setOauthToken(internalEncrypt(credentials.getOauthToken(),key));
        credentials.setOauthTokenSecret(internalEncrypt(credentials.getOauthTokenSecret(), key));
    }

    @PostLoad
    public void decrypt(ConnectionCredentials credentials) {
        byte[] key = loadPasswordEncryptionKey();
        credentials.setCredential(internalDecrypt(credentials.getCredential(), key));
        credentials.setApiKey(internalDecrypt(credentials.getApiKey(), key));
        credentials.setOauthToken(internalDecrypt(credentials.getOauthToken(),key));
        credentials.setOauthTokenSecret(internalDecrypt(credentials.getOauthTokenSecret(), key));
    }

}
