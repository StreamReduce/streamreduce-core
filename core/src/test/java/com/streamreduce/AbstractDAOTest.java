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
