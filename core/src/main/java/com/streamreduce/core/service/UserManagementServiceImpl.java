package com.streamreduce.core.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/3/12 21:26</p>
 */
@Component
@ManagedResource(objectName = "com.streamreduce.core.service:type=UserManagementService,name=user-management-srvc")
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    @Resource
    private UserService userService;

    private ObjectMapper objectMapper;

    public UserManagementServiceImpl() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @Override
    @ManagedOperation(description = "Returns a list of accounts as a JSON string.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "summary", description = "Returns summary results if true")})
    public String getAccounts(boolean summary) {
        List<Account> accounts = userService.getAccounts();
        return toJSON(accounts, summary);
    }

    @Override
    @ManagedOperation(description = "Returns a specific account as a JSON string.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "accountObjectId", description = "The ID of the account to return."),
            @ManagedOperationParameter(name = "summary", description = "Returns summary results if true.")})
    public String getAccount(String accountObjectId, boolean summary)
            throws AccountNotFoundException {

        Account account = userService.getAccount(new ObjectId(accountObjectId));
        return toJSON(account, summary);
    }

    @Override
    @ManagedOperation(description = "Returns a list of user for a specific account as a JSON string.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "accountObjectId", description = "Account ID."),
            @ManagedOperationParameter(name = "enabledUsersOnly", description = "Only returns enabled users if true."),
            @ManagedOperationParameter(name = "summary", description = "Returns summary results if true.")})
    public String getUsers(String accountObjectId, boolean enabledUsersOnly, boolean summary)
            throws AccountNotFoundException {

        Account account = userService.getAccount(new ObjectId(accountObjectId));
        List<User> users;
        if (enabledUsersOnly) {
            users = userService.allEnabledUsersForAccount(account);
        }
        else {
            users = userService.allUsersForAccount(account);
        }
        return toJSON(users, summary);
    }

    @Override
    @ManagedOperation(description = "Returns a specific account as a JSON string.")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "userObjectId", description = "The ID of the user to return."),
            @ManagedOperationParameter(name = "summary", description = "Returns summary results if true.")})
    public String getUser(String userObjectId, boolean summary)
            throws UserNotFoundException {
        User user = userService.getUserById(new ObjectId(userObjectId));
        return toJSON(user, summary);
    }

    private <T> String toJSON(List<T> list, boolean summary) {
        if (CollectionUtils.isEmpty(list)) {
            return "{'error' : 'No objects found.'}";
        }

        try {
            if (!summary) {
                return objectMapper.writeValueAsString(list);
            }
            else {
                List<Map<String, String>> summaries = Lists.newArrayList();
                for (T t : list) {
                    if (t instanceof User) {
                        summaries.add(toMap((User)t));
                    }
                    else {
                        summaries.add(toMap((Account)t));
                    }
                }
                return objectMapper.writeValueAsString(summaries);
            }
        }
        catch (IOException e) {
            LOGGER.error("Encountered exception", e);
            return String.format("{'error' : 'Encountered exception serializing users: %s'}", e.getMessage());
        }
    }

    private <T> String toJSON(Account account, boolean summary) {
        if (account == null) {
            return "{'error' : 'Account is null.'}";
        }

        try {
            if (summary) {
                return objectMapper.writeValueAsString(toMap(account));
            }
            else {
                return objectMapper.writeValueAsString(account);
            }
        }
        catch (IOException e) {
            LOGGER.error("Encountered exception", e);
            return String.format("{'error' : 'Encountered exception serializing connections: %s'}", e.getMessage());
        }
    }

    private <T> String toJSON(User user, boolean summary) {
        if (user == null) {
            return "{'error' : 'Account is null.'}";
        }

        try {
            if (summary) {
                return objectMapper.writeValueAsString(toMap(user));
            }
            else {
                return objectMapper.writeValueAsString(user);
            }
        }
        catch (IOException e) {
            LOGGER.error("Encountered exception", e);
            return String.format("{'error' : 'Encountered exception serializing connections: %s'}", e.getMessage());
        }
    }

    private Map<String, String> toMap(Account account) {
        Map<String, String> summary = Maps.newHashMap();
        summary.put("_id", account.getId().toString());
        summary.put("name", account.getName());
        summary.put("description", account.getDescription());
        summary.put("url", account.getUrl());
        return summary;
    }

    private Map<String, String> toMap(User user) {
        Map<String, String> summary = Maps.newHashMap();
        summary.put("_id", user.getId().toString());
        summary.put("alias", user.getAlias());
        summary.put("fullname", user.getFullname());
        summary.put("username", user.getUsername());
        summary.put("accountName", user.getAccount().getName());
        summary.put("accountId", user.getAccount().getId().toString());
        return summary;
    }
}
