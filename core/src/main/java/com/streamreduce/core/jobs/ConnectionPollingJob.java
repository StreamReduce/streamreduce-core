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

package com.streamreduce.core.jobs;

import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.streamreduce.connections.CloudProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.FeedProvider;
import com.streamreduce.connections.GoogleAnalyticsProvider;
import com.streamreduce.connections.MonitoringProvider;
import com.streamreduce.connections.ProjectHostingProvider;
import com.streamreduce.connections.TwitterProvider;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.EmailService;
import com.streamreduce.core.service.exception.InvalidCredentialsException;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ManagedResource(objectName="com.streamreduce.core.jobs:type=ConnectionPollingJob,name=connection-polling-job-mgmt", currencyTimeLimit = 15)
public class ConnectionPollingJob {

    private final Timer REFRESH_TIMER = Metrics.newTimer(ConnectionPollingJob.class,
                                                                "connection-polling-job-metrics", TimeUnit.SECONDS,
                                                                TimeUnit.MINUTES);
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${cloud.polling.job.interval}")
    private long cloudPollingJobInterval;
    @Value("${projecthosting.polling.job.interval}")
    private long projectHostingPollingJobInterval;
    @Value("${feed.polling.job.interval}")
    private long feedPollingJobInterval;
    @Value("${monitoring.polling.job.interval}")
    private long monitoringPollingJobInterval;
    @Value("${twitter.polling.job.interval}")
    private long twitterPollingJobInterval;
    @Value("${googleanalytics.polling.job.interval}")
    private long googleAnalyticsPollingJobInterval;

    @Value("${connection.polling.job.max.failed.count}")
    private long pollingJobMaxFailedCount;
    @Value("${connection.polling.job.broken.sleep.time}")
    private long pollingJobBrokenSleepTime;
    @Value("${connection.polling.job.bootstrap.batch.size}")
    private long pollingJobBootstrapBatchSize;
    @Value("${nodeable.polling.enabled}")
    private boolean pollingEnabled;


    @Autowired
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionDAO connectionDAO;
    @Autowired
    private EmailService emailService;

