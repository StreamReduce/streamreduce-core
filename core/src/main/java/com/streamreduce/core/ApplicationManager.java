package com.streamreduce.core;

import com.google.code.morphia.Datastore;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.model.SystemInfo;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.EmailService;
import com.streamreduce.core.service.EventService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.MessageService;
import com.streamreduce.core.service.OutboundStorageService;
import com.streamreduce.core.service.SearchService;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.core.service.UserService;

public interface ApplicationManager {

    SystemInfo getSystemInfo();

    SecurityService getSecurityService();

    EmailService getEmailService();

    MessageService getMessageService();

    UserService getUserService();

    InventoryService getInventoryService();

    ConnectionService getConnectionService();

    EventService getEventService();

    SearchService getSearchService();

    OutboundStorageService getOutboundStorageService();

    ConnectionProviderFactory getConnectionProviderFactory();

    // raw Datastore access
    Datastore getBusinessDBDatastore();

    Datastore getMessageDBDatastore();

}
