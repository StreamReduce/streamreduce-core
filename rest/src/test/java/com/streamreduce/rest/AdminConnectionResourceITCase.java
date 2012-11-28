package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;

import org.junit.Test;

public class AdminConnectionResourceITCase extends AbstractInContainerTestCase {

    private String authToken;

    public AdminConnectionResourceITCase() {
        super();
    }

    @Override
    public void tearDown() throws Exception {
        logout(authToken);
        super.tearDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // login
        authToken = login(testUsername, testUsername);
    }

    protected String getUrl() {
        return getPrivateUrlBase() + "/admin/connection/";
    }

    @Test
    public void testGetAPIKeyByAppId() throws Exception {
    }

    @Test
    public void testGetAPIKeyByGuid() throws Exception {
    }
}
