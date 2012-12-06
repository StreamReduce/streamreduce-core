/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.streamreduce.core.service;

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.ExternalIntegrationConnectionProvider;
import com.streamreduce.connections.OAuthEnabledConnectionProvider;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.ConnectionCredentialsEncrypter;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.service.exception.ConnectionExistsException;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.streamreduce.util.AWSClient;
import com.streamreduce.util.ExternalIntegrationClient;
import com.streamreduce.util.HashtagUtil;
import com.streamreduce.util.InvalidOutboundConfigurationException;
import com.streamreduce.util.SecurityUtil;
import com.streamreduce.util.WebHDFSClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


/**
 * Implementation of {@link ConnectionService}.
 */
@Service("connectionService")
public class ConnectionServiceImpl implements ConnectionService {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionDAO connectionDAO;
    @Autowired
    private ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private EventService eventService;
    @Autowired
    private OAuthTokenCacheService cacheService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection(Connection connection)
            throws ConnectionExistsException, InvalidCredentialsException, IOException {

        addDefaultProtocolToURLIfMissing(connection);
        checkForDuplicate(connection);
        setCredentialsIfOauth(connection);
        setCredentialsIfGateway(connection);
        validateExternalIntegrationConnections(connection);
        validateOutboundConfigurations(connection.getOutboundConfigurations());

        // Add provider id to hashtags just in case it wasn't done already
        String providerId = connection.getProviderId();
        connection.addHashtag(providerId);

        connectionDAO.save(connection);

        decryptCredentials(connection);

        // Create the event stream entry
        Event event = eventService.createEvent(EventId.CREATE, connection, null);

        // Create message
        messageService.sendConnectionMessage(event, connection);

        return connection;
    }

    private void decryptCredentials(Connection connection) {
        ConnectionCredentialsEncrypter credentialsEncrypter = new ConnectionCredentialsEncrypter();
        // force decryption until we upgrade to morphia 1.0
        if (connection.getCredentials() != null) {
            credentialsEncrypter.decrypt(connection.getCredentials());
        }
        if (CollectionUtils.isNotEmpty(connection.getOutboundConfigurations())) {
            for (OutboundConfiguration outboundConfiguration : connection.getOutboundConfigurations()) {
                if (outboundConfiguration.getCredentials() != null) {
                    credentialsEncrypter.decrypt(outboundConfiguration.getCredentials());
                }
            }
        }
    }

    private void addDefaultProtocolToURLIfMissing(Connection connection) {
        // For Feed connections, hold the user's hand by putting the default protocol (http://) on the URL if not there
        if (connection.getProviderId().equals(ProviderIdConstants.FEED_PROVIDER_ID)) {
            // Add the http:// protocol if missing
            try {
                new URL(connection.getUrl());
            } catch (MalformedURLException e) {
                if (connection.getUrl() != null && !connection.getUrl().contains("://")) {
                    connection.setUrl("http://" + connection.getUrl().trim());
                }
            }
        }
    }

    /**
     * Initializes values in connection.credentials for Oauth connections. There are three possible things that
     * can happen (not taking exceptions into account).
     * <p/>
     * <ol>
     * <li>If connection.authType isn't OAUTH, this method returns immediately without mutating anything in
     * connection</li>
     * <li>If connection.credentials.verifier is null/blank, we assume that the credentials.oauthToken
     * and credentials.oauthTokenSecret fields were previously valid and are meant to be re-used.</li>
     * <li>If connection.credentials.verifier had content, we assume that we are in the last steps of
     * an OAuth handshake with the connection provider and real credentials will retrieved using that
     * verification field.</li>
     * </ol>
     *
     * @param connection A connection currently being created.
     */
    private void setCredentialsIfOauth(Connection connection) {
        if (connection.getAuthType() != AuthType.OAUTH) {
            return;
        }

        if (StringUtils.hasText(connection.getCredentials().getOauthVerifier())) {
            finishHandshakeAndSetRealOauthTokens(connection);
        }

        setIdentityForOauthConnection(connection);
    }

    private void finishHandshakeAndSetRealOauthTokens(Connection connection) {
        ConnectionCredentials credentials = connection.getCredentials();
        OAuthEnabledConnectionProvider oauthProvider =
                connectionProviderFactory.oauthEnabledConnectionProviderFromId(connection.getProviderId());
        OAuthService oAuthService = oauthProvider.getOAuthService();
        Token requestToken = cacheService.retrieveAndRemoveToken(credentials.getOauthToken());
        Token token = oAuthService.getAccessToken(requestToken, new Verifier(credentials.getOauthVerifier()));
        oauthProvider.updateCredentials(credentials, token);
    }

    private void setIdentityForOauthConnection(Connection connection) {
        OAuthEnabledConnectionProvider oauthProvider =
                connectionProviderFactory.oauthEnabledConnectionProviderFromId(connection.getProviderId());
        connection.getCredentials().setIdentity(oauthProvider.getIdentityFromProvider(connection));
    }

