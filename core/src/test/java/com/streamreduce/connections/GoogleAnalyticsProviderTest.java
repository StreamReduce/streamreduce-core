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
