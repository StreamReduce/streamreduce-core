package com.streamreduce.util;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/30/12 10:23</p>
 */
public class HTTPUtilsITest {

    public static final int PORT = 53911;
    public static final int SSL_PORT = 53913;

    private static Server server;

    @BeforeClass
    public static void setupClass() throws Exception {
        server = new Server();

        org.mortbay.jetty.security.SslSocketConnector sslSocketConnector = new org.mortbay.jetty.security.SslSocketConnector();
        sslSocketConnector.setPort(SSL_PORT);
        try {
            String keystorePath = HTTPUtilsITest.class.getResource("/com/nodeable/util/keystore").toString();
            sslSocketConnector.setKeystore(keystorePath);
            sslSocketConnector.setKeyPassword("nodeable");
            sslSocketConnector.setTruststore(keystorePath);
            sslSocketConnector.setTrustPassword("nodeable");
            server.addConnector(sslSocketConnector);

            SocketConnector socketConnector = new SocketConnector();
            socketConnector.setPort(PORT);
            server.addConnector(socketConnector);

            server.setHandler(new TestRequestHandler());
            server.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        server.stop();
    }

    @Test
    public void testRedirectCapability() throws Exception {
        String response = HTTPUtils.openUrl(
                String.format("http://localhost:%s/redirectsource", PORT),
                "GET", null, "text/plain", null, null, null, null);
        Assert.assertTrue(response.startsWith("<!DOCTYPE html>"));
    }

    private static class TestRequestHandler extends AbstractHandler {

        @Override
        public void handle(String target, HttpServletRequest req, HttpServletResponse res, int dispatch) throws IOException, ServletException {

            if (target.equals("/redirectsource")) {
                redirectSource(req, res);
            }
            else if (target.equals("/redirecttarget")) {
                redirectTarget(req, res);
            }
        }

        private void redirectTarget(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
            res.setContentType("text/html;charset=utf-8");
            res.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = res.getWriter();
            writer.write("REDIRECTTARGET SUCCESS");
            writer.flush();
            ((Request) req).setHandled(true);
        }

        /**
         * Either redirects the client to a local, non-HTTPS target or to HTTPS Yahoo. Dealer's choice.
         *
         * @param req
         * @param res
         * @throws IOException
         * @throws ServletException
         */
        private void redirectSource(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
            //res.sendRedirect(String.format("http://localhost:%s/redirecttarget", PORT));
            res.sendRedirect("https://www.yahoo.com");
            ((Request) req).setHandled(true);
        }

    }

}
