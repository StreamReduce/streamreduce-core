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
