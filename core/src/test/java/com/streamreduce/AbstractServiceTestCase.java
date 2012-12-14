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

package com.streamreduce;

import com.mongodb.Mongo;
import com.streamreduce.core.ApplicationManager;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.SearchServiceImpl;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.datasource.BootstrapDatabaseDataPopulator;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ResourceBundle;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:core-config.xml",
        "classpath:test-datasource-config.xml",
        "classpath:camel-config.xml",
        "classpath:test-config.xml"}
)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractServiceTestCase {

    public final ResourceBundle cloudProperties = ResourceBundle.getBundle("cloud");
    public final ResourceBundle gitHubProperties = ResourceBundle.getBundle("github");
    public final ResourceBundle jiraProperties = ResourceBundle.getBundle("jira");
    public final ResourceBundle twitterProperties = ResourceBundle.getBundle("twitter");

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ApplicationManager applicationManager;
    @Autowired
    protected UserService userService;
    @Autowired
    protected SecurityService securityService;
    @Autowired
    protected ConnectionService connectionService;
    @Autowired
    private SearchServiceImpl searchServiceImpl;
    @Autowired
    private BootstrapDatabaseDataPopulator bootstrapDatabaseDataPopulator;


    protected User testUser;
    protected Account testAccount;

    @Before
    public void setUp() throws Exception {
        searchServiceImpl.setEnabled(false);
        createTestUser();
    }

    private void createTestUser() {
        String testUsername = "test@nodeable.com";

        try {
            testUser = userService.getUser(testUsername);
            testAccount = testUser.getAccount();
        } catch (UserNotFoundException e) {
            Account account = new Account.Builder()
                    .url("http://nodeable.com")
                    .description("Nodeable Test Account")
                    .name("Nodeable Test")
                    .build();
            testAccount = userService.createAccount(account);

            testUser = userService.createUser(
                    new User.Builder()
                            .username(testUsername)
                            .accountLocked(false)
                            .fullname("Test User")
                            .account(testAccount)
                            .roles(userService.getAdminRoles())
                            .accountOriginator(true)
                            .alias("test")
                            .password(testUsername)
                            .build()
            );
        }
    }

    @After
    public void tearDown() throws Exception {
        Mongo mongo = applicationManager.getMessageDBDatastore().getMongo();
        mongo.dropDatabase("TEST_nodeabledb");
        mongo.dropDatabase("TEST_nodeablemsgdb");
    }

    public User getTestUser() {
        return testUser;
    }

    public Account getTestAccount() {
        return testAccount;
    }
}
