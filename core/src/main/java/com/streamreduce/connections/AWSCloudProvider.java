package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.util.AWSClient;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The AWSCloudProvider class is an implementation of the
 * {@link CloudProvider} that provides support
 * for the Amazon cloud services.
 */
@Component("awsCloudProvider")
public class AWSCloudProvider extends AbstractCloudConnectionProvider implements ExternalIntegrationConnectionProvider {


    //AWS AccessKeyId and SecretKey act as a Username/Password pair.
    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.USERNAME_PASSWORD);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ProviderIdConstants.AWS_PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Amazon Web Services";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsernameLabel() {
        return "Access Key ID";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPasswordLabel() {
        return "Secret Access Key";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComputeId() {
        return "aws-ec2";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AWSClient getClient(Connection connection) {
        return new AWSClient(connection);
    }
}
