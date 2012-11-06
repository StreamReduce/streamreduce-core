package com.streamreduce.connections;

import com.google.common.collect.Sets;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.oauth.api.GitHubApi;
import com.streamreduce.util.GitHubClient;
import net.sf.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The GitHubProjectHostingProvider class is an implementation of the
 * {@link ProjectHostingProvider} that provides support for the GitHub project hosting
 * services.
 */
@Component
public class GitHubProjectHostingProvider extends AbstractProjectHostingConnectionProvider
        implements OAuthEnabledConnectionProvider, ExternalIntegrationConnectionProvider {

    static final private Set<AuthType> SUPPORTED_AUTH_TYPES = Sets.immutableEnumSet(AuthType.OAUTH);

    @Value("${nodeable.github.oauth.client_id}")
    private String oauthClientId;
    @Value("${nodeable.github.oauth.secret}")
    private String oauthSecret;
    @Value("${nodeable.github.oauth.callback.url}")
    private String oauthCallbackUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ProviderIdConstants.GITHUB_PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "GitHub";
    }

    @Override
    public Set<AuthType> getSupportedAuthTypes() {
        return SUPPORTED_AUTH_TYPES;
    }

    @Override
    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                .provider(GitHubApi.class)
                .apiKey(oauthClientId)
                .apiSecret(oauthSecret)
                .callback(oauthCallbackUrl)
                .build();
    }

    @Override
    public String getAuthorizationUrl() {
        return getOAuthService().getAuthorizationUrl(null);
    }

    @Override
    public String getIdentityFromProvider(Connection c) {
        try {
            GitHubClient gitHubClient = getClient(c);
            JSONObject jsonObject = gitHubClient.getUser();
            return jsonObject.getString("login");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCredentials(ConnectionCredentials credentials, Token token) {
        credentials.setOauthToken(token.getToken());
        credentials.setOauthTokenSecret(token.getSecret());
        credentials.setIdentity(null);
        credentials.setCredential(null);
        credentials.setApiKey(null);
    }

    @Override
    public GitHubClient  getClient(Connection connection) {
        return new GitHubClient(connection,getOAuthService());
    }
}
