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

package com.streamreduce;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.CloudProvider;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.resource.ErrorMessage;
import com.streamreduce.security.Roles;
import com.streamreduce.util.ConnectionUtils;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;
import static org.junit.Assert.assertEquals;


public abstract class AbstractInContainerTestCase extends AbstractServiceTestCase {

    public final ResourceBundle applicationProperties = ResourceBundle.getBundle("application");
    public final ResourceBundle cloudProperties = ResourceBundle.getBundle("cloud");
    public final ResourceBundle gitHubProperties = ResourceBundle.getBundle("github");
    public final ResourceBundle jiraProperties = ResourceBundle.getBundle("jira");
    public final ResourceBundle serverProperties = ResourceBundle.getBundle("server");
    public final String accountBaseUrl = getPublicApiUrlBase() + "/account";
    public final String adminBaseUrl = getPrivateUrlBase() + "/admin";
    public final String connectionsBaseUrl = getPublicApiUrlBase() + "/connections";
    public final String messagesBaseUrl = getPublicApiUrlBase() + "/messages";
    public final String imgBaseUrl = getPublicUrlBase() + "/gateway/custom";
    public final String inventoryItemBaseUrl = getPublicApiUrlBase() + "/inventory";
    public final String usersBaseUrl = getPublicApiUrlBase() + "/user";
    public final String testJcloudsInstanceGroup = "nodeable-test";
    public final String testUsername = "in_container_test_user@nodeable.com";

    public enum AuthTokenType {
        API,
        GATEWAY
    }

    protected Account testAccount;
    protected User testUser;
    @Autowired
    protected UserService userService;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUp(true);
    }

    protected void setUp(boolean startAMQ) throws Exception {

        // Create the test account and user
        // sometimes we fail to delete this... so don't just try it willy nilly.
        User user = null;
        try {
            user = userService.getUser(testUsername);
            testUser = user;
            testAccount = testUser.getAccount();

        } catch (UserNotFoundException unfe) {
        }

        if (user == null) {

            testAccount = new Account.Builder()
                    .name("In Container Test Account")
                    .description("This account is for testing in container.")
                    .url("http://nodeable.com")
                    .build();

            testAccount = userService.createAccount(testAccount);

            Set<Role> roles = userService.getAdminRoles();
            roles.add(applicationManager.getSecurityService().findRole(Roles.ADMIN_ROLE));

            testUser = new User.Builder()
                    .username(testUsername)
                    .password(testUsername)
                    .accountLocked(false)
                    .fullname("In Container Test User")
                    .userStatus(User.UserStatus.ACTIVATED)
                    .account(testAccount)
                    .roles(roles)
                    .accountOriginator(true)
                    .alias(UUID.randomUUID().toString())
                    .build();


            testUser = userService.createUser(testUser);
        }

        // TODO: Fix problem where local SecurityService doesn't see users logged in via RESTful API
    }



    @Override
    @After
    public void tearDown() throws Exception {
        // Delete the test account and user

        if (userService != null) {
            userService.deleteUser(testUser);
            userService.deleteAccount(testAccount.getId());

        }

        super.tearDown();
    }

    /**
     * Returns the user created for this test run.
     *
     * @return the user
     */
    public User getTestUser() {
        return testUser;
    }

    /**
     * Returns the base url for private endpoints.
     *
     * @return the private endpoint base url
     */
    public String getPrivateUrlBase() {
        String hostname = serverProperties.getString("server.host");
        String port = serverProperties.getString("server.port.private");
        return "http://" + hostname + ":" + port;
    }

    /**
     * Returns the base url for public endpoints.
     *
     * @return the public endpoint base url
     */
    public String getPublicUrlBase() {
        String hostname = serverProperties.getString("server.host");
        String port = serverProperties.getString("server.port.public");
        return "http://" + hostname + ":" + port;
    }

    /**
     * Returns the base url for public API endpoints.
     *
     * @return the public api endpoint base url
     */
    public String getPublicApiUrlBase() {
        return getPublicUrlBase() + "/api";
    }

    /**
     * Logs in the given user and returns the user's authentication token.
     *
     * @param username the username to login as
     * @param password the password for the username
     * @return the user's authentication token
     * @throws Exception if something goes wrong
     */
    public String login(String username, String password) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();

        // Set the User-Agent to be safe
        httpClient.getParams().setParameter(HttpProtocolParams.USER_AGENT, Constants.NODEABLE_HTTP_USER_AGENT);

        HttpPost post = new HttpPost(getPublicUrlBase() + "/authentication/login");
