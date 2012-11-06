package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/3/12 8:54 AM</p>
 */
@Component
public class WebHDFSProvider extends AbstractGatewayProvider {

    private static final Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.USERNAME_PASSWORD);

    WebHDFSProvider() {}

    @Override
    public String getId() {
        return ProviderIdConstants.WEBHDFS_PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "WebHDFS";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

}
