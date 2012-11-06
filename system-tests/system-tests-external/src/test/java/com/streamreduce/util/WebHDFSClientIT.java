package com.streamreduce.util;

import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Integration tests for the WebHDFSClient.
 * <p/>
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/3/12 8:16 AM</p>
 */
public class WebHDFSClientIT {

    private static ResourceBundle webhdfsProperties = ResourceBundle.getBundle("webhdfs");
    private static String host = webhdfsProperties.getString("webhdfs.host");
    private static String port = webhdfsProperties.getString("webhdfs.port");
    private static OutboundConfiguration.Builder outboundConfigurationBuilder;
    private static String testFileUrl = String.format("http://%s:%s/webhdfs/v1/testWebHDFSClientIT%s", host, port, System.currentTimeMillis());

    @BeforeClass
    public static void setUp() throws Exception {
        outboundConfigurationBuilder = new OutboundConfiguration.Builder()
                .protocol("webhdfs")
                .destination(testFileUrl)
                .credentials(new ConnectionCredentials("hadoop", null))
                .dataTypes(OutboundDataType.EVENT); //doesn't matter in this context
        WebHDFSClient webHDFSClient = new WebHDFSClient(outboundConfigurationBuilder.build());
        webHDFSClient.createFile("some content".getBytes("UTF-8"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        WebHDFSClient webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(testFileUrl)
                        .credentials(new ConnectionCredentials("hadoop", null))
                        .build());
        Assert.assertTrue(webHDFSClient.delete(true));

        webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(String.format("http://%s:%s/webhdfs/v1/mkdirstest", host, port))
                        .credentials(new ConnectionCredentials("hadoop", null))
                        .dataTypes(OutboundDataType.EVENT) //doesn't matter in this context
                        .build());
        Assert.assertTrue(webHDFSClient.delete(true));
    }

    @Test
    public void testValidateConnection_fileNotFound() throws Exception {
        WebHDFSClient webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(String.format("http://%s:%s/webhdfs/v1/foo", host, port))
                        .credentials(new ConnectionCredentials("hadoop", null))
                        .build());
        try {
            webHDFSClient.validateConnection();
            fail("Should have encountered IOException and didn't.");
        }
        catch (IOException ioe) {
            Assert.assertTrue(ioe.getMessage().contains("File does not exist: /foo"));
        }
    }

    @Test
    public void testValidateConnection_invalidCredentials() throws Exception {
        WebHDFSClient webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(String.format("http://%s:%s/webhdfs/v1/", host, port))
                        .credentials(new ConnectionCredentials("root", null))
                        .build());
        try {
            webHDFSClient.validateConnection();
            fail("Should have encountered InvalidCredentialsException and didn't.");
        }
        catch (InvalidCredentialsException ice) {
            assertEquals(ice.getMessage(), "User root does not own the target destination.");
        }
    }

    @Test
    public void testReadFile() throws Exception {
        WebHDFSClient webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(testFileUrl)
                        .credentials(new ConnectionCredentials("hadoop", null))
                        .build());
        byte[] data = webHDFSClient.readFile();
        assertEquals(new String(data), "some content");
    }

    @Test
    public void testMkdirs() throws Exception {
        WebHDFSClient webHDFSClient = new WebHDFSClient(
                outboundConfigurationBuilder
                        .destination(String.format("http://%s:%s/webhdfs/v1/", host, port))
                        .credentials(new ConnectionCredentials("hadoop", null))
                        .build());
        Assert.assertFalse(webHDFSClient.exists("mkdirstest"));
        webHDFSClient.mkdirs("mkdirstest");
        Assert.assertTrue(webHDFSClient.exists("mkdirstest"));
    }

}
