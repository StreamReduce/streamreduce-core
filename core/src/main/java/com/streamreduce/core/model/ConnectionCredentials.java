package com.streamreduce.core.model;

import com.google.code.morphia.annotations.EntityListeners;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents the credentials for a Connection object.
 *
 * <p>While the class has fields for various authentication methods, it's up to the connection client to
 * determine the correct fields to use. In some cases only identity or identity/credential are used (and
 * possibly overloaded), while other cases may see different fields in use.
 * </p>
 */
@EntityListeners(ConnectionCredentialsEncrypter.class)
public class ConnectionCredentials {

    private String identity;
    private String credential;
    private String apiKey;
    private String oauthToken;
    private String oauthTokenSecret;
    private String oauthRefreshToken;
    private transient String oauthVerifier;

    public ConnectionCredentials(@NotNull String identity, @Nullable String credential) {
        this.identity = identity;
        this.credential = credential;
    }

    public ConnectionCredentials(@NotNull String identity, @NotNull String credential, @NotNull String apiKey) {
        this.identity = identity;
        this.credential = credential;
        this.apiKey = apiKey;
    }

    public ConnectionCredentials() {
    }

    /**
     * Returns the cloud's "username" argument.
     * <p/>
     * (Note: Username is in quotes because you might not see username
     * in the UI and might instead see the cloud provider's
     * verbiage for the argument.  For example, for AWS the
     * username argument corresponds to the "Access Key ID".)
     *
     * @return the cloud's username argument
     */
    public String getIdentity() {
        return identity;
    }


    /**
     * Sets the cloud's "username" argument.
     *
     * @param identity the new username argument for the cloud
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    /**
     * Returns the cloud's "password" argument, which for some
     * cloud providers this could be null or empty signifying it is not used.
     * <p/>
     * (Note: Password is in quotes because you might not see password
     * in the UI and might instead see the cloud provider's
     * verbiage for the argument.  For example, for AWS the
     * password argument corresponds to the "Secret Key ID".)
     *
     * @return the cloud's password argument
     */
    public String getCredential() {
        return credential;
    }

    /**
     * Sets the cloud's "password" argument.
     *
     * @param credential the new password argument for the cloud
     */
    public void setCredential(String credential) {
        this.credential = credential;
    }

    /**
     * Returns the API key used for the associated connection. See the class docs for more information.
     *
     * @return connection API key
     */
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns the OAuth token.
     *
     * @return string
     */
    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public String getOauthVerifier() {
        return oauthVerifier;
    }

    public void setOauthVerifier(String oauthVerifier) {
        this.oauthVerifier = oauthVerifier;
    }

    /**
     * Returns the OAuth token secret.
     *
     * @return string
     */
    public String getOauthTokenSecret() {
        return oauthTokenSecret;
    }

    public void setOauthTokenSecret(String oauthTokenSecret) {
        this.oauthTokenSecret = oauthTokenSecret;
    }

    /**
     * Returns the OAuth refresh token.
     *
     * @return string
     */
    public String getOauthRefreshToken() {
        return oauthRefreshToken;
    }

    public void setOauthRefreshToken(String oauthRefreshToken) {
        this.oauthRefreshToken = oauthRefreshToken;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(StringUtils.lowerCase(identity))
                .append(credential)
                .append(apiKey)
                .append(oauthToken)
                .append(oauthTokenSecret)
                .append(oauthVerifier)
                .append(oauthRefreshToken)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConnectionCredentials) {
            ConnectionCredentials that = (ConnectionCredentials) o;
            return new EqualsBuilder()
                    .append(StringUtils.lowerCase(this.identity), StringUtils.lowerCase(that.identity))
                    .append(this.credential, that.credential)
                    .append(this.apiKey, that.apiKey)
                    .append(this.oauthToken, that.oauthToken)
                    .append(this.oauthTokenSecret, that.oauthTokenSecret)
                    .append(this.oauthVerifier, that.oauthVerifier)
                    .append(this.oauthRefreshToken, that.oauthRefreshToken)
                    .isEquals();
        }
        return false;
    }

    /**
     * <p>... because clone sucks.</p>
     *
     * <p>Used to make a defensive copy before serialization of ConnectionCredentials so that the original reference
     * is not encrypted.</p>
     *
     * @param orig An original instance of ConnectionCredentials
     * @return A new instance of ConnectionCredentials with the same values as the original, or null if null was passed
     * in.
     */
    public static ConnectionCredentials copyOf(ConnectionCredentials orig) {
        if (orig == null) { return null; }
        ConnectionCredentials newCC = new ConnectionCredentials();
        newCC.identity = orig.identity;
        newCC.credential = orig.credential;
        newCC.apiKey = orig.apiKey;
        newCC.oauthToken = orig.oauthToken;
        newCC.oauthTokenSecret = orig.oauthTokenSecret;
        newCC.oauthVerifier = orig.oauthVerifier;
        return newCC;
    }
}
