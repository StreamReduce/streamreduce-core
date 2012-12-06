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
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.test.service.S3TestUtils;
import com.streamreduce.test.service.TestUtils;
import com.streamreduce.util.AWSClient;
import com.streamreduce.util.InvalidOutboundConfigurationException;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ConnectionServiceValidateS3OutboundConfigITCase extends AbstractServiceTestCase {

    @Autowired
    ConnectionService connectionService;

    Connection awsConnection;
    S3TestUtils s3TestUtils;
    final String bucketName = "com.streamreduce.duplicated.bucket.test";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        awsConnection = TestUtils.createCloudConnection();
        s3TestUtils = new S3TestUtils(awsConnection.getCredentials());
        s3TestUtils.removeBuckets(bucketName);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

    }

    @Test
    public void testCreateConnection_s3BucketIsCreatedProactivelyForS3OutboundConfig() throws Exception {
        //Test that when a Connection is created with an OutboundConfiguration with s3 as the protocol, that the
        //bucket is created or previously exists, and can be written to by

        //Just to make sure the test doesn't error out because of a duplicate bucket existing
        String actualBucketName = bucketName + String.valueOf(System.currentTimeMillis());

        awsConnection.setOutboundConfigurations(Sets.newHashSet(
                new OutboundConfiguration.Builder()
                        .protocol("s3")
                        .credentials(awsConnection.getCredentials())
                        .dataTypes(OutboundDataType.PROCESSED)
                        .namespace(actualBucketName)
                        .build()
                )
        );
        awsConnection.setAccount(testAccount);
        awsConnection.setUser(testUser);

        connectionService.createConnection(awsConnection);

        AWSClient awsClient = new AWSClient(awsConnection);
        Set<? extends StorageMetadata> buckets = awsClient.getS3Buckets();
        Set<String> bucketNames = new HashSet<>();
        for (StorageMetadata bucket : buckets) {
            bucketNames.add(bucket.getName());
        }
        assertTrue(bucketNames.contains(actualBucketName));
    }

    @Test(expected = InvalidOutboundConfigurationException.class)
    public void testCreateConnection_failsValidationIfS3OutboundBucketExists() throws Exception {
        //Test that exercises that a connection will not be created if an OutboundConfiguration that has the same
        //bucket name with a set of AWS credentials not in the same AWS account
        createPreExistingBucket();

        //AWS creds for jason@nodeable.com, but Dave gets billed so feel free to use these for fun and profit
        String anotherAccessKey = "AKIAJYE3KRM4KGQ43OKQ";
        String anotherSecretKey = "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";

        //Reuse the same connection but do enough to make it look like a new, non-duplicate connection
        awsConnection.setId(null);
        awsConnection.setUser(testUser);
        awsConnection.setAccount(testUser.getAccount());
        awsConnection.setCredentials(new ConnectionCredentials(anotherAccessKey, anotherSecretKey));
        awsConnection.setOutboundConfigurations(Sets.newHashSet(new OutboundConfiguration.Builder()
                .protocol("s3")
                .dataTypes(OutboundDataType.PROCESSED)
                .namespace(bucketName)
                .credentials(new ConnectionCredentials(anotherAccessKey, anotherSecretKey))
                .build()));

        //Saving this connection should fail because the outboundConfiguration is invalid
        connectionService.createConnection(awsConnection);
    }




    @Test(expected = InvalidOutboundConfigurationException.class)
    public void testUpdateConnection_failsValidationIfS3OutboundBucketExists() throws Exception {
        createPreExistingBucket();

        awsConnection.setAccount(testAccount);
        awsConnection.setUser(testUser);
        Connection createdConnection = connectionService.createConnection(awsConnection);

        //AWS creds for jason@nodeable.com, but Dave gets billed so feel free to use these for fun and profit
        String anotherAccessKey = "AKIAJYE3KRM4KGQ43OKQ";
        String anotherSecretKey = "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";
        createdConnection.addOutboundConfiguration(new OutboundConfiguration.Builder()
                .protocol("s3")
                .dataTypes(OutboundDataType.PROCESSED)
                .namespace(bucketName)
                .credentials(new ConnectionCredentials(anotherAccessKey, anotherSecretKey))
                .build());
        connectionService.updateConnection(createdConnection);
    }

    private void createPreExistingBucket() throws InvalidCredentialsException {
        OutboundConfiguration preexistingOutboundConfig = new OutboundConfiguration.Builder()
                .protocol("s3")
                .credentials(awsConnection.getCredentials())
                .dataTypes(OutboundDataType.PROCESSED)
                .namespace(bucketName)
                .build();

        //Do something that makes the bucket appear
        AWSClient awsClient = new AWSClient(preexistingOutboundConfig);
        awsClient.createBucket(preexistingOutboundConfig);
    }
}
