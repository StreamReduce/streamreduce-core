package com.streamreduce.connections;

import com.streamreduce.ConnectionTypeConstants;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/26/12 14:24</p>
 */
public interface MonitoringProvider extends ConnectionProvider {
    public static final String TYPE = ConnectionTypeConstants.MONITORING_TYPE;
}
