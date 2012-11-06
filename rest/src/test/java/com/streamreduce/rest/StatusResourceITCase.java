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
