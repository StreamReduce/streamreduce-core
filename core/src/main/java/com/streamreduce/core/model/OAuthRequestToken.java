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
