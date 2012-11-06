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
