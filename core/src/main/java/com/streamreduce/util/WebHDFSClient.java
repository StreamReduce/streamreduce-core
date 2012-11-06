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

package com.streamreduce.util;

import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * This class provides a client interface to WebHDFS instances.
 * <p/>
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/3/12 7:50 AM</p>
 */
public class WebHDFSClient extends ExternalIntegrationClient {

    private final String url;
    private final String username;

    private DefaultHttpClient httpClient = new DefaultHttpClient();

    public WebHDFSClient(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
        // Sometimes WebHDFS needs a trailing slash, other times it doesn't. Rather finicky.
        if (!outboundConfiguration.getDestination().endsWith("/")) {
            this.url = outboundConfiguration.getDestination()+"/";
        }
        else {
            this.url = outboundConfiguration.getDestination();
        }
        this.username = outboundConfiguration.getCredentials().getIdentity();

    }

    /**
     * Reads the contents of the target path in connection URL, returning a byte[].
     *
     * @param target the target path to be read.  If target is null or blank, it is ignored.
     * @return byte[]
     * @throws IOException if there is an error reading the file from the server
     */
    public byte[] readFile(String target) throws IOException {
        target = StringUtils.isNotBlank(target) ?  target : "";
        HttpGet method = new HttpGet(String.format("%s%s?op=OPEN&user.name=%s", url, target,username));

        byte[] responseBody = null;
        try {
            HttpResponse response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (status.getStatusCode() != 200) {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
                throw new IOException(String.format("Error returned from server. Status: %s - %s", status.getStatusCode(), status.getReasonPhrase()));
            }
            if (entity != null) {
                responseBody = EntityUtils.toByteArray(entity);
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        }

        return responseBody;
    }

    /**
     * Reads the contents of the connection URL, returning a byte[].
     *
     * @return byte[]
     * @throws IOException if there is an error reading the file from the server
     */
    public byte[] readFile() throws IOException {
        return readFile("");
    }

    /**
     * Creates a new file, represented by the connection URL, writing the contents to the new file.
     *
     * @param contents byte[]
     * @throws IOException if the file cannot be created or written to.
     */
    public void createFile(byte[] contents) throws IOException {
        writeToFile(null, "CREATE", contents);
    }

    /**
     * Creates a new file, represented by the connection URL, writing the contents to the new file.
     *
     * @param target   the destination file
     * @param contents byte[]
     * @throws IOException if the file cannot be created or written to.
     */
    public void createFile(String target, byte[] contents) throws IOException {
        writeToFile(target, "CREATE", contents);
    }

    /**
     * Appends to an existing file, represented by the connection URL.
     *
     * @param contents byte[]
     * @throws IOException if the file cannot be located or appended to.
     */
    public void appendToFile(byte[] contents) throws IOException {
        writeToFile(null, "APPEND", contents);
    }

    /**
     * Creates directories. The directories are created as children of the connection URL.
     *
     * @param path directory path to create
     * @return boolean indicating success or failure
     * @throws IOException if the server returns a non-200 response code.
     */
    public boolean mkdirs(String path) throws IOException {
        HttpPut method = new HttpPut(String.format("%s/%s?op=MKDIRS&user.name=%s", trimUrl(url), trimUrl(path), username));

        try {
            HttpResponse response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (status.getStatusCode() != 200) {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
                throw new IOException(String.format("Error returned from server. Status: %s - %s", status.getStatusCode(), status.getReasonPhrase()));
            }
            JSONObject responseBody = readResponse(entity);
            if (entity != null) {
                return responseBody.getBoolean("boolean");
            } else {
                return false;
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        }
    }

    /**
     * Returns true of the path exists as a child of the connection URL.
     *
     * @param path path to check
     * @return boolean
     * @throws IOException if the server returns a non-200 response code.
     */
    @SuppressWarnings("unchecked" )
    public boolean exists(String path) throws IOException {
        HttpGet method = new HttpGet(String.format("%s?op=LISTSTATUS&user.name=%s", url, username));

        try {
            HttpResponse response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (status.getStatusCode() != 200) {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
                throw new IOException(String.format("Error returned from server. Status: %s - %s", status.getStatusCode(), status.getReasonPhrase()));
            }
            if (entity != null) {
                JSONObject responseBody = readResponse(entity);
                JSONObject fileStatuses = responseBody.getJSONObject("FileStatuses" );
                for (Iterator<JSONObject> iter = fileStatuses.getJSONArray("FileStatus" ).iterator(); iter.hasNext(); ) {
                    JSONObject fileStatus = iter.next();
                    if (fileStatus.getString("pathSuffix" ).equals(path)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException e) {
            method.abort();
            throw e;
        }
    }

    /**
     * Deletes a file or directory, represented by the connection URL.
     *
     * @param recursive if true, the deletion is performed recursively
     * @return boolean
     * @throws IOException if a non-200 response code is returned.
     */
    public boolean delete(boolean recursive) throws IOException {
        HttpDelete method = new HttpDelete(String.format("%s?op=DELETE&user.name=%s&recursive=%s", url, username, recursive));

        try {
            HttpResponse response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (status.getStatusCode() != 200) {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
                throw new IOException(String.format("Error returned from server. Status: %s - %s", status.getStatusCode(), status.getReasonPhrase()));
            }
            if (entity != null) {
                JSONObject responseBody = readResponse(entity);
                return responseBody.getBoolean("boolean" );
            } else {
                return false;
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        }
    }

    /**
     * Attempts to validate connectivity to the underlying URL.
     *
     * @throws InvalidCredentialsException if the owner of the URL is different than the username supplied in the connection object.
     * @throws IOException                 if the server returns a RemoteException.
     */
    public void validateConnection() throws InvalidCredentialsException, IOException {
        validateUrl();
        HttpGet method = new HttpGet(String.format("%s?op=GETFILESTATUS&user.name=%s", url, username));
        try {
            HttpResponse response = httpClient.execute(method);
            HttpEntity entity = response.getEntity();

            if (entity == null){
                return;
            }
            JSONObject responseBody = readResponse(entity);
            /*
               Right now I'm handling FileNotFoundExceptions as validation failures.
               Should we try to create the file/directory for the user? @NJH
            */
            if (responseBody.get("RemoteException" ) != null) {
                JSONObject remoteException = responseBody.getJSONObject("RemoteException" );
                throw new IOException(remoteException.getString("message" ));
            } else if (responseBody.get("FileStatus" ) != null) {
                JSONObject fileStatus = responseBody.getJSONObject("FileStatus" );
                /*
                   Since the file exists, does the user own it?
                */
                if (!fileStatus.get("owner" ).equals(username)) {
                    throw new InvalidCredentialsException(String.format("User %s does not own the target destination.", username));
                }
                /*
                   We can't write to a directory.
                   NOTE: This is commented out for now since I'm not sure it's necessary behavior. @NJH
               else if (fileStatus.get("type").equals("DIRECTORY")) {
                   throw new IOException("Target destination is a directory. You must specify a file.");
               }
                */
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        }
    }

    private void validateUrl() throws IllegalArgumentException {
        try {
            URL target = new URL(url);
            if (!target.getPath().contains("webhdfs/v1")) {
                throw new IllegalStateException("The destination URL was invalid: It must contain the path 'webhdfs/v1'.");
            }
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException(String.format("The destination URL was invalid: %s", e.getMessage()));
        }
    }

    private JSONObject readResponse(HttpEntity entity) throws IOException {
        String response = EntityUtils.toString(entity);
        return JSONObject.fromObject(response);
    }

    private HttpEntityEnclosingRequestBase getWriteMethod(String operation, String url) {
        if (operation.equals("CREATE" )) {
            return new HttpPut(url);
        } else {
            return new HttpPost(url);
        }
    }

    private void writeToFile(String target, String operation, byte[] contents) throws IOException {
        String targetUrl = target == null ?
                String.format("%s?op=%s&user.name=%s", url, operation, username) :
                String.format("%s/%s?op=%s&user.name=%s", trimUrl(url), target, operation, username);
        HttpEntityEnclosingRequestBase method = getWriteMethod(operation, targetUrl);

        int responseCode;
        HttpResponse response = null;
        try {
            response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 307) {
                throw new IOException(String.format("Error returned from server. Status: %s - %s", responseCode, status.getReasonPhrase()));
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        } finally {
            method.releaseConnection();
        }

        try {
            Header redirectUrl = response.getFirstHeader("Location");
            if (redirectUrl == null) {
                throw new IOException(String.format("Location header, indicating data node location, is null. Cannot continue appending to %s.", url));
            }

            method = getWriteMethod(operation, redirectUrl.getValue());
            method.setEntity(new ByteArrayEntity(contents));
            response = httpClient.execute(method);
            StatusLine status = response.getStatusLine();
            responseCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consume(entity);
            }
            if (responseCode != 200 && responseCode != 201) {
                throw new IOException(String.format("Error returned from server. Status: %s - %s; %s", responseCode, status.getReasonPhrase(), readResponse(entity)));
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        } finally {
            method.releaseConnection();
        }
    }

    private String trimUrl(String connectionUrl) {
        if (connectionUrl.endsWith("/" )) {
            return connectionUrl.substring(0, (connectionUrl.length() - 1));
        } else {
            return connectionUrl;
        }
    }
}
