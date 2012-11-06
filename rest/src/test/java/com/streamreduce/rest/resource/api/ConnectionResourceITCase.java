package com.streamreduce.rest.resource.api;


import com.google.common.collect.Sets;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests behavior of ConnectionResource without going through the REST layer.
 */
public class ConnectionResourceITCase extends BaseConnectionResourceITCase {

    @Test
    @Ignore
    public void testRemoveTag() {
        Connection stubConnection = new Connection.Builder()
                .user(testUser)
                .provider(mock(ConnectionProvider.class))
                .authType(AuthType.NONE)
                .hashtags(Sets.newHashSet("foo", "bar", "baz", "github"))
                .alias("stubConnection")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();

        connectionDAO.save(stubConnection);

        for (int i = 0; i < 100; i++) { // create 100 inventory items
            InventoryItem item = new InventoryItem.Builder()
                    .connection(stubConnection)
                    .hashtags(Sets.newHashSet("#foo", "#bar", "#baz", "#github"))
                    .alias("stubItem" + Integer.toString(i))
                    .description("stubItem:  " + Integer.toString(i))
                    .user(testUser)
                    .type("stub")
                    .visibility(SobaObject.Visibility.ACCOUNT)
                    .metadataId(new ObjectId())
                    .externalId("testKey")
                    .build();

            inventoryItemDAO.save(item);
        }

        long before = System.currentTimeMillis();
        connectionResource.removeTag(stubConnection.getId(), "#baz");
        long after = System.currentTimeMillis();

        long lengthInMillis = after - before;
        Assert.assertTrue(lengthInMillis < TimeUnit.SECONDS.toMillis(1));
        logger.info("Seconds taken to remove tags is " + lengthInMillis / 1000.0);

        Set<String> expectedTags = Sets.newHashSet("#foo", "#bar", "#github");

        Connection updatedStubConnection = connectionDAO.get(stubConnection.getId());
        assertEquals(expectedTags, updatedStubConnection.getHashtags());

        List<InventoryItem> inventoryItems = inventoryItemDAO.getInventoryItems(updatedStubConnection.getId());
        for (InventoryItem updatedItem : inventoryItems) {
            assertEquals(expectedTags, updatedItem.getHashtags());
        }
    }

    @Test
    public void testAddConnectionWithNoOutboundConfiguration() throws Exception {
        JSONObject connectionObject = new JSONObjectBuilder()
                .add("alias", "Feed Connection, Ya'll")
                .add("description", "Feed Connection, Ya'll")
                .add("visibility", "ACCOUNT")
                .add("providerId", "rss")
                .add("type", "feed")
                .add("authType", "NONE")
                .add("url", ConnectionResourceITCase.class.getResource("/com/nodeable/rss/sample_EC2.rss").toString())
                .add("inbound", true)
                .build();

        Response response = connectionResource.createConnection(connectionObject);

        Assert.assertEquals(200, response.getStatus());
    }


}
