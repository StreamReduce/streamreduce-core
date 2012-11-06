package com.streamreduce.connections;

import java.util.Set;

public interface ConnectionProvider {

    /**
     * Returns a unique string identifying the provider.
     *
     * @return the provider's unique identifier
     */
    public String getId();

    /**
     * Returns the provider type as string.
     * 
     * @return the provider type
     */
    public String getType();

    /**
     * Returns a string to display to the user for the provider.
     *
     * @return the provider's display name
     */
    public String getDisplayName();

    /**
     * Returns a string that the cloud provider uses for the "username"
     * credential for authenticating to their service.
     *
     * @return the label displayed for the "username" credential
     */
    public String getUsernameLabel();

    /**
     * Returns a string that the cloud provider uses for the "password"
     * credential for authenticating to their service.
     *
     * @return the label displayed for the "password" credential
     */
    public String getPasswordLabel();

    /**
     * Returns a Set of all AuthTypes that are supported by a provider.
     * @return all supported AuthTypes
     */
    public Set<AuthType> getSupportedAuthTypes();


}
