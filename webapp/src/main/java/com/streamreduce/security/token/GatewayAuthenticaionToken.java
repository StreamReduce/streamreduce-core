package com.streamreduce.security.token;

public class GatewayAuthenticaionToken extends NodeableAuthenticationToken {
    private static final long serialVersionUID = 1724655084055036920L;

    public GatewayAuthenticaionToken(final String gatewayToken) {
        super(gatewayToken);
    }
}
