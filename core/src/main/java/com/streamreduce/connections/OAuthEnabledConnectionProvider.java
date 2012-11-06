package com.streamreduce.connections;

import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public interface OAuthEnabledConnectionProvider extends ConnectionProvider {
    OAuthService getOAuthService();
    String getAuthorizationUrl();
    String getIdentityFromProvider(Connection c);
    void updateCredentials(ConnectionCredentials credentials, Token token);
}
