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

package com.streamreduce.core;

import com.google.code.morphia.Datastore;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.dao.SystemInfoDAO;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("applicationManager")
public class ApplicationManagerImpl implements ApplicationManager {

    @Autowired
    protected SecurityService securityService;
    @Autowired
    protected EmailService emailService;
    @Autowired
    protected MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private SystemInfoDAO systemInfoDAO;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EventService eventService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private OutboundStorageService outboundStorageService;
    @Autowired
    private ConnectionProviderFactory connectionProviderFactory;

    @Autowired
    @Qualifier(value = "businessDBDatastore")
    Datastore businessDBDatastore;
    @Autowired
    @Qualifier(value = "messageDBDatastore")
    Datastore messageDBDatastore;

    @Override
    public SystemInfo getSystemInfo() {
        return systemInfoDAO.getLatest();
    }

    @Override
    public SecurityService getSecurityService() {
        return securityService;
    }

    @Override
    public EmailService getEmailService() {
        return emailService;
    }

    @Override
    public MessageService getMessageService() {
        return messageService;
    }

    @Override
    public UserService getUserService() {
        return userService;
    }

    @Override
    public InventoryService getInventoryService() {
        return inventoryService;
    }

    @Override
    public ConnectionService getConnectionService() {
        return connectionService;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }

    public ConnectionProviderFactory getConnectionProviderFactory() {
        return connectionProviderFactory;
    }

    @Override
    public Datastore getBusinessDBDatastore() {
        return businessDBDatastore;
    }

    @Override
    public Datastore getMessageDBDatastore() {
        return messageDBDatastore;
    }

    @Override
    public SearchService getSearchService() {
        return searchService;
    }

    @Override
    public OutboundStorageService getOutboundStorageService() {
        return outboundStorageService;
    }
}
