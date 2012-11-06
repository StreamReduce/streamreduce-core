package com.streamreduce.rest.resource.api;

import com.streamreduce.AbstractServiceTestCase;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

public class OAuthResourceIT extends AbstractServiceTestCase {

    @Autowired
    @InjectMocks
    OAuthResource oAuthResource;


    @Test
    public void testOAuthUrlForGithub() {
        Response response = oAuthResource.getOAuthDetailsForProvider("github");
        Assert.assertEquals(200,response.getStatus());
        JSONObject responseBody = (JSONObject) response.getEntity();
        String authorizationUrl = responseBody.getString("authorizationUrl");
        Assert.assertTrue(authorizationUrl.startsWith("https://github.com/login/oauth/authorize"));
    }

    @Test
    public void testOAuthUrlForTwitter() {
        Response response = oAuthResource.getOAuthDetailsForProvider("twitter");
        Assert.assertEquals(200,response.getStatus());
        JSONObject responseBody = (JSONObject) response.getEntity();
        String authorizationUrl = responseBody.getString("authorizationUrl");
        Assert.assertTrue(authorizationUrl.startsWith("https://api.twitter.com/oauth/authorize?oauth_token"));
    }

    @Test
    public void testOAuthUrlForNonOAuthAccount() {
        Response response = oAuthResource.getOAuthDetailsForProvider("jira");
        Assert.assertEquals(400,response.getStatus());
    }
}
