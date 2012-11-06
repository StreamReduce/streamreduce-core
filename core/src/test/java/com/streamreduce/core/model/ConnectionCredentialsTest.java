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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class to validate equality/hashcode overrides on ConnectionCredentials
 */
public class ConnectionCredentialsTest {

    @Test
    public void testEquals() {
        ConnectionCredentials c1 = new ConnectionCredentials("USER", "pass");
        ConnectionCredentials c2 = new ConnectionCredentials("user", "pass");
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertTrue(c1.equals(c1));
    }

    @Test
    public void testHashCode() {
        ConnectionCredentials c1 = new ConnectionCredentials("USER", "pass");
        ConnectionCredentials c2 = new ConnectionCredentials("user", "pass");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testCopyOf() {
        ConnectionCredentials cc = new ConnectionCredentials("USER", "pass","alkhfalsfhasjlf");
        cc.setOauthToken("ahsjldfahslfashlfsa");
        cc.setOauthTokenSecret("kjahsglfkashflkashgflkashgflkas");
        cc.setOauthVerifier("i27salkfghaskjgnas");

        ConnectionCredentials copy = ConnectionCredentials.copyOf(cc);
        Assert.assertEquals(cc,copy);
        Assert.assertNotSame(cc,copy);
    }
}
