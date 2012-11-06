package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.Constants;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.junit.Ignore;
import org.junit.Test;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthenticationResourceITCase extends AbstractInContainerTestCase {

    protected String getUrl() {
        return getPublicApiUrlBase() + "/authentication/login";
    }

    public AuthenticationResourceITCase() {
        super();
    }

    @Test
    @Ignore
    public void testConsoleLoginOk() throws Exception {

        HttpClient httpClient = new DefaultHttpClient();

        // Set the User-Agent to be safe
        httpClient.getParams().setParameter(HttpProtocolParams.USER_AGENT, Constants.NODEABLE_HTTP_USER_AGENT);

        HttpPost httpPost = new HttpPost(getPublicUrlBase() + "/authentication/login");
        //HttpState state = httpClient.getState();
        String authnToken;

        HttpResponse response = null;
        try {

            // Login is done via Basic Authentication at this time

//            state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME),
//                    new UsernamePasswordCredentials(testUsername, testUsername));

            response = httpClient.execute(httpPost);
            authnToken = response.getFirstHeader(Constants.NODEABLE_AUTH_TOKEN).getValue();
        } finally {
            httpPost.releaseConnection();
        }

        if (response != null) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        }

        assertNotNull(authnToken);
    }

    @Ignore
    @Test
    public void testConsoleLoginFailure() throws Exception {

        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(HttpProtocolParams.USER_AGENT, Constants.NODEABLE_HTTP_USER_AGENT);

//        HttpState state = client.getState();
//        state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME),
//                new UsernamePasswordCredentials(testUsername, "wrong_password"));
        HttpPost post = new HttpPost(getUrl());

        try {
            int status = client.execute(post).getStatusLine().getStatusCode();
            assertEquals(HttpStatus.SC_UNAUTHORIZED, status);
        } finally {
            post.releaseConnection();
        }
    }

}
