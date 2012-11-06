package com.streamreduce.core.model;

import com.streamreduce.util.SecurityUtil;

import java.util.ResourceBundle;

import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CredentialsEncrypter {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    protected String internalEncrypt(String plaintext, byte[] key) {
        if (plaintext == null) {
            return null;
        }
        return SecurityUtil.encryptPassword(plaintext, key);
    }

    protected String internalDecrypt(String ciphertext, byte[] key) {
        if (ciphertext == null) {
            return null;
        }
        return SecurityUtil.decryptPassword(ciphertext, key);
    }

    protected byte[] loadPasswordEncryptionKey() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("application");
        String encoded = resourceBundle.getString("nodeable.encryptionKey");
        return Base64.decode(encoded);
    }

}
