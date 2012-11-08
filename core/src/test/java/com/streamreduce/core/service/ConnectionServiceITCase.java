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

package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testListAllConnections() throws Exception {

        Account nAccount = testUser.getAccount();
        List<Connection> nConnections = connectionService.getConnections(null, testUser);


        for (Connection connection : nConnections) {
            Assert.assertTrue(connection.getAccount().getId().equals(nAccount.getId()));
        }
    }

    @Test(expected = ConnectionNotFoundException.class)
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testGetConnectionThrowsConnectionNotFoundForNewObjectId() throws ConnectionNotFoundException {
        connectionService.getConnection(new ObjectId());
    }


}
