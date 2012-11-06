package com.streamreduce.core.model;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.dao.ConnectionDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.ConstraintViolationException;

/**
 * Integration Test that verifies validation works on saving Connections with ConnectionDAO.
 */
public class ConnectionValidationITCase extends AbstractServiceTestCase {

    private static final String SAMPLE_FEED_FILE_PATH = ConnectionValidationITCase.class.getResource(
            "/com/nodeable/rss/sample_EC2.rss").toString();

    @Autowired
    ConnectionDAO connectionDAO;

    @Test
    public void testCreateConnectionHappyPath() throws Exception {
        //Tests creating a connection works
        connectionDAO.save(instantiateValidConnection());
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsEmptyProviderId() throws Exception {
        Connection c = instantiateValidConnection();
        c.setProviderId("    ");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsInvalidProviderId() throws Exception {
        Connection c = instantiateValidConnection();
        c.setProviderId("adsadfasf");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsEmptyType() throws Exception {
        Connection c = instantiateValidConnection();
        c.setType("   ");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsInvalidType() throws Exception {
        Connection c = instantiateValidConnection();
        c.setType("adfasfasf");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsInvalidURL() throws Exception {
        Connection c = instantiateValidConnection();
        c.setUrl("really this is not a URL");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateConnectionFailsNullAccount() throws Exception {
        Connection c = instantiateValidConnection();
        c.setAccount(null);
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEmptyAlias() throws Exception {
        Connection c = instantiateValidConnection();
        ReflectionTestUtils.setField(c,"alias","");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testBlankAlias() throws Exception {
        Connection c = instantiateValidConnection();
        ReflectionTestUtils.setField(c, "alias", "      ");
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testMaxLengthAlias() throws Exception {
        Connection c = instantiateValidConnection();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 129; i++) {
            sb.append("a");
        } //string of 129 length
        c.setAlias(sb.toString());
        connectionDAO.save(c);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testNullAccount() throws Exception {
        Connection c = instantiateValidConnection();
        c.setAccount(null);
        connectionDAO.save(c);
    }

    private Connection instantiateValidConnection() {
        return new Connection.Builder()
                .alias("rss")
                .description("rss")
                .visibility(SobaObject.Visibility.PUBLIC)
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .user(testUser)
                .credentials(null)
                .url(SAMPLE_FEED_FILE_PATH)
                .authType(AuthType.NONE)
                .build();
    }
}
