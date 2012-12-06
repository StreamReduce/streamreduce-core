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

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.model.Account;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profile;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;
import com.google.api.services.analytics.model.Webproperty;
import com.google.common.collect.Lists;
import com.streamreduce.connections.GoogleAnalyticsProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/4/12 13:29</p>
 */
public class GoogleAnalyticsClient extends ExternalIntegrationClient {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    private JacksonFactory jsonFactory = new JacksonFactory();
    private NetHttpTransport transport = new NetHttpTransport();
    private Analytics analytics;

    public GoogleAnalyticsClient(Connection connection) {
        super(connection);
    }

    @Override
    public void validateConnection() throws InvalidCredentialsException, IOException {
        List<JSONObject> profileIds = getProfiles();
        if (CollectionUtils.isEmpty(profileIds)) {
            throw new InvalidCredentialsException("You must have one or more profiles defined in Google Analytics in order to continue.");
        }
    }

    public List<JSONObject> getProfiles() throws IOException {
        Analytics analytics = initializeAnalytics();
        Accounts accounts = analytics.management().accounts().list().execute();

        List<JSONObject> jsonProfiles = Lists.newArrayList();

        if (accounts.getItems().isEmpty()) {
            System.err.println("No accounts found");
        }
        else {
            for (Account account : accounts.getItems()) {
                String accountId = account.getId();

                // Query webproperties collection.
                Webproperties webproperties = analytics.management().webproperties().list(accountId).execute();

                if (webproperties.getItems().isEmpty()) {
                    System.err.println("No Webproperties found");
                }
                else {
                    for (Webproperty webproperty : webproperties.getItems()) {
                        String webpropertyId = webproperty.getId();

                        // Query profiles collection.
                        Profiles profiles = analytics.management().profiles().list(accountId, webpropertyId).execute();
                        if (profiles.getItems().isEmpty()) {
                            System.err.println("No profiles found");
                        }
                        else {
                            for (Profile profile : profiles.getItems()) {
                                jsonProfiles.add(new JSONObjectBuilder()
                                        .add("id", profile.getId())
                                        .add("name", profile.getName())
                                        .build());
                            }
                        }
                    }
                }
            }
        }
        return jsonProfiles;
    }

    public JSONObject getProfileMetrics(String profileId) throws IOException {
        String formattedToday = DATE_TIME_FORMATTER.print(new DateTime());
        Analytics analytics = initializeAnalytics();
        GaData data = analytics.data().ga()
                .get("ga:" + profileId, formattedToday, formattedToday, "ga:visitors,ga:newVisits")
                .execute();

        if (CollectionUtils.isEmpty(data.getRows())) {
            return null;
        }


        JSONObjectBuilder builder = new JSONObjectBuilder();
        builder.add("id", profileId);
        int columnIndex = 0;
        for (GaData.ColumnHeaders header : data.getColumnHeaders()) {
            JSONObjectBuilder metric = new JSONObjectBuilder();
            String metricName = normalizeMetricName(header.getName());
            metric.add("metric", metricName);
            metric.add("id", profileId);
            Set<String> hashtags = new HashSet<>();
            hashtags.add(metricName.toLowerCase());

            for (List<String> row : data.getRows()) {
                for (int rowIndex = 0; rowIndex < row.size(); rowIndex++) {
                    if (rowIndex == columnIndex) {
                        metric.add("data", new Float(row.get(rowIndex)));
                    }
                }
            }

            hashtags.add("googleanalytics");
            metric.add("hashtags", hashtags);
            builder.append("metrics", metric.build());
            columnIndex++;
        }

        return builder.build();
    }

    public List<JSONObject> getAllProfileMetrics(Set<String> profileIds) throws IOException {
        List<JSONObject> allProfileMetrics = Lists.newArrayList();
        for (String profileId : profileIds) {
            allProfileMetrics.add(getProfileMetrics(profileId));
        }
        return allProfileMetrics;
    }

    private Analytics initializeAnalytics() {
        if (analytics == null) {
            GoogleAnalyticsProvider provider = new GoogleAnalyticsProvider();
            TokenResponse tokenResponse = new TokenResponse().setRefreshToken(getConnectionCredentials().getOauthRefreshToken());
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setJsonFactory(jsonFactory)
                    .setTransport(transport)
                    .setClientSecrets(provider.getClientId(), provider.getClientSecret())
                    .build()
                    .setFromTokenResponse(tokenResponse);
            analytics = new Analytics.Builder(transport, jsonFactory, credential).build();
        }
        return analytics;
    }

    private String normalizeMetricName(String name) {
        if (name.startsWith("ga:")) {
            return name.substring(3, name.length());
        }
        else {
            return name;
        }
    }
}
