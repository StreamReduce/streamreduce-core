package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import org.hibernate.validator.constraints.NotEmpty;
import org.scribe.model.Token;

/**
 * Model class used solely to cache oauthRequestTokens to our data tier in order to support
 * three-legged Oauth requests that require us to keep request token between steps of the
 * Oauth handshake.
 */
@Entity(value = "oauthRequestTokenCache", noClassnameStored = true)
public class OAuthRequestToken {

    @NotEmpty
    @Indexed(unique = true)
    @Id
    private String oauthToken;
    private String oauthTokenSecret;

    @SuppressWarnings("unused") //used by morphia
    private OAuthRequestToken() {}

    /**
     * Creates an OauthRequestToken object from an existing Token.  If token is null, then all fields
     * in the created object are initialized to null.  Otherwise, oauthToken is initialized to to token.token
     * and oauthTokenSecret is initialized to token.secret.
     * @param token
     */
    public OAuthRequestToken(Token token) {
        if (token == null) {
            return;
        }
        this.oauthToken = token.getToken();
        this.oauthTokenSecret = token.getSecret();
    }

    /**
     * Converts this OAuthRequestToken model object to a Scribe Token.
     * @return a Scribe Token object with the same token and secret as kept in this model.
     */
    public Token toScribeToken() {
        return new Token(oauthToken, oauthTokenSecret);
    }
}
