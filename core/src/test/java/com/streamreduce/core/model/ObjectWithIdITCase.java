package com.streamreduce.core.model;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.test.service.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Extension of {@link AbstractServiceTestCase} that will test that
 * {@link ObjectWithId} works as expected.
 */
public class ObjectWithIdITCase extends AbstractServiceTestCase {

    @Autowired
    private ConnectionService connectionService;

    @Test
    public void testObjectVersioning() throws Exception {
        Connection testConnection = TestUtils.createFeedConnectionWithSpecificOutboundDatatypes();
        testConnection.setAccount(testAccount);
        testConnection.setUser(testUser);


        // Initial version is 0
        Assert.assertEquals(0, testConnection.getVersion());

        connectionService.createConnection(testConnection);

        // After creation, version is 1
        Assert.assertEquals(1, testConnection.getVersion());

        testConnection.setDescription("Updated description.");

        connectionService.updateConnection(testConnection);

        // After non-silent update, version is 2
        Assert.assertEquals(2, testConnection.getVersion());

        testConnection.setLastActivityPollDate(new Date());

        connectionService.updateConnection(testConnection, true);

        // After silent update, version should still be 2
        Assert.assertEquals(2, testConnection.getVersion());
    }
}
