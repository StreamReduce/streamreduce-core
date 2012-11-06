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

package com.streamreduce.core.model;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.dao.ConnectionDAO;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static com.streamreduce.connections.AuthType.NONE;
import static com.streamreduce.connections.AuthType.USERNAME_PASSWORD;
import static org.junit.Assert.assertEquals;


public class ConnectionPrePersistITCase extends AbstractServiceTestCase {
    
    Connection connection;
    
    @Autowired
    ConnectionDAO connectionDAO;
    @Autowired
    ConnectionProviderFactory connectionProviderFactory;
            
    @After
    public void tearDown() throws Exception {
        try {
            if (connection != null) {
                connectionDAO.delete(connection);
            }
        } catch (Exception e) {
            logger.warn("Connection not deleted",e);
        }
        super.tearDown();
    }
    
    @Test
    public void testConnectionPrePersistGitHubProvider() {
        //Tests that the proper default authType is set for a connection with github has the providerId
        Connection c = new Connection.Builder()
                .alias("test github")
                .description("test github")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.GITHUB_PROVIDER_ID))
                .user(getTestUser())
                .authType(NONE)
                .build();
        ReflectionTestUtils.setField(c, "authType", null);
        connectionDAO.save(c);
        assertEquals(AuthType.USERNAME_PASSWORD,c.getAuthType());
    }

    @Test
    public void testConnectionPrePersistJiraProvider() {
        //Tests that the proper default authType is set for a connection with github has the providerId
        Connection c = new Connection.Builder()
                .alias("test jira")
                .description("test jira")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.JIRA_PROVIDER_ID))
                .user(getTestUser())
                .authType(NONE)
                .build();
        ReflectionTestUtils.setField(c, "authType", null);
        connectionDAO.save(c);
        assertEquals(AuthType.USERNAME_PASSWORD,c.getAuthType());
    }

    @Test
    public void testConnectionPrePersistAwsProvider() {
        //Tests that the proper default authType is set for a connection with github has the providerId
        Connection c = new Connection.Builder()
                .alias("test aws")
                .description("test aws")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.AWS_PROVIDER_ID))
                .user(getTestUser())
                .authType(NONE)
                .build();
        ReflectionTestUtils.setField(c, "authType", null);
        connectionDAO.save(c);
        assertEquals(AuthType.USERNAME_PASSWORD,c.getAuthType());
    }

    @Test
    public void testConnectionPrePersistRssProvider() {
        //Tests that the proper default authType is set for a connection with github has the providerId
        Connection c = new Connection.Builder()
                .alias("test rss")
                .description("test rss")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.FEED_PROVIDER_ID))
                .user(getTestUser())
                .authType(USERNAME_PASSWORD)
                .build();
        ReflectionTestUtils.setField(c, "authType", null);
        connectionDAO.save(c);
        assertEquals(NONE,c.getAuthType());
    }
}
