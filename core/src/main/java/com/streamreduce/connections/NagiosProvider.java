package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/13/12 13:44</p>
 */
@Component
public class NagiosProvider extends AbstractGatewayProvider implements PushConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.API_KEY);

    @Override
    public String getId() {
        return ProviderIdConstants.NAGIOS_PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Nagios";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }
}