//        HttpState state = httpClient.getState();
        String authnToken;

        try {
            // Login is done via Basic Authentication at this time

//            state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME),
//                    new UsernamePasswordCredentials(username, password));

            HttpResponse httpReponse = httpClient.execute(post);

            Header authHeader = httpReponse.getFirstHeader(Constants.NODEABLE_AUTH_TOKEN);

            if (authHeader != null) {
                authnToken = httpReponse.getFirstHeader(Constants.NODEABLE_AUTH_TOKEN).getValue();
            } else {
                String response = IOUtils.toString(httpReponse.getEntity().getContent());

                try {
                    ErrorMessage em = jsonToObject(response,
                            TypeFactory.defaultInstance().constructType(ErrorMessage.class));
                    throw new Exception(em.getErrorMessage());
                } catch (Exception e) {
                    throw new Exception("Unable to login: " + response);
                }
            }
        } finally {
            post.releaseConnection();
        }

        return authnToken;
    }

    /**
     * Logs a user out.
     *
     * @param authnToken the user's authentication url
     * @throws Exception if something goes wrong
     */
    public void logout(String authnToken) throws Exception {
        makeRequest(getPublicUrlBase() + "/api/user/logout", "GET", null, authnToken);
    }

    public String makeRequest(String url, String method, Object data, String authnToken)
            throws Exception {
        return makeRequest(url, method, data, authnToken, AuthTokenType.API);
    }

    /**
     * Makes a request to the given url using the given method and possibly submitting
     * the given data.  If you need the request to be an authenticated request, pass in
     * your authentication token as well.
     *
     * @param url        the url for the request
     * @param method     the method to use for the request
     * @param data       the data, if any, for the request
     * @param authnToken the token retrieved during authentication
     * @param type       API or GATEWAY, tells us the auth token key to use
     * @return the response as string (This is either the response payload or the status code if no payload is sent)
     * @throws Exception if something goes wrong
     */
    public String makeRequest(String url, String method, Object data, String authnToken, AuthTokenType type)
            throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpRequestBase request;
        String actualPayload;

        // Set the User-Agent to be safe
        httpClient.getParams().setParameter(HttpProtocolParams.USER_AGENT, Constants.NODEABLE_HTTP_USER_AGENT);

        // Create the request object
        if (method.equals("DELETE")) {
            request = new HttpDelete(url);
        } else if (method.equals("GET")) {
            request = new HttpGet(url);
        } else if (method.equals("POST")) {
            request = new HttpPost(url);
        } else if (method.equals("PUT")) {
            request = new HttpPut(url);
        } else if (method.equals("HEAD")) {
            request = new HttpHead(url);
        } else {
            throw new IllegalArgumentException("The method you specified is not supported.");
        }

        // Put data into the request for POST and PUT requests
        if (method.equals("POST") || method.equals("PUT")) {
            HttpEntityEnclosingRequestBase eeMethod = (HttpEntityEnclosingRequestBase) request;
            String requestBody = data instanceof JSONObject ? ((JSONObject) data).toString() :
                    new ObjectMapper().writeValueAsString(data);

            eeMethod.setEntity(new StringEntity(requestBody, MediaType.APPLICATION_JSON, "UTF-8"));
        }

        // Add the authentication token to the request
        if (authnToken != null) {
            if (type.equals(AuthTokenType.API)) {
                request.addHeader(Constants.NODEABLE_AUTH_TOKEN, authnToken);
            } else if (type.equals(AuthTokenType.GATEWAY)) {
                request.addHeader(Constants.NODEABLE_API_KEY, authnToken);
            } else {
                throw new Exception("Unsupported Type of  " + type + " for authToken " + authnToken);
            }

        }

        // Execute the request
        try {
            HttpResponse response = httpClient.execute(request);
            String payload = IOUtils.toString(response.getEntity().getContent());

            // To work around HEAD, where we cannot receive a payload, and other scenarios where the payload could
            // be empty, let's stuff the response status code into the response.
            actualPayload = payload != null &&
                    payload.length() > 0 ?
                    payload :
                    Integer.toString(response.getStatusLine().getStatusCode());
        } finally {
            request.releaseConnection();
        }

        return actualPayload;
    }

    /**
     * Returns an object from the passed in JSON string based on the JavaType passed in.
     *
     * @param <T>  Variable type based on the JavaType passed in
     * @param json the JSON string to parse
     * @param type the type to return
     * @return the parsed object
     * @throws Exception if something goes wrong
     */

    @SuppressWarnings("unchecked")
    public <T> T jsonToObject(String json, JavaType type) throws Exception {
        ObjectMapper om = new ObjectMapper();

        try {
            return (T) om.readValue(json, type);
        } catch (Exception e) {
            try {
                ErrorMessage em = om.readValue(json, ErrorMessage.class);

                throw new Exception(em.getErrorMessage());
            } catch (IOException e2) {
                throw new IOException(e.getMessage() + ": " + json);
            }
        }
    }

    /**
     * Creates a dummy AWS EC2 node used for testing inventory.
     *
     * @param cloud         the cloud to create the node in/with
     * @param securityGroup the security group to create the node in/with
     * @return the created node
     * @throws Exception if something goes wrong
     */
    public NodeMetadata createDummyAWSEC2Node(ConnectionResponseDTO cloud, String securityGroup) throws Exception {
        ComputeService computeService = getComputeService(cloud);

        // Create a node
        Statement bootInstructions = AdminAccess.standard();
        NodeMetadata newNode = getOnlyElement(computeService.createNodesInGroup(securityGroup, 1,
                runScript(bootInstructions)));

        return newNode;
    }

    public ComputeService getComputeService(ConnectionResponseDTO cloud) {
        CloudProvider provider = (CloudProvider) ConnectionUtils.getProviderFromId(CloudProvider.TYPE,
                cloud.getProviderId());
        Properties overrides = new Properties();

        // Choose from only Amazon provided AMIs
        overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY,
                "owner-id=137112412989;state=available;image-type=machine");
        overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");

        // Inject the SSH implementation
        Iterable<Module> modules = ImmutableSet.<Module>of(new SshjSshClientModule());

        return new ComputeServiceContextFactory().createContext(provider.getComputeId(),
                cloudProperties.getString("nodeable.aws.accessKeyId"), cloudProperties.getString("nodeable.aws.secretKey"), modules,
                overrides).getComputeService();
    }

    public void refreshCloudInventoryItemCache(Connection cloud, String authnToken) throws Exception {
        assertEquals("200",
                makeRequest(connectionsBaseUrl + "/" + cloud.getId() + "/inventory/refresh", "POST", null, authnToken));
    }

    public ConnectionResponseDTO createConnection(String authnToken, String alias, String description,
                                                  ConnectionCredentials credentials, String providerId,
                                                  String url, String type, AuthType authType
    )
            throws Exception {
        JSONObject json = new JSONObject();
        JSONObject credentialsJSON = new JSONObject();

        credentialsJSON.put("credential", credentials.getCredential());
        credentialsJSON.put("identity", credentials.getIdentity());

        json.put("alias", alias);
        json.put("description", description);
        json.put("credentials", credentialsJSON);
        json.put("providerId", providerId);
        json.put("url", url);
        json.put("type", type);
        json.put("authType", authType);

        return jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
    }

}
