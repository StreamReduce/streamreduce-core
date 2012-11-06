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


import com.google.common.collect.Sets;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.test.service.TestUtils;

import java.util.HashSet;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class OutboundStorageServiceImplTest {

    @Test
    public void testSendRawMessageNullPayload() throws Exception {
        //Tests that sending a Null payload as a raw message does not write to any outbound connections.
        Connection connection = TestUtils.createFeedConnectionWithSpecificOutboundDatatypes(OutboundDataType.RAW);
        OutboundStorageServiceImpl outboundStorageService = new OutboundStorageServiceImpl();
        Assert.assertEquals(0,outboundStorageService.sendRawMessage(null, connection));
    }

    @Test
    public void testSendRawMessage_noOutbound() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(connection.getAccount()).thenReturn(account);
        HashSet<OutboundConfiguration> outboundConfigurations = Sets.newHashSet(Mockito.mock(OutboundConfiguration.class));
        Mockito.when(connection.getOutboundConfigurations()).thenReturn(outboundConfigurations);

        OutboundStorageServiceImpl outboundStorageService = new OutboundStorageServiceImpl();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "foo");
        Assert.assertEquals(0,outboundStorageService.sendRawMessage(jsonObject, connection));
    }

    @Test
    public void testSendRawMessage_toConnectionWithNoOutboundConfigs() throws Exception {
        //sendRawMessage should return 0 for the number of messages that were actually sent.
        Connection connection = TestUtils.createFeedConnectionWithSpecificOutboundDatatypes();
        OutboundStorageServiceImpl outboundStorageService = new OutboundStorageServiceImpl();
        Assert.assertEquals(0,outboundStorageService.sendRawMessage(TestUtils.createValidSampleIMGPayload(), connection));
    }

    @Test
    public void testSendRawMessageConnectionRawNotInOutbounds() throws Exception {
        //Tests that sending a Null payload as a raw message does not write to any outbound connections.
        Connection connection = TestUtils.createFeedConnectionWithSpecificOutboundDatatypes(OutboundDataType.PROCESSED);
        OutboundStorageServiceImpl outboundStorageService = new OutboundStorageServiceImpl();
        Assert.assertEquals(0,outboundStorageService.sendRawMessage(TestUtils.createValidSampleIMGPayload(), connection));
    }
}
