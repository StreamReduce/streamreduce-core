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

package com.streamreduce.test.service;


import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.streamreduce.core.model.ConnectionCredentials;
import org.apache.commons.lang.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.s3.blobstore.S3BlobStore;
import org.jclouds.s3.blobstore.S3BlobStoreContext;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class S3TestUtils {

    private S3BlobStore s3BlobStore;

    //TODO: does all of this behavior belong on AWSClient?

    public S3TestUtils(ConnectionCredentials connectionCredentials) {
        S3BlobStoreContext blobStoreContext = ContextBuilder.newBuilder("aws-s3")
                .credentials(connectionCredentials.getIdentity(), connectionCredentials.getCredential())
                .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()))
                .buildView(S3BlobStoreContext.class);
        s3BlobStore = blobStoreContext.getBlobStore();
    }

    /**
     * Removes all buckets whose name starts with the passed in prefix.  If an empty prefix is passed in, no buckets
     * are removed.
     * @param bucketNamePrefix - The bucket name prefix.
     * @return The number of buckets removed
     */
    public int removeBuckets(String bucketNamePrefix) {
        if (StringUtils.isEmpty(bucketNamePrefix)) { return 0; }
        int count = 0;
        for (StorageMetadata metadata : s3BlobStore.list()) {
            String bucketName = metadata.getName();
            if (s3BlobStore.containerExists(bucketName) && bucketName.startsWith(bucketNamePrefix)) {
                s3BlobStore.deleteContainer(bucketName);
                count++;
            }
        }
        return count;
    }

    public Blob getFirstBlobFromS3ThatMatchesPrefix(String expectedBucketName, String prefix) {
        Assert.assertTrue("Test failed - bucket wasn't created when it should have been automatically created.",
                s3BlobStore.containerExists(expectedBucketName));

        List<Blob> blobs = getBlobsFromS3(expectedBucketName,prefix);
        Assert.assertEquals(1,blobs.size());

        return blobs.get(0);
    }

    public List<Blob> getBlobsFromS3(String expectedBucketName, String prefix) {
        if (!s3BlobStore.containerExists(expectedBucketName)) {
            return Collections.emptyList();
        }

        PageSet<? extends StorageMetadata> pageSet = s3BlobStore.list(expectedBucketName,
                ListContainerOptions.Builder.afterMarker(prefix));

        List<Blob> blobs = new ArrayList<>();
        for (StorageMetadata metadata : pageSet) {
            blobs.add(
                    s3BlobStore.getBlob(expectedBucketName, metadata.getName())
            );
        }
        return blobs;
    }

    public Blob getExpectedBlob(String bucket, String key) {
        return s3BlobStore.getBlob(bucket,key);
    }

}