    private void setCredentialsIfGateway(Connection connection) {
        if (connection.getAuthType() != null && connection.getAuthType().equals(AuthType.API_KEY)) {
            // auto generate an API key for them
            if (connection.getCredentials() == null) {
                connection.setCredentials(new ConnectionCredentials());
            }
            // we also use a user agent as a validation factor
            // so when we later validate the token, we also validate the user agent
            String apiToken = SecurityUtil.issueRandomAPIToken();
            connection.getCredentials().setIdentity(apiToken);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Connection> getConnections(@Nullable String type) {
        // TODO: Event handling
        return connectionDAO.allConnectionsOfType(type);
    }

    @Override
    public List<Connection> getPublicConnections(@Nullable String type) {
        return connectionDAO.allPublicConnectionsOfType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Connection> getConnections(@Nullable String type, User user) {
        // TODO: Event handling
        return connectionDAO.forTypeAndUser(type, user);
    }


    @Override
    public List<Connection> getAccountConnections(Account account) {
        return connectionDAO.forAccount(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection(ObjectId id) throws ConnectionNotFoundException {
        Connection connection = connectionDAO.get(id);

        if (connection == null) {
            // TODO: Event handling
            throw new ConnectionNotFoundException(id == null ? "null" : id.toString());
        }

        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection updateConnection(Connection connection) throws ConnectionExistsException,
            InvalidCredentialsException, IOException {
        return updateConnection(connection, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection updateConnection(Connection connection, boolean silentUpdate) throws ConnectionExistsException,
            InvalidCredentialsException, IOException {

        connection.setSilentUpdate(silentUpdate);

        if (!silentUpdate) {
            checkForDuplicate(connection);
            validateExternalIntegrationConnections(connection);
            // always ensure updated, validated connections are flagged as unbroken.
            connection.setAsUnbroke();
            validateOutboundConfigurations(connection.getOutboundConfigurations());
        }

        connectionDAO.save(connection);

        decryptCredentials(connection);

        if (!silentUpdate) {
            // Create the event stream entry
            Event event = eventService.createEvent(EventId.UPDATE, connection, null);

            // Create message
            messageService.sendConnectionMessage(event, connection);
        }

        return connection;
    }

    private void validateExternalIntegrationConnections(Connection connection) throws InvalidCredentialsException, IOException {
        ExternalIntegrationConnectionProvider connectionProvider;
        try {
            connectionProvider = connectionProviderFactory.externalIntegrationConnectionProviderFromId(connection.getProviderId());
        } catch (Exception e) {
            logger.info("Connection with providerId of " + connection.getProviderId() + " does not " +
                    "support validation via an external client.");
            return;
        }

        ExternalIntegrationClient externalClient = connectionProvider.getClient(connection);
        externalClient.validateConnection();
        externalClient.cleanUp();
    }

    private void validateOutboundConfigurations(Set<OutboundConfiguration> outboundConfigurations) throws InvalidCredentialsException, IOException {
        if (CollectionUtils.isEmpty(outboundConfigurations)) {
            return;
        }

        ExternalIntegrationClient externalClient = null;
        try {
            for (OutboundConfiguration outboundConfiguration : outboundConfigurations) {
                if (outboundConfiguration.getProtocol().equals("s3")) {
                    AWSClient awsClient = new AWSClient(outboundConfiguration);
                    externalClient = awsClient;
                    externalClient.validateConnection();
                    try {
                        awsClient.createBucket(outboundConfiguration);
                    } catch (IllegalStateException e) { //thrown when a bucket name is already taken
                        throw new InvalidOutboundConfigurationException(e.getMessage(), e);
                    }
                } else if (outboundConfiguration.getProtocol().equals("webhdfs")) {
                    externalClient = new WebHDFSClient(outboundConfiguration);
                    externalClient.validateConnection();
                }
            }
        } finally {
            if (externalClient != null) {
                externalClient.cleanUp();
            }
        }
    }

    public void deleteConnection(Connection connection) {
        deleteConnectionInventory(connection.getId());

        Event event = eventService.createEvent(EventId.DELETE, connection, null);
        messageService.sendConnectionMessage(event, connection);

        connectionDAO.delete(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteConnectionInventory(ObjectId connectionId) {
        List<InventoryItem> inventoryItems = inventoryService.getInventoryItems(connectionId);

        for (InventoryItem inventoryItem : inventoryItems) {
            inventoryService.deleteInventoryItem(inventoryItem);
        }
    }

    /**
     * Checks a given connection against all existing connections in a given account to ensure a duplicate connection
     * does not already exist.  A connection is considered a duplicate of another connection when
     * <ul>
     * <li>Aliases are the same</li>
     * <li>If there are credentials with the connection, url+credentials must not be the same</li>
     * <li>If there are no credentials with the connection, url must be not be the same</li>
     * </ul>
     *
     * @param connection a potential new/updated Connection
     * @throws ConnectionExistsException when a duplicate connection is detected.
     */
    protected void checkForDuplicate(Connection connection) throws ConnectionExistsException {
        List<Connection> connections = connectionDAO.forTypeAndUser(connection.getType(), connection.getUser());

        for (Connection otherConnection : connections) {
            // We use this for create and update so id might not be set yet
            try {
                // If the connection being compared against is the same as the one being updated, do not compare
                if (getConnection(connection.getId()) != null && otherConnection.getId().equals(connection.getId())) {
                    continue;
                }
            } catch (ConnectionNotFoundException e) {
                // Should not matter in this context
            }

            checkForEqualAlias(connection, otherConnection);

            String cUrl = connection.getUrl() != null ? connection.getUrl().trim().toLowerCase() : "";
            String oUrl = otherConnection.getUrl() != null ? otherConnection.getUrl().trim().toLowerCase() : "";

            // Duplicate if credentials aren't set and URL exists elsewhere in account.
            if (credsAreNullOrBlank(connection) && credsAreNullOrBlank(otherConnection) && cUrl.equals(oUrl)) {
                throw ConnectionExistsException.Factory.duplicateCredentials(connection);
            }

            // Duplicate if both credentials and URLs are equal.
            // If the URLs are the same then make sure credentials are not
            if (cUrl.equals(oUrl)) {
                ConnectionCredentials cCreds = connection.getCredentials();
                ConnectionCredentials oCreds = otherConnection.getCredentials();

                if ((cCreds == null && oCreds == null) || (cCreds != null && oCreds != null && cCreds.equals(oCreds))) {
                    throw ConnectionExistsException.Factory.duplicateCredentials(connection);
                }
            }
        }
    }

    private boolean credsAreNullOrBlank(Connection connection) {
        return connection.getCredentials() == null || connection.getCredentials().getIdentity() == null ||
                connection.getCredentials().getIdentity().trim().equals("");
    }

    private void checkForEqualAlias(Connection newConnection, Connection existingConnection)
            throws ConnectionExistsException {
        if (newConnection.getAlias() != null && existingConnection.getAlias() != null &&
                newConnection.getAlias().equalsIgnoreCase(existingConnection.getAlias())) {
            throw ConnectionExistsException.Factory.duplicateAlias(newConnection);
        }
    }

    @Override
    public void fireOneTimeHighPriorityJobForConnection(Connection connection) throws ConnectionNotFoundException,
            InvalidCredentialsException, IOException {
        inventoryService.refreshInventoryItemCache(connection);
        inventoryService.pullInventoryItemActivity(connection);
    }

    @Override
    public void addHashtag(Connection target, SobaObject tagger, String tag) {
        target.addHashtag(tag);

        handleHashtagEvent(EventId.HASHTAG_ADD, target, tagger, tag);

        try {
            updateConnection(target, true);
        } catch (ConnectionExistsException e) {
            logger.error(e.getMessage());
        } catch (InvalidCredentialsException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void removeHashtag(Connection target, SobaObject tagger, String tag) {
        String normalizedTag = HashtagUtil.normalizeTag(tag);
        target.removeHashtag(normalizedTag);

        handleHashtagEvent(EventId.HASHTAG_DELETE, target, tagger, normalizedTag);

        try {
            updateConnection(target, true);
        } catch (ConnectionExistsException e) {
            logger.error(e.getMessage());
        } catch (InvalidCredentialsException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void handleHashtagEvent(EventId eventId, Connection target, SobaObject tagger, String tag) {
        // Create the event
        Map<String, Object> eventContext = new HashMap<>();

        if (eventId == EventId.HASHTAG_ADD) {
            eventContext.put("addedHashtag", tag);
        } else if (eventId == EventId.HASHTAG_DELETE) {
            eventContext.put("deletedHashtag", tag);
        }

        Event event = eventService.createEvent(eventId, target, eventContext);

        // Create the message
        // TODO: Should this use MessageService#sendConnectionMessage(Event, Connection)?
        messageService.sendAccountMessage(event, tagger, target, new Date().getTime(),
                MessageType.CONNECTION, target.getHashtags(), null);
    }

    @Override
    public void flagConnectionAsBroken(Connection connection, String lastErrorMessage) {
        connection.setAsBroke("[" + new Date() + "] - " + lastErrorMessage);
        connectionDAO.save(connection);
    }

    @Override
    public void clearBrokenFlag(Connection connection) {
        connection.setAsUnbroke();
        connectionDAO.save(connection);
    }

    @Override
    public Connection getConnectionByGUID(String guid) throws ConnectionNotFoundException{
        // TODO:
        return null;
    }
}
