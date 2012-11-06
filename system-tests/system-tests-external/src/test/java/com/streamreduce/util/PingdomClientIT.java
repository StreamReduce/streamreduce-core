package com.streamreduce.util;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/25/12 20:54</p>
 */
public class PingdomClientIT {

    private ResourceBundle pingdomProperties = ResourceBundle.getBundle("pingdom");

    private Account testAccount = new Account.Builder()
            .url("http://nodeable.com")
            .description("Nodeable Test Account")
            .name("Nodeable Testing")
            .build();
    private User testUser = new User.Builder()
            .account(testAccount)
            .accountLocked(false)
            .accountOriginator(true)
            .fullname("Nodeable Test User")
            .username("test_user_" + new Date().getTime() + "@nodeable.com")
            .build();

    @Test
    public void testValidateConnection() throws Exception {
        PingdomClient client = new PingdomClient(buildConnection(
                pingdomProperties.getString("nodeable.integrations.pingdom.identity"),
                pingdomProperties.getString("nodeable.integrations.pingdom.credential"),
                pingdomProperties.getString("nodeable.integrations.pingdom.apikey")));
        client.validateConnection();
    }

    @Test
    public void testValidateConnection_badCreds() throws Exception {
        PingdomClient client = new PingdomClient(buildConnection(
                "whoopwhoop@nodeable.com",
                pingdomProperties.getString("nodeable.integrations.pingdom.credential"),
                pingdomProperties.getString("nodeable.integrations.pingdom.apikey")));
        try {
            client.validateConnection();
            Assert.fail("Should've encountered an InvalidCredentialsException.");
        }
        catch (InvalidCredentialsException ice) {
            Assert.assertEquals("Invalid email and/or password", ice.getMessage());
        }
    }

    @Test
    public void testValidateConnection_badApiKey() throws Exception {
        PingdomClient client = new PingdomClient(buildConnection(
                pingdomProperties.getString("nodeable.integrations.pingdom.identity"),
                pingdomProperties.getString("nodeable.integrations.pingdom.credential"),
                "whoopwhoop"));
        try {
            client.validateConnection();
            Assert.fail("Should've encountered an InvalidCredentialsException.");
        }
        catch (InvalidCredentialsException ice) {
            Assert.assertEquals("Invalid application key", ice.getMessage());
        }
    }

    @Test
    public void testChecks() throws Exception {
        PingdomClient client = new PingdomClient(buildConnection(
                pingdomProperties.getString("nodeable.integrations.pingdom.identity"),
                pingdomProperties.getString("nodeable.integrations.pingdom.credential"),
                pingdomProperties.getString("nodeable.integrations.pingdom.apikey")));
        List<JSONObject> inventory = client.checks ();
        Assert.assertNotNull(inventory);
        Assert.assertEquals(inventory.size(), 1);
    }

    private Connection buildConnection(String username, String password, String apiKey) {
        return new Connection.Builder()
                .alias("PingdomClientIT Connection")
                .account(testAccount)
                .user(testUser)
                .authType(AuthType.USERNAME_PASSWORD_WITH_API_KEY)
                .credentials(new ConnectionCredentials(username, password, apiKey))
                .url(PingdomClient.PINGDOM_URL)
                .provider(ConnectionProvidersForTests.PINGDOM_PROVIDER)
                .build();
    }
}
