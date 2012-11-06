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

import com.google.code.morphia.Datastore;
import com.mongodb.Mongo;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/19/12 16:23</p>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:core-config.xml",
        "classpath:test-datasource-config.xml"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractDAOTest {

    @Resource(name="businessDBDatastore")
    private Datastore businessDBDatastore;

    @After
    public void tearDown() throws Exception {
        Mongo mongo = businessDBDatastore.getMongo();
        mongo.dropDatabase("TEST_nodeabledb");
        mongo.dropDatabase("TEST_nodeablemsgdb");
    }

}