    /**
     * Refreshes the inventory cache for appropriate connection objects.
     */
    @Scheduled(fixedRate = 30000)
    @ManagedOperation(description = "Refreshes the inventory cache for appropriate connection objects")
    public void execute() {

        if (!pollingEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        List<Connection> connectionsNeedingPolling = new ArrayList<Connection>();
        List<Connection> allConnections = connectionService.getConnections(null);
        for (Connection connection : allConnections) {
            if (connectionProviderFactory.pushConnectionProvider(connection.getProviderId()) != null || connection.isBroken() || connection.isDisabled()) {
                continue;
            }
            long interval = getPollingIntervalForConnection(connection);
            long elapsedTimeSinceLastRun = now - connection.getPollingLastExecutionTime();
            boolean pollingNeeded = !connection.isPollingInProgress() && (connection.getPollingFailedCount() <= pollingJobMaxFailedCount) && (elapsedTimeSinceLastRun > interval);
            boolean pollingTakingTooLong = connection.isPollingInProgress() && (elapsedTimeSinceLastRun > (5 * interval));
            boolean pollingPastSleepingPeriod = !connection.isPollingInProgress() && (connection.getPollingFailedCount() > pollingJobMaxFailedCount) && (elapsedTimeSinceLastRun > pollingJobBrokenSleepTime);
            if (pollingNeeded || pollingTakingTooLong || pollingPastSleepingPeriod) {
                connectionsNeedingPolling.add(connection);
                if (connectionsNeedingPolling.size() == pollingJobBootstrapBatchSize) {
                    break;
                }
            }
        }
        for (final Connection connection : connectionsNeedingPolling) {
            logger.info("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] last polled at " + new Date(connection.getPollingLastExecutionTime()) + ", refreshing");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    refresh(connection);
                }
            });
        }
    }

    private long getPollingIntervalForConnection(Connection connection) {
        String providerType = connection.getType();
        long interval;
        if (providerType.equals(CloudProvider.TYPE)) {
            interval = cloudPollingJobInterval;
        } else if (providerType.equals(ProjectHostingProvider.TYPE)) {
            interval = projectHostingPollingJobInterval;
        } else if (providerType.equals(GoogleAnalyticsProvider.TYPE)) {
            interval = googleAnalyticsPollingJobInterval;
        } else if (providerType.equals(FeedProvider.TYPE)) {
            interval = feedPollingJobInterval;
        } else if (providerType.equals(MonitoringProvider.TYPE)) {
            interval = monitoringPollingJobInterval;
        } else if (providerType.equals(TwitterProvider.TYPE)) {
            interval = twitterPollingJobInterval;
        } else {
            logger.error("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] unable to get the schedule interval for the polling job of providerType " + connection.getType());
            interval = Long.MAX_VALUE;
        }
        return interval;
    }

    private void refresh(Connection connection) {
        long now = System.currentTimeMillis();
        try {
            logger.info("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] polling job started");
            Query<Connection> updateQuery = connectionDAO.createQuery().field(Mapper.ID_KEY).equal(connection.getId());
            UpdateOperations<Connection> ops = connectionDAO.createUpdateOperations().set("pollingInProgress", true).set("pollingLastExecutionTime", now);
            connectionDAO.update(updateQuery, ops);
            // connection fields have to be updated because connection is saved again in the inventory refresh process
            connection.setPollingInProgress(true);
            connection.setPollingLastExecutionTime(now);
            connectionService.fireOneTimeHighPriorityJobForConnection(connection);
            logger.info("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] " + connection.getType() + " polling job finished (" + (System.currentTimeMillis() - now) + " ms)");
            ops = connectionDAO.createUpdateOperations().set("pollingInProgress", false).unset("pollingFailedCount");
            connectionDAO.update(updateQuery, ops);
        } catch (InvalidCredentialsException e) {
            logger.error("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] authentication failure in polling job, set as broken", e);
            Query<Connection> updateQuery = connectionDAO.createQuery().field(Mapper.ID_KEY).equal(connection.getId());
            UpdateOperations<Connection> ops = connectionDAO.createUpdateOperations().set("pollingInProgress", false).set("broken", true).set("lastErrorMessage", e.getMessage());
            connectionDAO.update(updateQuery, ops);
            emailService.sendConnectionBrokenEmail(connection);
        } catch (Exception e) {
            logger.error("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] " + connection.getType() + " polling job borked (" + (System.currentTimeMillis() - now) + " ms)", e);
            Query<Connection> updateQuery = connectionDAO.createQuery().field(Mapper.ID_KEY).equal(connection.getId());
            UpdateOperations<Connection> ops = connectionDAO.createUpdateOperations().set("pollingInProgress", false).inc("pollingFailedCount");
            connectionDAO.update(updateQuery, ops);
            long failedCount = connection.getPollingFailedCount() + 1;
            if (failedCount > pollingJobMaxFailedCount) {
                logger.error("[JOB] Connection " + connection.getId() + " [" + connection.getAlias() + "] failed count exceeded maximum value " + pollingJobMaxFailedCount + ", sleeping");
            }
        }
    }

    @ManagedAttribute(description = "Cloud Polling Job Interval")
    public long getCloudPollingJobInterval() {
        return cloudPollingJobInterval;
    }

    @ManagedAttribute(description = "Cloud Polling Job Interval")
    public void setCloudPollingJobInterval(long cloudPollingJobInterval) {
        this.cloudPollingJobInterval = cloudPollingJobInterval;
    }

    @ManagedAttribute(description = "Project Hosting Polling Job Interval")
    public long getProjectHostingPollingJobInterval() {
        return projectHostingPollingJobInterval;
    }

    @ManagedAttribute(description = "Project Hosting Polling Job Interval")
    public void setProjectHostingPollingJobInterval(long projectHostingPollingJobInterval) {
        this.projectHostingPollingJobInterval = projectHostingPollingJobInterval;
    }

    @ManagedAttribute(description = "Google Analytics Polling Job Interval")
    public long getGoogleAnalyticsPollingJobInterval() {
        return googleAnalyticsPollingJobInterval;
    }

    @ManagedAttribute(description = "Google Analytics Polling Job Interval")
    public void setGoogleAnalyticsPollingJobInterval(long googleAnalyticsPollingJobInterval) {
        this.googleAnalyticsPollingJobInterval = googleAnalyticsPollingJobInterval;
    }

    @ManagedAttribute(description = "Feed Polling Job Interval")
    public long getFeedPollingJobInterval() {
        return feedPollingJobInterval;
    }

    @ManagedAttribute(description = "Feed Polling Job Interval")
    public void setFeedPollingJobInterval(long feedPollingJobInterval) {
        this.feedPollingJobInterval = feedPollingJobInterval;
    }

    @ManagedAttribute(description = "Monitoring Polling Job Interval")
    public long getMonitoringPollingJobInterval() {
        return monitoringPollingJobInterval;
    }

    @ManagedAttribute(description = "Monitoring Polling Job Interval")
    public void setMonitoringPollingJobInterval(long monitoringPollingJobInterval) {
        this.monitoringPollingJobInterval = monitoringPollingJobInterval;
    }

    @ManagedAttribute(description = "Twitter Polling Job Interval")
    public long getTwitterPollingJobInterval() {
        return twitterPollingJobInterval;
    }

    @ManagedAttribute(description = "Twitter Polling Job Interval")
    public void setTwitterPollingJobInterval(long twitterPollingJobInterval) {
        this.twitterPollingJobInterval = twitterPollingJobInterval;
    }

    @ManagedAttribute(description = "Polling Job Max Failed Count")
    public long getPollingJobMaxFailedCount() {
        return pollingJobMaxFailedCount;
    }

    @ManagedAttribute(description = "Polling Job Max Failed Count")
    public void setPollingJobMaxFailedCount(long pollingJobMaxFailedCount) {
        this.pollingJobMaxFailedCount = pollingJobMaxFailedCount;
    }

    @ManagedAttribute(description = "Polling Job Broken Sleep Time")
    public long getPollingJobBrokenSleepTime() {
        return pollingJobBrokenSleepTime;
    }

    @ManagedAttribute(description = "Polling Job Broken Sleep Time")
    public void setPollingJobBrokenSleepTime(long pollingJobBrokenSleepTime) {
        this.pollingJobBrokenSleepTime = pollingJobBrokenSleepTime;
    }

    @ManagedAttribute(description = "Polling Job Bootstrap Batch Size")
    public long getPollingJobBootstrapBatchSize() {
        return pollingJobBootstrapBatchSize;
    }

    @ManagedAttribute(description = "Polling Job Bootstrap Batch Size")
    public void setPollingJobBootstrapBatchSize(long pollingJobBootstrapBatchSize) {
        this.pollingJobBootstrapBatchSize = pollingJobBootstrapBatchSize;
    }
}
