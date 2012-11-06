package com.streamreduce.rest.dto.response;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class AuthTypeResponseDTO {
    private final String type;
    private final String usernameLabel;
    private final String passwordLabel;
    private final String commandLabel;
    private final String oauthEndpoint;

    private AuthTypeResponseDTO(Builder b) {
        type = b.type;
        usernameLabel = b.usernameLabel;
        passwordLabel = b.passwordLabel;
        commandLabel = b.commandLabel;
        oauthEndpoint = b.oauthEndpoint;
    }

    public String getType() {
        return type;
    }

    public String getUsernameLabel() {
        return usernameLabel;
    }

    public String getPasswordLabel() {
        return passwordLabel;
    }

    public String getCommandLabel() {
        return commandLabel;
    }

    public String getOauthEndpoint() {
        return oauthEndpoint;
    }

    public static class Builder {
        private String type;
        private String usernameLabel;
        private String passwordLabel;
        private String commandLabel;
        private String oauthEndpoint;

        public AuthTypeResponseDTO build() {
            return new AuthTypeResponseDTO(this);
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder usernameLabel(String usernameLabel) {
            this.usernameLabel = usernameLabel;
            return this;
        }

        public Builder passwordLabel(String passwordLabel) {
            this.passwordLabel = passwordLabel;
            return this;
        }

        public Builder commandLabel(String commandLabel) {
            this.commandLabel = commandLabel;
            return this;
        }

        public Builder oauthEndpoint(String oauthEndpoint) {
            this.oauthEndpoint = oauthEndpoint;
            return this;
        }


    }
}
