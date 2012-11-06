package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatusResourceITCase extends AbstractInContainerTestCase {

    protected String getUrl() {
        return "http://localhost:6099/status/system";
    }

    @Test
    @Ignore
    public void testStatusBasic() throws Exception {

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(getUrl());

        try {
            HttpResponse response =  client.execute(get);
            int status = response.getStatusLine().getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    @Ignore
    public void testStatusVerbose() throws Exception {

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(getUrl() + "/verbose");

        try {
            HttpResponse response =  client.execute(get);
            int status = response.getStatusLine().getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
        } finally {
            get.releaseConnection();
        }
    }

}
