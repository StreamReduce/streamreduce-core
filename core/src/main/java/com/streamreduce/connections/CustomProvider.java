package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CustomProvider extends AbstractGatewayProvider implements PushConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.API_KEY);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ProviderIdConstants.CUSTOM_PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Custom Connection";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }



}
