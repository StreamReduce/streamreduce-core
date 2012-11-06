package com.streamreduce.core.service;

import com.streamreduce.core.model.Account;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.streamreduce.core.model.messages.SobaMessage;
import net.sf.json.JSONObject;

public interface SearchService {

    URL createRiverForAccount(Account account);

    void createRiversForAccounts(List<Account> accounts);

    List<SobaMessage> searchMessages(Account account, String resourceName, Map<String, String> searchParameters,
                              JSONObject query);

    JSONObject makeRequest(String path, JSONObject payload, Map<String, String> urlParameters, String method);



}
