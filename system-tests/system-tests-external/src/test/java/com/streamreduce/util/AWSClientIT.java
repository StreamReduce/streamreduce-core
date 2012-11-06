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

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.User;
import net.sf.json.JSONObject;
import org.jclouds.aws.domain.Region;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.compute.domain.ComputeType;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.ResourceBundle;

/**
 * Tests for {@link AWSClient}.
 */
public class AWSClientIT {

    private ResourceBundle cloudProperties = ResourceBundle.getBundle("cloud");
    private String awsAccessKeyId = cloudProperties.getString("nodeable.aws.accessKeyId");
    private String awsSecretKey = cloudProperties.getString("nodeable.aws.secretKey");
    private Connection connection;
    AWSClient awsClient;

    @Before
    public void setUp() throws Exception {
        Account testAccount = new Account.Builder()
                .url("http://nodeable.com")
                .description("Nodeable Test Account")
                .name("Nodeable Testing")
                .build();
        User testUser = new User.Builder()
                .account(testAccount)
                .accountLocked(false)
                .accountOriginator(true)
                .fullname("Nodeable Test User")
                .username("test_user_" + new Date().getTime() + "@nodeable.com")
                .build();

        connection = new Connection.Builder()
                .provider(ConnectionProvidersForTests.AWS_CLOUD_PROVIDER)
                .credentials(new ConnectionCredentials(awsAccessKeyId, awsSecretKey))
                .alias("Test AWS Connection")
                .user(testUser)
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        awsClient = new AWSClient(connection);
        try {
            awsClient.validateConnection();
        }
        catch (Exception e) {
            Assert.fail("Failed to validate the AWS connection");
        }
    }

    /**
     * Simple test that ensures that getting the AWS EC2 instances works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetEC2Instances() throws Exception {
        for (JSONObject instance : awsClient.getEC2Instances()) {
            Assert.assertEquals(ComputeType.NODE.toString(), instance.getString("type"));
        }
    }

    /**
     * Simple test that ensures that getting the AWS S3 buckets works as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetS3Buckets() throws Exception {
        for (JSONObject bucket : awsClient.getS3BucketsAsJson()) {
            Assert.assertEquals(StorageType.CONTAINER.toString(), bucket.getString("type"));
        }
    }

    /**
     * Tests that S3's eu-west-1 support in jclouds works as expected.
     *
     * @see <a href="https://nodeable.jira.com/browse/SOBA-1876" />
     * @see <a href="http://code.google.com/p/jclouds/issues/detail?id=1008" />
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testBucketsInEURegion() throws Exception {
        BlobStore store = awsClient.getBlobStoreContext().getBlobStore();
        LocationBuilder lb = new LocationBuilder();
        String bucketName = "euregiontest" + new Date().getTime();
        StorageMetadata bucket = null;

        try {
            lb.id(Region.EU_WEST_1)
              .scope(LocationScope.REGION)
              .description("Superfluous description to appease jclouds.");

            store.createContainerInLocation(lb.build(), bucketName);

            for (StorageMetadata sm : store.list()) {
                if (sm.getName().equals(bucketName)) {
                    bucket = sm;
                    break;
                }
            }
        } finally {
            store.deleteContainer(bucketName);
        }

        Assert.assertNotNull(bucket);
    }

}
