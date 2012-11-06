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

package com.streamreduce.util;

import com.streamreduce.Constants;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Simple class that provides helper methods for HTTP-related work.
 */
public final class HTTPUtils {

    public static Logger LOGGER = LoggerFactory.getLogger(HTTPUtils.class);

    private static HttpClient httpClient;
    static {
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        httpClient = new DefaultHttpClient(connectionManager);
    }

    public static String openOAuthUrl(String url, String method, String data, String mediaType,
                                      OAuthService oAuthService, ConnectionCredentials credentials,
                                      @Nullable List<Header> requestHeaders, List<Header> responseHeaders)
            throws InvalidCredentialsException, IOException {

        String oAuthToken = credentials.getOauthToken();
        String oAuthSecret = credentials.getOauthTokenSecret();
        Token token;

        if (StringUtils.hasText(oAuthSecret)) {
            token = new Token(oAuthToken, oAuthSecret);
        } else {
            token = new Token(oAuthToken, "");
        }

        OAuthRequest request = new OAuthRequest(Verb.valueOf(method.toUpperCase()), url);

        if (data != null) {
            request.addPayload(data);
        }

        if (requestHeaders == null) {
            requestHeaders = new ArrayList<Header>();
        }

        for (Header header : requestHeaders) {
            request.addHeader(header.getName(), header.getValue());
        }

        if (mediaType != null) {
            request.addHeader("Content-Type", mediaType);
        }

        oAuthService.signRequest(token, request);

        Response response = request.send();

        if (response.getCode() == 401 || response.getCode() == 403) {
            throw new InvalidCredentialsException("The OAuth Token is invalid, or has been revoked");
        } else if (response.getCode() != 200) {
            throw new IOException("Unexpected status code of " + response.getCode() + ": " + response.getBody() +
                                          " for a " + method + " request to" + url);
        }

        if (response.getHeaders() != null) {
            for (Map.Entry<String, String> headerKeyValue : response.getHeaders().entrySet()) {
                if (headerKeyValue.getKey() != null) {
                    Header h = new BasicHeader(headerKeyValue.getKey(), headerKeyValue.getValue());
                    responseHeaders.add(h);
                }
            }
        }

        if (response.getBody() == null) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getStream(), writer);
            return writer.toString();
        } else {
            return response.getBody();
        }
    }

    /**
     * Opens a connection to the specified URL with the supplied username and password,
     * if supplied, and then reads the contents of the URL.
     *
     * @param url             the url to open and read from
     * @param method          the method to use for the request
     * @param data            the request body as string
     * @param mediaType       the media type of the request
     * @param username        the username, if any
     * @param password        the password, if any
     * @param requestHeaders  the special request headers to send
     * @param responseHeaders save response headers
     * @return the read string from the
     * @throws InvalidCredentialsException if the connection credentials are invalid
     * @throws IOException                 if there is a problem with the request
     */
    public static String openUrl(String url, String method, String data,
                                 String mediaType, @Nullable String username, @Nullable String password,
                                 @Nullable List<Header> requestHeaders, @Nullable List<Header> responseHeaders)
            throws InvalidCredentialsException, IOException {

        String response = null;

        /* Set the cookie policy */
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        /* Set the user agent */
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Constants.NODEABLE_HTTP_USER_AGENT);

        HttpContext context = new BasicHttpContext();
        HttpRequestBase httpMethod;

        if (method.equals("DELETE")) {
            httpMethod = new HttpDelete(url);
        } else if (method.equals("GET")) {
            httpMethod = new HttpGet(url);
        } else if (method.equals("POST")) {
            httpMethod = new HttpPost(url);
        } else if (method.equals("PUT")) {
            httpMethod = new HttpPut(url);
        } else {
            throw new IllegalArgumentException("The method you specified is not supported.");
        }

        // Put data into the request for POST and PUT requests
        if (method.equals("POST") || method.equals("PUT") && data != null) {
            HttpEntityEnclosingRequestBase eeMethod = (HttpEntityEnclosingRequestBase) httpMethod;

            eeMethod.setEntity(new StringEntity(data, ContentType.create(mediaType, "UTF-8")));
        }

        /* Set the username/password if any */
        if (username != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(username, password));
            context.setAttribute(ClientContext.CREDS_PROVIDER, credentialsProvider);
        }

        /* Add request headers if need be */
        if (requestHeaders != null) {
            for (Header header : requestHeaders) {
                httpMethod.addHeader(header);
            }
        }

        LOGGER.debug("Making HTTP request as " + (username != null ? username : "anonymous") + ": " + method +
                " - " + url);

        /* Make the request and read the response */
        try {
            HttpResponse httpResponse = httpClient.execute(httpMethod);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                response = EntityUtils.toString(entity);
            }
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 401 || responseCode == 403) {
                throw new InvalidCredentialsException("The connection credentials are invalid.");
            } else if (responseCode < 200 || responseCode > 299) {
                throw new IOException("Unexpected status code of " + responseCode + " for a " + method + " request to " + url);
            }

            if (responseHeaders != null) {
                responseHeaders.addAll(Arrays.asList(httpResponse.getAllHeaders()));
            }
        } catch (IOException e) {
            httpMethod.abort();
            throw e;
        }

        return response;
    }

}
