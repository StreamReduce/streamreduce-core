package com.streamreduce.core.dao;

import com.streamreduce.AbstractDAOTest;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.test.service.TestUtils;
import com.streamreduce.util.JSONObjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class InventoryItemDAOIT extends AbstractDAOTest {

    static final String SAMPLE_EXTERNAL_ID = "ABC-DEF-123456789";

    @Autowired
    private InventoryItemDAO inventoryItemDAO;
    @Autowired
    private ConnectionDAO connectionDAO;
    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private InventoryService inventoryService;

    private Account testAccount;
    private Connection testConnection;

    @Before
    public void setUp() throws Exception {
        testConnection = TestUtils.createTestFeedConnection();
        testConnection.setExternalId("ABC-DEF-123456789");

        //fake InventoryService out by giving it a custom provider type, which allows inventory to be created
        testConnection.setProviderId(ProviderIdConstants.CUSTOM_PROVIDER_ID);
        testAccount = testConnection.getAccount();
        accountDAO.save(testAccount);
        userDAO.save(testConnection.getUser());
        connectionDAO.save(testConnection);
        inventoryService.createInventoryItem(testConnection, new JSONObjectBuilder().add("inventoryItemId",SAMPLE_EXTERNAL_ID).build());

    }

    @Test
    public void testForAccount() throws Exception  {
        List<InventoryItem> inventoryItems = inventoryItemDAO.forAccount(testAccount);
        assertEquals(1, inventoryItems.size());
    }

    @Test
    public void testGetByExternalId() {
        List<InventoryItem> inventoryItems = inventoryItemDAO.getByExternalId(SAMPLE_EXTERNAL_ID);
        assertEquals(1, inventoryItems.size());
    }

    @Test
    public void testGetByExternalIdNotDeleted() throws Exception {
        InventoryItem deletedItem = inventoryService.createInventoryItem(testConnection,
                new JSONObjectBuilder().add("inventoryItemId",SAMPLE_EXTERNAL_ID).build());
        inventoryService.markInventoryItemDeleted(deletedItem);

        List<InventoryItem> inventoryItems = inventoryItemDAO.getByExternalId(SAMPLE_EXTERNAL_ID);
        assertEquals(2, inventoryItems.size());

        List<InventoryItem> inventoryItemsNotDeleted = inventoryItemDAO.getByExternalIdNotDeleted(SAMPLE_EXTERNAL_ID);
        assertEquals(1, inventoryItemsNotDeleted.size());


    }
}
