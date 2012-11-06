package com.streamreduce.util;

import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.OutboundConfiguration;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AWSClientTest {

    AWSClient awsClientUnderTest;

    @Before
    public void setUp() {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.getProviderId()).thenReturn(ProviderIdConstants.AWS_PROVIDER_ID);
        awsClientUnderTest = new AWSClient(mockConnection);
    }


    @Test
    public void testConvertOutboundConnectionToBucketName_UsesNamespaceAndIsLowerCase() throws Exception {
        OutboundConfiguration mockOutboundConfiguration = mock(OutboundConfiguration.class);
        when(mockOutboundConfiguration.getNamespace()).thenReturn("Foo");
        String bucketName = awsClientUnderTest.convertOutboundConnectionToBucketName(mockOutboundConfiguration);
        assertEquals("foo",bucketName);
    }

    @Test
    public void testConvertOutboundConnectionToBucketName_UsesAccountId() throws Exception {
        ObjectId accountId = new ObjectId();
        Account acct = new Account.Builder().name("testacct").build();
        acct.setId(accountId);
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.getAccount()).thenReturn(acct);

        OutboundConfiguration mockOutboundConfiguration = mock(OutboundConfiguration.class);
        when(mockOutboundConfiguration.getNamespace()).thenReturn(null);
        when(mockOutboundConfiguration.getOriginatingConnection()).thenReturn(mockConnection);

        String bucketName = awsClientUnderTest.convertOutboundConnectionToBucketName(mockOutboundConfiguration);
        String expected = "com.streamreduce." + accountId.toString();
        assertEquals(expected,bucketName);
    }
}
