package com.streamreduce.connections;

import com.streamreduce.ConnectionTypeConstants;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/5/12 09:27</p>
 */
public interface AnalyticsProvider extends ConnectionProvider {

    public static final String TYPE = ConnectionTypeConstants.ANALYTICS_TYPE;

}
