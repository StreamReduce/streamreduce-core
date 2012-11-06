package com.streamreduce.connections;

import com.streamreduce.ConnectionTypeConstants;

public interface GatewayProvider extends ConnectionProvider {
    public static final String TYPE = ConnectionTypeConstants.GATEWAY_TYPE;
}
