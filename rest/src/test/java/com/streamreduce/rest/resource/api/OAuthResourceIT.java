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
