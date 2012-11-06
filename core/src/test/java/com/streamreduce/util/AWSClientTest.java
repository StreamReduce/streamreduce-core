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
