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

package com.streamreduce.rest.resource.api;


import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.dao.InventoryItemDAO;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.SecurityService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ResourceBundle;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseConnectionResourceITCase extends AbstractServiceTestCase {

    @Autowired
    ConnectionDAO connectionDAO;
    @Autowired
    InventoryItemDAO inventoryItemDAO;
    @Autowired
    ConnectionResource connectionResource;
    @Autowired
    ConnectionService connectionService;

    ResourceBundle cloudProperties = ResourceBundle.getBundle("cloud");
    ResourceBundle webhdfsProperties = ResourceBundle.getBundle("webhdfs");

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityService testUserSecurityService = mock(SecurityService.class);

        when(testUserSecurityService.getCurrentUser()).thenReturn(testUser);

        ReflectionTestUtils.setField(connectionResource, "securityService", testUserSecurityService);
    }

    String getTestBucketName() {
        return String.format("com.streamreduce.s3test-%s", UUID.randomUUID().toString());
    }
}
