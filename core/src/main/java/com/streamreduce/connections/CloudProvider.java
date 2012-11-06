package com.streamreduce.connections;


import com.streamreduce.ConnectionTypeConstants;

/**
 * The CloudProvider interface describes a cloud provider.
 */
public interface CloudProvider extends ConnectionProvider {

    public static final String TYPE = ConnectionTypeConstants.CLOUD_TYPE;

    /**
     * Returns a string that maps to the jclouds provider id for the
     * provider's compute service.
     *
     * @return the jclouds id for the provider's compute service
     */
    public String getComputeId();

}
