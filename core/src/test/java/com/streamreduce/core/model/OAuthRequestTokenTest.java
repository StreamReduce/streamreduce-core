package com.streamreduce.core.model;

import org.junit.Test;
import org.scribe.model.Token;

import static org.junit.Assert.assertEquals;

public class OAuthRequestTokenTest {

    @Test
    public void testToScribeToken() throws Exception {
        Token token = new Token("access","secret");
        OAuthRequestToken  requestToken = new OAuthRequestToken(token);
        assertEquals(token.getToken(), requestToken.toScribeToken().getToken());
        assertEquals(token.getSecret(), requestToken.toScribeToken().getSecret());
    }
}
