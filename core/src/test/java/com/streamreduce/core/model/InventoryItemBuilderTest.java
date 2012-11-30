package com.streamreduce.core.model;

import com.streamreduce.test.service.TestUtils;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class InventoryItemBuilderTest {

    InventoryItem.Builder builderUnderTest;

    @Before
    public void setUp() {
        builderUnderTest = new InventoryItem.Builder()
                .user(mock(User.class))
                .account(mock(Account.class));

    }

    @Test
    public void testBuilder_happyPath() {
        Connection c = TestUtils.createCloudConnection();
        ObjectId metaDataObjectId = new ObjectId();

        InventoryItem inventoryItem = builderUnderTest.connection(c)
                .type("someType")
                .metadataId(metaDataObjectId)
                .build();

        assertEquals(c, inventoryItem.getConnection());
        assertEquals("someType", inventoryItem.getType());
        assertEquals(metaDataObjectId, inventoryItem.getMetadataId());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_nullConnection() {
        InventoryItem inventoryItem = builderUnderTest
                .type("someType")
                .metadataId(new ObjectId())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_nullType() {
        InventoryItem inventoryItem = builderUnderTest
                .connection(TestUtils.createCloudConnection())
                .metadataId(new ObjectId())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_emptyType() {
        InventoryItem inventoryItem = builderUnderTest
                .connection(TestUtils.createCloudConnection())
                .type("  ")
                .metadataId(new ObjectId())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_nullMetadataId() {
        InventoryItem inventoryItem = builderUnderTest
                .connection(TestUtils.createCloudConnection())
                .type("someType")
                .build();
    }
}
