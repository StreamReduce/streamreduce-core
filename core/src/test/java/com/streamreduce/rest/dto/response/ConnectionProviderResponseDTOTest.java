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

package com.streamreduce.rest.dto.response;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.streamreduce.connections.ConnectionProvidersForTests;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertTrue;

public class ConnectionProviderResponseDTOTest {


    @Test
    public void testConnectionProviderResponseDTO_URLForGithubProjectHostingProvider() {
        ConnectionProviderResponseDTO dto = ConnectionProviderResponseDTO.toDTO(
                ConnectionProvidersForTests.GITHUB_PROVIDER, true);
        AuthTypeResponseDTO githubOauthAuthTypeDto = getOauthAuthTypeResponseDTO(dto);
        assertTrue(githubOauthAuthTypeDto.getOauthEndpoint().endsWith("/api/oauth/providers/github"));
    }

    @Test
    public void testConnectionProviderResponseDTO_URLForTwitterProvider() {
        ConnectionProviderResponseDTO dto = ConnectionProviderResponseDTO.toDTO(
                ConnectionProvidersForTests.TWITTER_PROVIDER, true);
        AuthTypeResponseDTO twitterOauthAuthTypeDto = getOauthAuthTypeResponseDTO(dto);
        assertTrue(twitterOauthAuthTypeDto.getOauthEndpoint().endsWith("/api/oauth/providers/twitter"));
    }

    private AuthTypeResponseDTO getOauthAuthTypeResponseDTO(ConnectionProviderResponseDTO dto) {
        return Iterables.find(dto.getAuthTypes().authTypes, new Predicate<AuthTypeResponseDTO>() {
            @Override
            public boolean apply(@Nullable AuthTypeResponseDTO input) {
                return (input != null && "OAUTH".equals(input.getType()));
            }
        });
    }
}
