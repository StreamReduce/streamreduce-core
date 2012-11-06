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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.rest.resource.ErrorMessage;
import com.streamreduce.rest.resource.gateway.GatewayResource;
import com.streamreduce.test.service.S3TestUtils;
import com.streamreduce.test.service.TestUtils;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jclouds.blobstore.domain.Blob;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class OutboundStorageServiceRawToS3IT extends AbstractServiceTestCase {

    @Autowired
    GatewayResource gatewayResource;
    @Autowired
    OutboundStorageService outboundStorageService;

    S3TestUtils s3TestUtils;


    @After
    public void tearDown() throws Exception {
        try {
            if (s3TestUtils != null) {
                s3TestUtils.removeBuckets("com.streamreduce");
            }
        }
        catch (Exception e) {
            logger.error("unable to delete buckets", e);
        }
        super.tearDown();
    }

    @Test
    public void testSendRawMessageAppearsInS3() throws Exception {
        Connection testIMGConnection = createAndMockForIMGConnectionWithOutboundTypes(OutboundDataType.RAW);
        testIMGConnection.setUser(testUser);
        testIMGConnection.setAccount(testAccount);
        testIMGConnection.setId(null);
        connectionService.createConnection(testIMGConnection);

        JSONObject imgPayload  = TestUtils.createValidSampleIMGPayload();
        Response response = gatewayResource.createCustomConnectionMessage(imgPayload); //magic happens here

        //Start verification
        if (response.getStatus() != 201) {
            ErrorMessage e = (ErrorMessage) response.getEntity();
            Assert.fail("Unable to verify test due to IMG failure.  Response has a status of " + response.getStatus() +
                    " and an error message of : " + e.getErrorMessage());
        }

        //Give enough time for this to trickle to SQS and back to server
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        ConnectionCredentials s3Creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(s3Creds);

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String prefix = "raw/" + testIMGConnection.getId() + "/";
        Blob payload = s3TestUtils.getFirstBlobFromS3ThatMatchesPrefix(expectedBucketName, prefix);

        JSONObject jsonPayload = JSONObject.fromObject(IOUtils.toString(payload.getPayload().getInput()));
        Assert.assertEquals(imgPayload,jsonPayload);
    }

    @Test
    public void testSendRawMessageDoesNotAppearWithoutRawOutboundDataTypeInS3() throws Exception {
        Connection testIMGConnection = createAndMockForIMGConnectionWithOutboundTypes(
                OutboundDataType.EVENT, OutboundDataType.PROCESSED, OutboundDataType.INSIGHT);


        JSONObject imgPayload  = TestUtils.createValidSampleIMGPayload();
        Response response = gatewayResource.createCustomConnectionMessage(imgPayload); //magic happens here

        //Start verification
        if (response.getStatus() != 201) {
            ErrorMessage e = (ErrorMessage) response.getEntity();
            Assert.fail("Unable to verify test due to IMG failure.  Response has a status of " + response.getStatus() +
                    " and an error message of : " + e.getErrorMessage());
        }

        ConnectionCredentials s3Creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(s3Creds);

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String prefix = "raw/" + testIMGConnection.getId() + "/";
        List<Blob> blobs = s3TestUtils.getBlobsFromS3(expectedBucketName,prefix);

        Assert.assertEquals(0,blobs.size());
    }

    private Connection createAndMockForIMGConnectionWithOutboundTypes(OutboundDataType... dataTypes)  {
        Connection testIMGConnection = TestUtils.createIMGConnectionWithSpecificOutboundDatatypes(dataTypes);

        SecurityService mockSecurityService = mock(SecurityService.class);
        when(mockSecurityService.getCurrentGatewayConnection()).thenReturn(testIMGConnection);
        ReflectionTestUtils.setField(gatewayResource,"securityService",mockSecurityService);

        return testIMGConnection;
    }
}
