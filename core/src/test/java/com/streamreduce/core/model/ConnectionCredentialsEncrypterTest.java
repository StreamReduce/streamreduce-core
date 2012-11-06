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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/30/12 09:02</p>
 */
public class ConnectionCredentialsEncrypterTest {

    private ConnectionCredentialsEncrypter encrypter = new ConnectionCredentialsEncrypter();
    private ConnectionCredentials credentials = new ConnectionCredentials();

    @Before
    public void setUp() throws Exception {
        credentials.setCredential("my-credential");
        credentials.setIdentity("my-identity");
        credentials.setApiKey("my-api-key");
        credentials.setOauthToken("my-oauth-token");
        credentials.setOauthTokenSecret("my-oauth-token-secret");
        encrypter.encrypt(credentials);
    }

    @Test
    public void testEncrypt() throws Exception {
        Assert.assertEquals(credentials.getIdentity(), "my-identity");
        Assert.assertFalse("my-api-key".equals(credentials.getApiKey()));
        Assert.assertFalse("my-credential".equals(credentials.getCredential()));
        Assert.assertFalse("my-oauth-token".equals(credentials.getOauthToken()));
        Assert.assertFalse("my-oauth-token-secret".equals(credentials.getOauthTokenSecret()));
    }

    @Test
    public void testDecrypt() throws Exception {
        encrypter.decrypt(credentials);
        Assert.assertEquals("my-identity", credentials.getIdentity());
        Assert.assertEquals("my-api-key",credentials.getApiKey());
        Assert.assertEquals("my-credential", credentials.getCredential());
        Assert.assertEquals("my-oauth-token", credentials.getOauthToken());
        Assert.assertEquals("my-oauth-token-secret", credentials.getOauthTokenSecret());
    }
}
