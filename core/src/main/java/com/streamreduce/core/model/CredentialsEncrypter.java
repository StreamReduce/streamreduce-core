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
