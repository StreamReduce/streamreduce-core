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


import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.test.service.S3TestUtils;
import com.streamreduce.test.service.TestUtils;
import com.streamreduce.util.AWSClient;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.jclouds.blobstore.domain.Blob;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OutboundStorageSobaMessageToS3IT extends AbstractServiceTestCase {

    @Autowired
    OutboundStorageService outboundStorageService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    InventoryService inventoryService;

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
    public void testSendProcessedMessageAppearsInS3() throws Exception {
        JSONObject imgPayload = TestUtils.createValidSampleIMGPayload();
        Connection testIMGConnection = TestUtils.createIMGConnectionWithSpecificOutboundDatatypes(OutboundDataType.PROCESSED);
        testIMGConnection.setAccount(testAccount);
        testIMGConnection.setUser(testUser);
        testIMGConnection = connectionService.createConnection(testIMGConnection);
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .connection(testIMGConnection)
                .dateGenerated(System.currentTimeMillis())
                .hashtags(Sets.newHashSet("#foo"))
                .sender(testIMGConnection)
                .transformedMessage(imgPayload.getString("message"))
                .type(MessageType.GATEWAY)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundStorageService.sendSobaMessage(sobaMessage, testIMGConnection);
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String key = "processed/" + sobaMessage.getConnectionId() + "/" + sobaMessage.getId();

        ConnectionCredentials creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(creds);

        Blob payload = s3TestUtils.getExpectedBlob(expectedBucketName, key);

        //Test that what made it to S3 is the same thing we get when we turn the sobaMessage into a dto
        JSONObject actualJSONPayloadFromS3 = JSONObject.fromObject(IOUtils.toString(payload.getPayload().getInput()));
        StringWriter sw = new StringWriter();
        new ObjectMapper().writeValue(sw, SobaMessageResponseDTO.fromSobaMessage(sobaMessage));
        JSONObject expectedJSONFromOriginalSobaMessage = JSONObject.fromObject(sw.toString());

        Assert.assertEquals(expectedJSONFromOriginalSobaMessage, actualJSONPayloadFromS3);
    }

    @Test
    public void testSendProcessedMessageDoesNotAppearInS3() throws Exception {
        //If our connection is not created to have an outbound datatype of PROCESSED, we shouldn't get
        //placed in S3

        JSONObject imgPayload = TestUtils.createValidSampleIMGPayload();
        Connection testIMGConnection =
                TestUtils.createIMGConnectionWithSpecificOutboundDatatypes(
                        OutboundDataType.RAW, OutboundDataType.INSIGHT, OutboundDataType.EVENT);
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .connection(testIMGConnection)
                .dateGenerated(System.currentTimeMillis())
                .hashtags(Sets.newHashSet("#foo"))
                .sender(testIMGConnection)
                .transformedMessage(imgPayload.getString("message"))
                .type(MessageType.GATEWAY)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundStorageService.sendSobaMessage(sobaMessage, testIMGConnection);

        ConnectionCredentials creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(creds);

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String prefix = "processed/" + sobaMessage.getConnectionId() + "/";
        List<Blob> payloads = s3TestUtils.getBlobsFromS3(expectedBucketName, prefix);

        Assert.assertEquals(0, payloads.size());
    }

    @Test
    public void testSendInsightMessageDoesAppearInS3() throws Exception {
        //Make sure if INSIGHT is an OutboundDataType that we send it to s3 for an SobaMessage with type NODEBELLY

        JSONObject imgPayload = TestUtils.createValidSampleIMGPayload();
        Connection testIMGConnection =
                TestUtils.createIMGConnectionWithSpecificOutboundDatatypes(
                        OutboundDataType.INSIGHT);
        testIMGConnection.setUser(testUser);
        testIMGConnection.setAccount(testAccount);
        testIMGConnection.setId(null);
        connectionService.createConnection(testIMGConnection);

        SobaMessage sobaMessage = new SobaMessage.Builder()
                .connection(testIMGConnection)
                .dateGenerated(System.currentTimeMillis())
                .hashtags(Sets.newHashSet("#foo"))
                .sender(testIMGConnection)
                .transformedMessage(imgPayload.getString("message"))
                .type(MessageType.NODEBELLY) //This is a fakeout... no Nodebelly/Insight Message looks like this.
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundStorageService.sendSobaMessage(sobaMessage, testIMGConnection);
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));     //give this some time to go through the queue

        ConnectionCredentials creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(creds);

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String prefix = "insight/" + sobaMessage.getConnectionId() + "/";
        Blob payload = s3TestUtils.getFirstBlobFromS3ThatMatchesPrefix(expectedBucketName, prefix);

        //Test that what made it to S3 is the same thing we get when we turn the sobaMessage into a dto
        JSONObject actualJSONPayloadFromS3 = JSONObject.fromObject(IOUtils.toString(payload.getPayload().getInput()));
        StringWriter sw = new StringWriter();
        new ObjectMapper().writeValue(sw, SobaMessageResponseDTO.fromSobaMessage(sobaMessage));
        JSONObject expectedJSONFromOriginalSobaMessage = JSONObject.fromObject(sw.toString());

        Assert.assertEquals(expectedJSONFromOriginalSobaMessage, actualJSONPayloadFromS3);
    }

    @Test
    public void testSendInsightMessageDoesNotAppearInS3() throws Exception {
        //If SobaMessage does not have a type of NODEBELLY, it shouldn't be sent as an insight message to
        //outbound

        JSONObject imgPayload = TestUtils.createValidSampleIMGPayload();
        Connection testIMGConnection =
                TestUtils.createIMGConnectionWithSpecificOutboundDatatypes(
                        OutboundDataType.INSIGHT);
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .connection(testIMGConnection)
                .dateGenerated(System.currentTimeMillis())
                .hashtags(Sets.newHashSet("#foo"))
                .sender(testIMGConnection)
                .transformedMessage(imgPayload.getString("message"))
                .type(MessageType.GATEWAY)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundStorageService.sendSobaMessage(sobaMessage, testIMGConnection);

        ConnectionCredentials creds = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(creds);

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String prefix = "insight/" + sobaMessage.getConnectionId() + "/";
        List<Blob> payloads = s3TestUtils.getBlobsFromS3(expectedBucketName, prefix);

        Assert.assertEquals(0, payloads.size());
    }

    @Test
    public void testEndToEndForProcessedMessages() throws Exception {
        Connection feedConnection =
                TestUtils.createFeedConnectionWithSpecificOutboundDatatypes(OutboundDataType.PROCESSED);

        //Let ConnectionService take care of creating the Id
        feedConnection.setId(null);
        //Make sure user/account are set to persisted values on the connections
        feedConnection.setUser(testUser);
        feedConnection.setAccount(testAccount);

        //Make the feed connection have a last_activity_poll from before the feed messages
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String feb282012TimeStamp = Long.toString(sdf.parse("2012-02-28").getTime());
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("last_activity_poll", feb282012TimeStamp);
        feedConnection.setMetadata(metadata);

        //Create the feed connection and fire the job to create messages
        Connection createdFeedConnection = connectionService.createConnection(feedConnection);
        connectionService.fireOneTimeHighPriorityJobForConnection(feedConnection);

        //Chill a few seconds and let the job and outbound service do their work.
        //It's not very fast...
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        s3TestUtils = new S3TestUtils(TestUtils.createConnectionCredentialsForAWS());

        String expectedBucketName = "com.streamreduce." + createdFeedConnection.getAccount().getId();
        String prefix = "processed/" + feedConnection.getId() + "/";

        List<Blob> blobs = s3TestUtils.getBlobsFromS3(expectedBucketName, prefix);
        Assert.assertEquals(3, blobs.size());
    }

    @Test
    public void testSendProcessedMessageAppearsInS3WithDifferentRegion() throws Exception {
        JSONObject imgPayload = TestUtils.createValidSampleIMGPayload();
        Connection testIMGConnection = TestUtils.createIMGConnectionWithSpecificOutboundDatatypes();

        OutboundConfiguration outboundConfiguration = new OutboundConfiguration.Builder()
                .credentials(TestUtils.createCloudConnection().getCredentials())
                .dataTypes(OutboundDataType.PROCESSED)
                .protocol("s3")
                .destination("eu-west-1")
                .build();
        testIMGConnection.setOutboundConfigurations(Sets.newHashSet(outboundConfiguration));
        testIMGConnection.setAccount(testAccount);
        testIMGConnection.setUser(testUser);
        testIMGConnection = connectionService.createConnection(testIMGConnection);


        SobaMessage sobaMessage = new SobaMessage.Builder()
                .connection(testIMGConnection)
                .dateGenerated(System.currentTimeMillis())
                .hashtags(Sets.newHashSet("#foo"))
                .sender(testIMGConnection)
                .transformedMessage(imgPayload.getString("message"))
                .type(MessageType.GATEWAY)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundStorageService.sendSobaMessage(sobaMessage, testIMGConnection);
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        String expectedBucketName = "com.streamreduce." + testIMGConnection.getAccount().getId();
        String key = "processed/" + sobaMessage.getConnectionId() + "/" + sobaMessage.getId();

        ConnectionCredentials credentials = new ArrayList<OutboundConfiguration>(
                testIMGConnection.getOutboundConfigurations()).get(0).getCredentials();
        s3TestUtils = new S3TestUtils(credentials);

        Blob payload = s3TestUtils.getExpectedBlob(expectedBucketName, key);

        //Test that what made it to S3 is the same thing we get when we turn the sobaMessage into a dto
        JSONObject actualJSONPayloadFromS3 = JSONObject.fromObject(IOUtils.toString(payload.getPayload().getInput()));
        StringWriter sw = new StringWriter();
        new ObjectMapper().writeValue(sw, SobaMessageResponseDTO.fromSobaMessage(sobaMessage));
        JSONObject expectedJSONFromOriginalSobaMessage = JSONObject.fromObject(sw.toString());

        Assert.assertEquals(expectedJSONFromOriginalSobaMessage, actualJSONPayloadFromS3);
        final String bucketName = payload.getMetadata().getContainer();

        List<JSONObject> allBucketsAsJSONObjs = new AWSClient(outboundConfiguration).getS3BucketsAsJson();
        JSONObject bucketAsJSON = Iterables.find(allBucketsAsJSONObjs, new Predicate<JSONObject>() {
            @Override
            public boolean apply(@Nullable JSONObject input) {
                return input != null && bucketName.equals(input.getString("name"));
            }
        });
        Assert.assertEquals("eu-west-1",bucketAsJSON.getJSONObject("location").getString("description"));
    }

}
