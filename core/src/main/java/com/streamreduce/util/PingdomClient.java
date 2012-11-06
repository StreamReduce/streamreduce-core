package com.streamreduce.util;

import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client class used to interact with Pingdom.
 *
 * <p>Pingdom doesn't use authenticated sessions. You have to use the username, password and API key in every request.</p>
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/25/12 11:40</p>
 */
public class PingdomClient extends ExternalIntegrationClient {

    public static final String PINGDOM_HOST = "api.pingdom.com";
    public static final String PINGDOM_URL = "https://"+PINGDOM_HOST+"/api/2.0/";

    private String apiKey;
    private Connection connection;

    private DefaultHttpClient httpClient = new DefaultHttpClient();

    public PingdomClient(Connection connection) {
        super(connection);
        this.connection = connection;

        ConnectionCredentials credentials = connection.getCredentials();
        if (credentials == null) {
            throw new IllegalArgumentException("Connection must have username/password credentials.");
        }

        apiKey = credentials.getApiKey();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(PINGDOM_HOST, 443),
                new UsernamePasswordCredentials(getConnectionCredentials().getIdentity(),
                        getConnectionCredentials().getCredential()));
    }

    /**
     * Returns a list of JSONObjects representing the inventory for a given Pingdom account.
     *
     * @return list of JSONObjects or null
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public List<JSONObject> checks() throws IOException {
        JSONObject json = makeRequest("checks");
        // check for an error condition. not sure this will ever occur.
        if (!json.containsKey("checks")) {
            if (json.containsKey("error")) {
                throw new IOException("Encountered exception when retrieving Pingdom inventory: " +
                        json.getJSONObject("error").getString("errormessage"));
            }
            else {
                throw new IOException("Encountered unknown error when retrieving Pingdom inventory.");
            }
        }

        JSONArray checks = json.getJSONArray("checks");
        List<JSONObject> inventory = new ArrayList<JSONObject>();
        for (Iterator<JSONObject> i = checks.iterator(); i.hasNext();) {
            inventory.add(i.next());
        }
        return inventory;
    }

    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        validateCredentials();

        JSONObject json = makeRequest("servertime");
        // first look for a successful response.
        if (json.containsKey("servertime")) {
            return;
        }
        // otherwise we got back an error.
        else if (json.containsKey("error")) {
            JSONObject error = json.getJSONObject("error");
            throw new InvalidCredentialsException(error.getString("errormessage"));
        }
    }

    /**
     * Pingdom requires both username/password and an API key.
     *
     * @throws InvalidCredentialsException
     */
    private void validateCredentials() throws InvalidCredentialsException {
        ConnectionCredentials creds = connection.getCredentials();
        if (creds == null || (creds.getIdentity() == null || creds.getCredential() == null || creds.getApiKey() == null)) {
            throw new InvalidCredentialsException("Connection credentials require a username, password and API key.");
        }
    }

    private JSONObject makeRequest(String path) throws IOException {
        JSONObject jsonObject = new JSONObject();
        HttpGet method = new HttpGet(PINGDOM_URL + path);
        method.addHeader(new BasicHeader("App-Key", apiKey));
        try {
            HttpResponse response = httpClient.execute(method);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                jsonObject = readResponse(entity);
            }
        } catch (IOException e) {
            method.abort();
            throw e;
        }

        return jsonObject;
    }

    private JSONObject readResponse(HttpEntity entity) throws IOException {
        String response = EntityUtils.toString(entity);
        return JSONObject.fromObject(response);
    }

}
