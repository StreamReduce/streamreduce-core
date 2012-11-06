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

import com.google.common.collect.Iterables;
import com.streamreduce.connections.AnalyticsProvider;
import com.streamreduce.connections.CloudProvider;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.FeedProvider;
import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.connections.MonitoringProvider;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.connections.SocialProvider;
import com.streamreduce.core.model.Account;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class ConnectionUtils {

    public static final Map<String, Class<? extends ConnectionProvider>> PROVIDER_MAP =
            new HashMap<String, Class<? extends ConnectionProvider>>();

    static {
        PROVIDER_MAP.put(CloudProvider.TYPE, CloudProvider.class);
        PROVIDER_MAP.put(FeedProvider.TYPE, FeedProvider.class);
        PROVIDER_MAP.put(GatewayProvider.TYPE, GatewayProvider.class);
        PROVIDER_MAP.put(MonitoringProvider.TYPE, MonitoringProvider.class);
        PROVIDER_MAP.put(ProjectHostingProvider.TYPE, ProjectHostingProvider.class);
        PROVIDER_MAP.put(AnalyticsProvider.TYPE, AnalyticsProvider.class);
        PROVIDER_MAP.put(SocialProvider.TYPE, SocialProvider.class);
    }

    /**
     * Returns an iterable of all {@link ConnectionProvider}, regardless of type.
     *
     * @return an iterable of supported provider objects, regardless of type
     */
    public static Iterable<? extends ConnectionProvider> getAllProviders() {
        Iterable<? extends ConnectionProvider> allProviders = null;

        for (String providerType : PROVIDER_MAP.keySet()) {
            Iterable<? extends ConnectionProvider> providers = getSupportedProviders(providerType);

            if (allProviders == null) {
                allProviders = providers;
            } else {
                allProviders = Iterables.concat(allProviders, providers);
            }
        }

        return allProviders;
    }

    /**
     * Returns an iterable of {@link ConnectionProvider} of a particular type or null if the provider type is invalid.
     *
     * @param providerType the type of the provider
     * @return an iterable of supported provider objects of a particular type
     */
    public static Iterable<? extends ConnectionProvider> getSupportedProviders(String providerType) {
        if (!PROVIDER_MAP.keySet().contains(providerType)) {
            return null;
        }
        return ServiceLoader.load(PROVIDER_MAP.get(providerType));
    }

    /**
     * Returns the provider with the given id and type or null if one cannot be found.
     *
     * @param providerType the type of the provider
     * @param providerId   the of the provider
     * @return - ConnectionProvider or null
     */
    public static ConnectionProvider getProviderFromId(String providerType, String providerId) {
        for (ConnectionProvider provider : getSupportedProviders(providerType)) {
            if (provider.getId().equals(providerId)) {
                return provider;
            }
        }

        return null;
    }

    public static boolean isBlacklisted(Account account, ObjectId connectionId) {
        return account.getPublicConnectionBlacklist().contains(connectionId);

    }

}
