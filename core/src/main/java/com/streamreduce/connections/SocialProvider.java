package com.streamreduce.connections;

import com.streamreduce.ConnectionTypeConstants;

/**
 * The CloudProvider interface describes a social network provider.
 */
public interface SocialProvider extends ConnectionProvider {

    public static final String TYPE = ConnectionTypeConstants.SOCIAL_TYPE;

}
