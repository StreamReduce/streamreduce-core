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

package com.streamreduce.connections;

import com.streamreduce.core.model.ConnectionCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.scribe.model.Token;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/6/12 15:12</p>
 */
public class GoogleAnalyticsProviderTest {

    @Test
    public void testUpdateCredentials() throws Exception {
        Token mockToken = Mockito.mock(Token.class);
        Mockito.when(mockToken.getRawResponse()).thenReturn("{\n" +
                "  \"access_token\" : \"ya29.AHES6ZS2UHOclj7rSPx8lAKbQo2Z6AOPmGeSlaeGA5eMxhe5nO6pAg\",\n" +
                "  \"token_type\" : \"Bearer\",\n" +
                "  \"expires_in\" : 3600,\n" +
                "  \"refresh_token\" : \"1/1B3mzvdzu7dEpQKa2PHoBigmhu-Jefuj-a90Cs7AsEw\"\n" +
                "}");
        ConnectionCredentials credentials = new ConnectionCredentials();

        new GoogleAnalyticsProvider().updateCredentials(credentials, mockToken);

        Assert.assertNotNull(credentials.getOauthToken());
        Assert.assertEquals(credentials.getOauthToken(), "ya29.AHES6ZS2UHOclj7rSPx8lAKbQo2Z6AOPmGeSlaeGA5eMxhe5nO6pAg");
        Assert.assertNotNull(credentials.getOauthRefreshToken());
        Assert.assertEquals(credentials.getOauthRefreshToken(), "1/1B3mzvdzu7dEpQKa2PHoBigmhu-Jefuj-a90Cs7AsEw");
    }
}
