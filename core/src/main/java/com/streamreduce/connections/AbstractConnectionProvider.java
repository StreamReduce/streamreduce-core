package com.streamreduce.connections;


public abstract class AbstractConnectionProvider implements ConnectionProvider {

    AbstractConnectionProvider() {
    }

    /**
     * {@inheritDoc}
     */
    public String getUsernameLabel() {
        return "Username";
    }

    /**
     * {@inheritDoc}
     */
    public String getPasswordLabel() {
        return "Password";
    }

}
