package com.streamreduce.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkUtils {

    protected static Logger logger = LoggerFactory.getLogger(NetworkUtils.class);
    protected static int DEFAULT_TIMEOUT = 3000;


    /**
     * Must be run as root to really do ICMP, or else it falls back to a port 7 echo
     *
     * @param host   - ip
     * @param timout -
     * @return - true if reachable
     */
    public static boolean pingable(String host, int timout) {
        try {
            InetAddress a = InetAddress.getByName(host);
            logger.debug("[MONITOR UTIL] attempting to do native java ping   " + host);
            return a.isReachable(timout);
        } catch (UnknownHostException e) {
            logger.debug("[MONITOR UTIL] Unknown host of " + host + " " + e.getMessage());
        } catch (IOException e) {
            logger.debug("[MONITOR UTIL] IOException tying to ping " + host + " " + e.getMessage());
        }
        return false;
    }

    public static boolean ping(String host) {
        return pingable(host, DEFAULT_TIMEOUT);
    }

}
