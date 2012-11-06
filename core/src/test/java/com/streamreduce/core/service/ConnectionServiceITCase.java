package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;


public class ConnectionServiceITCase extends AbstractServiceTestCase {

    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;
    @Mock
    MessageService messageService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(connectionService, "messageService", messageService);
    }

    @Test
    public void testListAllConnections() throws Exception {

        Account nAccount = testUser.getAccount();
        List<Connection> nConnections = connectionService.getConnections(null, testUser);


        for (Connection connection : nConnections) {
            Assert.assertTrue(connection.getAccount().getId().equals(nAccount.getId()));
        }
    }

    @Test(expected = ConnectionNotFoundException.class)
    public void testGetConnectionThrowsConnectionNotFoundForNewObjectId() throws ConnectionNotFoundException {
        connectionService.getConnection(new ObjectId());
    }


}
