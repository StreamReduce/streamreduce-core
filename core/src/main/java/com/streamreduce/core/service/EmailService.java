package com.streamreduce.core.service;

import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.SobaMessage;

import java.util.List;

import net.sf.json.JSONObject;

public interface EmailService {

    void setEnabled(boolean enabled);

    void sendPasswordResetEmail(User user, boolean mobile);

    // confirmation link
    void sendNewUserActivationEmail(User user);

    // confirmation link for invite
    void sendInviteUserActivationEmail(User user);

    // new user or invited user setup is done, let them know
    void sendUserAccountSetupCompleteEmail(User user);

    // new account has insights available
    void sendInsightsAvailableEmail(List<User> users);

    void sendBugReport(String username, String company, String summary, String details, String debugInfo);

//    void sendDirectMessageEmail(List<User> userList, SobaMessage sobaMessage);

    void sendCommentAddedEmail(Account account, SobaMessage message, MessageComment comment);

    void sendUserMessageAddedEmail(User sender, SobaMessage sobaMessage);

    void sendUserMessageEmail(User user, SobaMessage message, JSONObject payload);

    void sendConnectionBrokenEmail(Connection connection);
}
