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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.*;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.*;
import net.sf.json.xml.XMLSerializer;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2ApiMetadata;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.aws.ec2.domain.Tag;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.aws.ec2.util.TagFilters;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.cloudwatch.CloudWatchAsyncApi;
import org.jclouds.cloudwatch.CloudWatchApi;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.RestContext;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Sets.filter;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.all;

/**
 * AWSClient provides necessary methods for interacting with Amazon Web Services.
 */
public class AWSClient extends ExternalIntegrationClient {
    private static final String BUCKET_PREFIX = "com.streamreduce";
    private static final String S3_OBJECT_EVENT_PREFIX = "event/";
    private static final String S3_OBJECT_SOBA_MESSAGE_PREFIX = "message/";

    private BlobStoreContext blobStoreContext;
    private ComputeServiceContext computeServiceContext;
    private RestContext<CloudWatchApi, CloudWatchAsyncApi> cloudWatchContext;

    private static final String EC2_API_BASE = "ec2.amazonaws.com";

    public AWSClient(Connection connection) {
        super(connection);
        Assert.isTrue(connection.getProviderId().equals(ProviderIdConstants.AWS_PROVIDER_ID));
    }

    public AWSClient(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
        Assert.isTrue(outboundConfiguration.getProtocol().equals("s3"));
    }

    /**
     * Destroys a compute instance and returns the result.
     *
     * @param nodeId the node id to destroy
     *
     * @return the success of the destroy attempt
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    public boolean destroyEC2Instance(String nodeId) throws InvalidCredentialsException {
        getComputeServiceContext().getComputeService().destroyNode(nodeId);

        NodeMetadata nodeMetadata = getEC2Instance(nodeId);

        return new RetryablePredicate<NodeMetadata>(NodePredicates.RUNNING, 30, 5,
                                                    TimeUnit.SECONDS).apply(nodeMetadata);
    }

    /**
     * Reboots the instance and returns the result.
     *
     * @param nodeId the node id to reboot
     *
     * @return the success of the reboot attempt
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    public boolean rebootEC2Instance(String nodeId) throws InvalidCredentialsException {
        getComputeServiceContext().getComputeService().rebootNode(nodeId);

        NodeMetadata nodeMetadata = getEC2Instance(nodeId);

        return new RetryablePredicate<NodeMetadata>(NodePredicates.RUNNING, 30, 5,
                                                    TimeUnit.SECONDS).apply(nodeMetadata);
    }

    /**
     * Returns the node metadata for the given node id.
     *
     * @param nodeId the jclouds node id
     *
     * @return the node metadata
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    public NodeMetadata getEC2Instance(String nodeId) throws InvalidCredentialsException {
        return getComputeServiceContext().getComputeService().getNodeMetadata(nodeId);
    }

    /**
     * Return the {@link Tag}s currently applied to the EC2 instance identified by the node id passed in.
     *
     * @param nodeId the node id of the EC2 instance we're interested in
     *
     * @return set of tag objects.
     *
     * @throws InvalidCredentialsException if the connection's credentials are invalid
     */
    public Set<Tag> getEC2InstanceTags(String nodeId) throws InvalidCredentialsException {
        AWSEC2Client ec2Client = AWSEC2Client.class.cast(getComputeServiceContext()
                                                                 .unwrap(AWSEC2ApiMetadata.CONTEXT_TOKEN).getApi());

        return ec2Client.getTagServices()
                        .describeTagsInRegion(null, TagFilters.filters()
                                                              .resourceType(TagFilters.ResourceType.INSTANCE)
                                                              .resourceId(nodeId).build());
    }

    /**
     * Returns a list of JSONObjects representing the compute nodes available to this AWS connection.
     *
     * @return list of JSONObjects representing the compute nodes
     *
     * @throws InvalidCredentialsException if the client's credentials are invalid
     */
    public List<JSONObject> getEC2Instances() throws InvalidCredentialsException {
        ComputeService computeService = getComputeServiceContext().getComputeService();
        Set<? extends NodeMetadata> rawComputeNodes = filter(
                computeService.listNodesDetailsMatching(all()), not(TERMINATED));
        List<JSONObject> computeNodes = new ArrayList<JSONObject>();

        for (NodeMetadata computeNode : rawComputeNodes) {
            computeNodes.add(JSONObject.fromObject(computeNode));
        }

        return computeNodes;
    }



