package com.streamreduce.datasource;

import com.mongodb.BasicDBObject;
import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.Constants;
import com.streamreduce.connections.CloudProvider;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.rest.dto.response.ConnectionInventoryResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.InventoryItemResponseDTO;

import java.util.List;

import junit.framework.Assert;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests that {@link com.streamreduce.datasource.BootstrapDatabaseDataPopulator} works as expected.
 */
public class BootstrapDatabaseDataPopulatorITCase extends AbstractInContainerTestCase {

    private Account rootAccount = null;
    private Account integrationsAccount = null;

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private InventoryService inventoryService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        for (Account account : userService.getAccounts()) {
            if (account.getName().equals(Constants.NODEABLE_SUPER_ACCOUNT_NAME)) {
                rootAccount = account;
            }
            if (rootAccount != null) {
                break;
            }
        }
    }

    /**
     * Make sure the proper accounts and users have been bootstrapped.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testAccountsAndUsersCreatedProperly() throws Exception {
        Assert.assertNotNull(rootAccount);
        Assert.assertNotNull(integrationsAccount);

        User rootUser = null;

        for (User user : userService.allUsersForAccount(rootAccount)) {
            if (user.getUsername().equals(Constants.NODEABLE_SUPER_USERNAME)) {
                rootUser = user;
            }
            if (rootUser != null) {
                break;
            }
        }


        Assert.assertNotNull(rootUser);
    }


    /**
     * Make sure connections and inventory items that are public do not leak sensitive information.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore
    public void testForSecurityLeaks() throws Exception {
        // NOTE: This could be put elsewhere but since it was written as part of SOBA-1855, here it sits for now

        String authnToken = login(testUsername, testUsername);
        List<ConnectionResponseDTO> allConnections =
                jsonToObject(makeRequest(connectionsBaseUrl, "GET", null, authnToken),
                        TypeFactory.defaultInstance().constructCollectionType(List.class,
                                ConnectionResponseDTO.class));
        String awsAccessKeyId = cloudProperties.getString("nodeable.integrations.aws.accessKeyId");
        String awsSecretKey = cloudProperties.getString("nodeable.integrations.aws.secretKey");

        Assert.assertEquals(26, allConnections.size());

        for (ConnectionResponseDTO connection : allConnections) {
            // Make sure public connections do not have the connection credentials in them
            Assert.assertFalse(connection.isOwner());
            Assert.assertNull(connection.getIdentity());

            // Only cloud inventory items can be leaked so let's filter our inventory items
            if (connection.getType().equals(CloudProvider.TYPE)) {
                inventoryService.refreshInventoryItemCache(connectionService.getConnection(connection.getId()));

                List<InventoryItem> rawInventoryItems = inventoryService.getInventoryItems(connection.getId());
                int retry = 0;

                while (rawInventoryItems.size() == 0 && retry < 3) {
                    Thread.sleep(30000);
                    rawInventoryItems = inventoryService.getInventoryItems(connection.getId());
                    retry++;
                }

                if (rawInventoryItems.size() == 0) {
                    throw new Exception("Unable to prepare for the test so tests are unable to run.");
                }

                // Make sure public inventory items do not have anything sensitive in them
                String rawResponse = makeRequest(connectionsBaseUrl + "/" + connection.getId() + "/inventory", "GET",
                        null, authnToken);
                ConnectionInventoryResponseDTO responseDTO =
                        jsonToObject(rawResponse,
                                TypeFactory.defaultInstance().constructType(ConnectionInventoryResponseDTO.class));

                for (InventoryItemResponseDTO inventoryItem : responseDTO.getInventoryItems()) {
                    BasicDBObject payload = inventoryItem.getPayload();

                    Assert.assertFalse(JSONObject.fromObject(payload).toString().contains(awsAccessKeyId));
                    Assert.assertFalse(JSONObject.fromObject(payload).toString().contains(awsSecretKey));
                    Assert.assertFalse(payload.containsField("adminPassword"));
                    Assert.assertFalse(payload.containsField("credentials"));
                }

            }
        }
    }

}
