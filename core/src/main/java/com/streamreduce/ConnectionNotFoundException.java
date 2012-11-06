package com.streamreduce;

public class ConnectionNotFoundException extends NotFoundException {
    private static final long serialVersionUID = -5112561671556115157L;

    public ConnectionNotFoundException(String id) {
        // TODO: Add the connection type into the message
        super("Unable to find a connection with an id of " + id);
    }
}