    public Set<? extends StorageMetadata> getS3Buckets() throws InvalidCredentialsException {
        BlobStore store = getBlobStoreContext().getBlobStore();
        return store.list();
    }

    /**
     * Returns a list of JSONObjects representing the storage blobs available to this AWS connection.
     *
     * @return list of JSONObjects representing the storage blobs
     *
     * @throws InvalidCredentialsException if the client's credentials are invalid
     */
    public List<JSONObject> getS3BucketsAsJson() throws InvalidCredentialsException {
        Set<? extends StorageMetadata> rawBlobs = getS3Buckets();
        List<JSONObject> blobs = new ArrayList<JSONObject>();

        for (StorageMetadata storageMetadata : rawBlobs) {
            blobs.add(JSONObject.fromObject(storageMetadata));
        }

        return blobs;
    }

    public List<JSONObject> describeRegions() throws IOException, InvalidCredentialsException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Action", "DescribeRegions");
        params.put("Version", "2012-08-15");
        JSONObject rawResponse = makeRequest(EC2_API_BASE, params);
        return getJSONChildren("regionInfo", rawResponse);
    }

    public List<JSONObject> describeImages(String endpoint, Set<String> imageIds) throws IOException, InvalidCredentialsException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Action", "DescribeImages");
        params.put("Version", "2012-08-15");
        int count = 0;
        for (String imageId : imageIds) {
            count += 1;
            params.put("ImageId." + count, imageId);
        }
        JSONObject rawResponse = makeRequest(endpoint, params);
        return getJSONChildren("item", rawResponse.getJSONObject("imagesSet"));
    }

    public List<JSONObject> describeInstances(String endpoint) throws IOException, InvalidCredentialsException {
        List<JSONObject> response = new ArrayList<JSONObject>();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("Action", "DescribeInstances");
        params.put("Version", "2012-08-15");
        params.put("Filter.1.Name", "instance-state-name");
        params.put("Filter.1.Value.1", "running");
        params.put("Filter.1.Value.2", "stopped");
        JSONObject rawResponse = makeRequest(endpoint, params);
        for (JSONObject reservation : getJSONChildren("item", rawResponse.getJSONObject("reservationSet"))) {
            response.addAll(getJSONChildren("item", reservation.getJSONObject("instancesSet")));
        }
        return response;
    }

    public List<JSONObject> describeInstanceStatus(String endpoint) throws IOException, InvalidCredentialsException {
        List<JSONObject> response = new ArrayList<JSONObject>();
        HashMap<String, String> params = new HashMap<String, String>();
        String nextToken = null;
        do {
            params.put("Action", "DescribeInstanceStatus");
            params.put("Version", "2012-08-15");
            if (nextToken != null) {
                params.put("NextToken", nextToken);
            }
            JSONObject rawResponse = makeRequest(endpoint, params);
            response.addAll(getJSONChildren("item", rawResponse.getJSONObject("instanceStatusSet")));
            Object obj = rawResponse.get("NextToken");
            nextToken = (String) obj;
        } while (nextToken != null);
        return response;
    }

    public List<JSONObject> listMetrics(String endpoint) throws IOException, InvalidCredentialsException {
        List<JSONObject> response = new ArrayList<JSONObject>();
        String nextToken = null;
        do {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("Action", "ListMetrics");
            params.put("Version", "2010-08-01");
            if (nextToken != null) {
                params.put("NextToken", nextToken);
            }
            JSONObject rawResponse = makeRequest(endpoint, params);
            response.addAll(getJSONChildren("member", rawResponse.getJSONObject("ListMetricsResult").getJSONObject("Metrics")));
            Object obj = rawResponse.getJSONObject("ListMetricsResult").get("NextToken");
            nextToken = (String) obj;
        } while (nextToken != null);
        return response;
    }

    public List<JSONObject> getMetricStatistics(String endpoint, String namespace, String metricName, String dimensionName, String dimensionValue)
            throws IOException, InvalidCredentialsException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Action", "GetMetricStatistics");
        params.put("Version", "2010-08-01");
        params.put("Namespace", namespace);
        params.put("MetricName", metricName);
        params.put("Dimensions.member.1.Name", dimensionName);
        params.put("Dimensions.member.1.Value", dimensionValue);
        params.put("Statistics.member.1", "Average");
        long now = new Date().getTime();
        params.put("StartTime", getFormattedTimestamp(new Date(now - 60 * 60 * 1000)));
        params.put("EndTime", getFormattedTimestamp(new Date(now)));
        params.put("Period", "60");
        JSONObject rawResponse = makeRequest(endpoint, params);
        return getJSONChildren("member", rawResponse.getJSONObject("GetMetricStatisticsResult").getJSONObject("Datapoints"));
    }

    public Map<String, JSONArray> describeInstanceAndImageForRunningAndStoppedInstances(String endpoint) throws IOException, InvalidCredentialsException {
        Map<String, JSONArray> instanceAndImageByInstanceId = new HashMap<String, JSONArray>();
        Map<String, JSONObject> instancesById = new HashMap<String, JSONObject>();
        Map<String, JSONObject> imagesById = new HashMap<String, JSONObject>();
        List<JSONObject> instances = describeInstances(endpoint);
        for (JSONObject instance : instances) {
            String instanceId = instance.getString("instanceId");
            String imageId = instance.getString("imageId");
            instancesById.put(instanceId, instance);
            imagesById.put(imageId, null);
        }
        List<JSONObject> images = describeImages(endpoint, imagesById.keySet());
        for (JSONObject image : images) {
            String imageId = image.getString("imageId");
            imagesById.put(imageId, image);
        }
        for (JSONObject instance : instances) {
            String instanceId = instance.getString("instanceId");
            String imageId = instance.getString("imageId");
            JSONArray arr = new JSONArray();
            arr.element(instancesById.get(instanceId));
            arr.element(imagesById.get(imageId));
            instanceAndImageByInstanceId.put(instanceId, arr);
        }
        return instanceAndImageByInstanceId;
    }

    public Map<String, JSONObject> getMetricsStatisticsForRunningInstances(String endpoint) throws IOException, InvalidCredentialsException {
        Map<String, JSONObject> statisticsByInstance = new HashMap<String, JSONObject>();
        List<JSONObject> instances = describeInstances(endpoint);
        List<JSONObject> metrics = listMetrics(endpoint.replace("ec2", "monitoring"));
        List<JSONObject> runningInstances = filterInstancesByInstanceState("running", instances);
        for (JSONObject instance : runningInstances) {
            String instanceId = instance.getString("instanceId");
            List<JSONObject> instanceMetrics = filterMetricsByInstanceId(instanceId, metrics);
            JSONObject statistics = new JSONObject();
            for (JSONObject metric : instanceMetrics) {
                List<JSONObject> datapoints =
                        getMetricStatistics(endpoint.replace("ec2", "monitoring"),
                                metric.getString("Namespace"), metric.getString("MetricName"), "InstanceId", instanceId);
                statistics.element(metric.getString("MetricName"), datapoints.get(0));
            }
            statisticsByInstance.put(instanceId, statistics);
        }
        return statisticsByInstance;
    }

    private List<JSONObject> filterInstancesByInstanceState(String instanceState, List<JSONObject> instances) {
        List<JSONObject> filtered = new ArrayList<JSONObject>();
        for (JSONObject instance : instances) {
            String stateName = instance.getJSONObject("instanceState").getString("name");
            if (stateName.equals(instanceState)) {
                filtered.add(instance);
            }
        }
        return filtered;
    }

    private List<JSONObject> filterMetricsByInstanceId(String instanceId, List<JSONObject> metrics) {
        List<JSONObject> filtered = new ArrayList<JSONObject>();
        for (JSONObject metric : metrics) {
            JSONObject dimension = metric.getJSONObject("Dimensions").getJSONObject("member");
            String dimensionName = dimension.getString("Name");
            String dimensionValue = dimension.getString("Value");
            if (dimensionName.equals("InstanceId") && dimensionValue.equals(instanceId)) {
                filtered.add(metric);
            }
        }
        return filtered;
    }

    public List<JSONObject> getService(String endpoint) throws IOException, InvalidCredentialsException {
        JSONObject rawResponse = makeS3Request(endpoint, null, "/");
        return getJSONChildren("Bucket", rawResponse.getJSONObject("Buckets"));
    }

    public JSONObject getBucketLocation(String endpoint, String bucket) throws IOException, InvalidCredentialsException {
        JSONObject rawResponse = makeS3Request(endpoint, bucket, "/?location");
        return new JSONObject().element("LocationConstraint", rawResponse == null ? "US" : rawResponse.get("#text"));
    }

    public List<JSONObject> getBucketsWithLocation(String endpoint) throws IOException, InvalidCredentialsException {
        List<JSONObject> buckets = getService("s3.amazonaws.com");
        for (JSONObject bucket : buckets) {
            JSONObject location = getBucketLocation("s3.amazonaws.com", bucket.getString("Name"));
            bucket.element("LocationConstraint", location.get("LocationConstraint"));
        }
        return buckets;
    }

    private JSONObject makeRequest(String endpoint, Map<String, String> params) throws IOException, InvalidCredentialsException {
        String url = signUrl("https://" + endpoint + "/", params);
        String rawResponse = HTTPUtils.openUrl(url, "GET", null, null, null, null, null, null);
        JSONObject json = (JSONObject) new XMLSerializer().read(rawResponse);
        return json;
    }

    private JSONObject makeS3Request(String endpoint, String bucket, String path) throws IOException, InvalidCredentialsException {
        String date = getS3FormattedTimestamp(new Date());
        String signature = calculateS3Signature((bucket == null? "" : "/" + bucket) + path, getConnectionCredentials().getCredential(), date);
        List<Header> requestHeader = new ArrayList<Header>();
        requestHeader.add(new BasicHeader("Date", date));
        requestHeader.add(new BasicHeader("Authorization", "AWS " + getConnectionCredentials().getIdentity() + ":" + signature));
        String url = "https://" + (bucket == null ? "" : bucket + ".") + endpoint + path;
        String rawResponse = HTTPUtils.openUrl(url, "GET", null, null, null, null, requestHeader, null);
        JSON json = new XMLSerializer().read(rawResponse);
        return (json instanceof JSONObject ? (JSONObject) json : null);
    }

    private List<JSONObject> getJSONChildren(String key, JSONObject json) {
        List<JSONObject> array = new ArrayList<JSONObject>();
        Object obj = json.get(key);
        if (obj instanceof JSONObject) {
            array.add((JSONObject) obj);
        } else if (obj instanceof JSONArray) {
            array.addAll((JSONArray) obj);
        }
        return array;
    }

    private String signUrl(String rawUrl, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        // http://docs.amazonwebservices.com/general/latest/gr/signature-version-2.html
        params.put("AWSAccessKeyId", getConnectionCredentials().getIdentity());
        params.put("Timestamp", getFormattedTimestamp(new Date()));
        params.put("SignatureVersion", "2");
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Signature", calculateSignature(rawUrl, getConnectionCredentials().getCredential(), params));
        return rawUrl + "?" + getQueryString(params);
    }

    private String getFormattedTimestamp(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }

    private String getS3FormattedTimestamp(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }

    private String getQueryString(Map<String,String> params) {
        StringBuilder query = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> param = iter.next();
            query.append(urlEncode(param.getKey())).append("=").append(urlEncode(param.getValue()));
            if (iter.hasNext()) query.append("&");
        }
        return query.toString();
    }

    private String calculateSignature(String rawUrl, String key, Map<String,String> params) {
        StringBuilder canonical = new StringBuilder();
        try {
            canonical.append("GET").append("\n");
            canonical.append(new URL(rawUrl).getHost()).append("\n");
            canonical.append("/").append("\n");
            SortedMap<String, String> sorted = new TreeMap<String, String>(params);
            canonical.append(getQueryString(sorted));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
            byte[] sign = mac.doFinal(canonical.toString().getBytes("UTF-8"));
            return new String(Base64.encodeBase64(sign));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String calculateS3Signature(String rawUrl, String key, String date) {
        StringBuilder canonical = new StringBuilder();
        try {
            canonical.append("GET").append("\n");
            canonical.append("\n");
            canonical.append("\n");
            canonical.append(date).append("\n");
            canonical.append(rawUrl);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1"));
            byte[] sign = mac.doFinal(canonical.toString().getBytes("UTF-8"));
            return new String(Base64.encodeBase64(sign));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the human readable name for the region id passed in.
     *
     * @param regionId the region id to get its human readable name
     *
     * @return the human readable name or the region id if a human readable name isn't known
     */
    public static String getRegionName(String regionId) {
        // Map the region ids to human readable names (Known regions as of 16/08/2011)
        // Note: This will be unnecessary once jclouds has LocationMetadata
        // Note: This will need to be refactored when we get multiple cloud providers supported if jclouds
        //       isn't updated by then.
        if (regionId.equals("ap-northeast-1")) {
            return "Asia Pacific (Tokyo)";
        } else if (regionId.equals("ap-southeast-1")) {
            return "Asia Pacific (Singapore)";
        } else if (regionId.equals("eu-west-1")) {
            return "EU West (Ireland)";
        } else if (regionId.equals("sa-east-1")) {
            return "South America (Sao Paulo)";
        } else if (regionId.equals("us-east-1")) {
            return "US East (Virginia)";
        } else if (regionId.equals("us-west-1")) {
            return "US West (California)";
        } else if (regionId.equals("us-west-2")) {
            return "US West (Oregon)";
        } else {
            return regionId; // Reasonable default
        }
    }

    /**
     * Returns the iso3166 code for the region id passed in.
     *
     * @param regionId the region id to get its iso3166 code
     *
     * @return the iso3166 code or the region id if a iso3166 code isn't known
     */
    public static String getRegionIso3166Code(String regionId) {
        if (regionId.equals("ap-northeast-1")) {
            return "JP-13";
        } else if (regionId.equals("ap-southeast-1")) {
            return "SG";
        } else if (regionId.equals("eu-west-1")) {
            return "IE";
        } else if (regionId.equals("sa-east-1")) {
            return "BR-SP";
        } else if (regionId.equals("us-east-1")) {
            return "US-VA";
        } else if (regionId.equals("us-west-1")) {
            return "US-CA";
        } else if (regionId.equals("us-west-2")) {
            return "US-OR";
        } else {
            return regionId; // Reasonable default
        }
    }

    /**
     * Returns the human readable name for the hardware id passed in.
     *
     * @param hardwareId the hardware id to get its human readable name
     *
     * @return the human readable name or the hardware id if a human readable name isn't known
     */
    public static String getHardwareName(String hardwareId) {
        // Map the hardware ids to human readable names (Known hardware ids as of 16/08/2011)
        // http://aws.amazon.com/ec2/instance-types/
        // Note: This will be unnecessary once jclouds has LocationMetadata
        // Note: This will need to be refactored when we get multiple cloud providers supported if jclouds
        //       isn't updated by then.
        if (hardwareId.equals("c1.medium")) {
            return "High-CPU Medium";
        } else if (hardwareId.equals("c1.xlarge")) {
            return "High-CPU Extra Large";
        } else if (hardwareId.equals("cg1.4xlarge")) {
            return "Cluster GPU Quadruple Extra Large";
        } else if (hardwareId.equals("cc1.4xlarge")) {
            return "Cluster Compute Quadruple Extra Large";
        } else if (hardwareId.equals("m1.large")) {
            return "Large";
        } else if (hardwareId.equals("m1.small")) {
            return "Small";
        } else if (hardwareId.equals("m1.xlarge")) {
            return "Extra Large";
        } else if (hardwareId.equals("m2.xlarge")) {
            return "High-Memory Extra Large";
        } else if (hardwareId.equals("m2.2xlarge")) {
            return "High-Memory Double Extra Large";
        } else if (hardwareId.equals("m2.4xlarge")) {
            return "High-Memory Quadruple Extra Large";
        } else if (hardwareId.equals("t1.micro")) {
            return "Micro";
        } else {
            return hardwareId; // Reasonable default
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        getComputeServiceContext();
        getBlobStoreContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp() {
        try {
            if (computeServiceContext != null) {
                computeServiceContext.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to clean up the AWS EC2 connection.", e);
        }
        try {
            if (cloudWatchContext != null) {
                cloudWatchContext.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to clean up the AWS Cloudwatch connection.", e);
        }
        try {
            if (blobStoreContext != null) {
                blobStoreContext.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to clean up the AWS Blobstore connection.", e);
        }
    }

    /**
     * Returns the jclouds {@link ComputeServiceContext} for AWS EC2.
     *
     * @return the jclouds compute service context
     *
     * @throws InvalidCredentialsException if the credentials associated with this client are invalid
     */
    public ComputeServiceContext getComputeServiceContext() throws InvalidCredentialsException {
        if (computeServiceContext == null) {
            Properties overrides = new Properties();

            // Empty the default AMI queries for quicker image lookup
            overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
            overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");

            String username = getConnectionCredentials().getIdentity();
            String password = getConnectionCredentials().getCredential();

            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                throw new InvalidCredentialsException("You must supply an identity/username for cloud connections.");
            }

            try {
                computeServiceContext = ContextBuilder.newBuilder("aws-ec2")
                                                      .credentials(username, password)
                                                      .modules(ImmutableSet.<Module>of(
                                                              new SshjSshClientModule(),
                                                              new SLF4JLoggingModule()))
                                                      .overrides(overrides)
                                                      .buildView(ComputeServiceContext.class);

                // since the compute service context doesn't try to auth against AWS until it
                // needs to, make a call to listNodes() to force the auth.
                computeServiceContext.getComputeService().listNodes();
            } catch (AuthorizationException ae) {
                throw new InvalidCredentialsException(ae);
            }
        }

        return computeServiceContext;
    }

    /**
     * Returns the jclouds {@link BlobStoreContext} for AWS S3.
     *
     * @return the jclouds blob store context
     *
     * @throws InvalidCredentialsException if the credentials associated with this client are invalid
     */
    public BlobStoreContext getBlobStoreContext() throws InvalidCredentialsException {
        if (blobStoreContext == null) {
            String username = getConnectionCredentials().getIdentity();
            String password = getConnectionCredentials().getCredential();

            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                throw new InvalidCredentialsException("You must supply an identity/username for cloud connections.");
            }

            try {
                blobStoreContext = ContextBuilder.newBuilder("aws-s3")
                                                 .credentials(username, password)
                                                 .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()))
                                                 .buildView(BlobStoreContext.class);
            } catch (AuthorizationException ae) {
                throw new InvalidCredentialsException(ae);
            }
        }

        return blobStoreContext;
    }

    /**
     * Returns the jclouds {@link RestContext} for AWS CloudWatch.
     *
     * @return the jclouds RestContext for AWS Cloudwatch
     *
     * @throws InvalidCredentialsException if the credentials associated with this client are invalid
     */
    public RestContext<CloudWatchApi, CloudWatchAsyncApi> getCloudWatchServiceContext()
            throws InvalidCredentialsException {
        if (cloudWatchContext == null) {
            String username = getConnectionCredentials().getIdentity();
            String password = getConnectionCredentials().getCredential();

            try {
                cloudWatchContext = ContextBuilder.newBuilder("aws-cloudwatch")
                        .credentials(username, password)
                        .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()))
                        .build();
            } catch (AuthorizationException ae) {
                throw new InvalidCredentialsException(ae);
            }
        }

        return cloudWatchContext;
    }


    /**
     *
     * @param outboundConfiguration
     * @return the name of the created bucket
     */
    public String createBucket(OutboundConfiguration outboundConfiguration) throws InvalidCredentialsException {
        BlobStore s3BlobStore = getBlobStoreContext().getBlobStore();
        String bucketName = convertOutboundConnectionToBucketName(outboundConfiguration);

        Location location = null;
        if (StringUtils.hasText(outboundConfiguration.getDestination())) {
            LocationBuilder builder = new LocationBuilder();
            builder.id(outboundConfiguration.getDestination());
            builder.scope(LocationScope.REGION);
            builder.description(outboundConfiguration.getDestination());

            location = builder.build();
        }

        s3BlobStore.createContainerInLocation(location ,bucketName); //defaults to us-standard if location is null
        return bucketName;
    }

    /**
     *
     * @param outboundConfiguration
     * @param payloadIdentifier
     * @param payload bytes to be written to S3.
     * @return an eTag of the object that was just created.
     */
    public String pushToS3(OutboundConfiguration outboundConfiguration, String payloadIdentifier, byte[] payload)
            throws InvalidCredentialsException {
        BlobStore s3BlobStore = getBlobStoreContext().getBlobStore();
        String bucketName = createBucket(outboundConfiguration);
        Blob blob = s3BlobStore.blobBuilder(payloadIdentifier).payload(payload).build();
        return s3BlobStore.putBlob(bucketName,blob);
    }

    /**
     * Extracts a bucket name from an OutboundConfiguration.  If the OutboundConfiguration's namespace property is non-null
     * and contains text, that property is used as the bucketName.  Otherwise, a default bucketname in the form of
     * "com.streamreduce.${account.id}" is used to create the bucket.
     * @param outboundConfiguration - The OutboundConfiguration reference representing an outbound operation to S3
     * @return Either OutboundConfiguration.getNamespace() or "com.streamreduce.${account.id}" if namespace
     * is empty.
     */
    String convertOutboundConnectionToBucketName(OutboundConfiguration outboundConfiguration) {
        if (StringUtils.hasText(outboundConfiguration.getNamespace())) {
            return outboundConfiguration.getNamespace().toLowerCase();
        } else {
            return BUCKET_PREFIX + "." +
                    outboundConfiguration.getOriginatingConnection().getAccount().getId().toString().toLowerCase();
        }
    }

}
