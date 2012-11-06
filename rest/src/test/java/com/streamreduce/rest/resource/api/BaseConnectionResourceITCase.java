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
