package com.streamreduce.core.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.service.exception.UserNotFoundException;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import net.sf.json.JSONObject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("emailService")
public class EmailServiceImpl extends AbstractService implements EmailService, InitializingBean {

    private static final String DEFAULT_ENCODING = "UTF-8";

    @Resource(name = "emailProperties")
    protected Properties emailProperties;
    @Resource(name = "simpleEmailServiceClient")
    protected AmazonSimpleEmailServiceClient simpleEmailServiceClient;
    @Resource(name = "userService")
    protected UserService userService;
    @Value("${email.enabled}")
    protected boolean enabled;
    /*
     * This is basically an undocumented function to allow unit testing to continue when false.
     */
    @Value("${email.background.send:true}")
    protected boolean backgroundSend = true;

    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    @Override
    public void afterPropertiesSet() throws Exception {
        //Load templates from classpath for Velocity and cache them
        Velocity.setProperty(Velocity.RESOURCE_LOADER, "class");
        Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);

        //Use log4j for logging in Velocity (otherwise, it uses its own)
        Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
        Velocity.setProperty("runtime.log.logsystem.log4j.logger", logger.getName());

        Velocity.init();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void sendPasswordResetEmail(User user, boolean mobile) {
        try {
            String userId = URLEncoder.encode(user.getId().toString(), DEFAULT_ENCODING);
            String key = URLEncoder.encode(user.getSecretKey(), DEFAULT_ENCODING);
            String secretKey = key + "/" + userId;

            String from = (String) emailProperties.get("email.from");
            String urlPrefix = mobile ? (String) emailProperties.get("email.mobile.urlprefix") : (String) emailProperties.get("email.urlprefix");
            String subject = (String) emailProperties.get("email.resetPassword.subject");

            VelocityContext context = new VelocityContext();
            context.put("urlPrefix", urlPrefix);
            context.put("secretKey", secretKey);

            logger.debug("[SES EMAIL] sending password reset email for " + user.getUsername());

            // send the email
            sendEmail(user.getUsername(), from, subject, "password_reset_email", context);

        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void sendNewUserActivationEmail(User user) {
        try {

            // body
            String id = URLEncoder.encode(user.getId().toString(), DEFAULT_ENCODING);
            String key = URLEncoder.encode(user.getSecretKey(), DEFAULT_ENCODING);
            // append to the url
            String secretyKey = key + "/" + id;

            String from = (String) emailProperties.get("email.from");
            String urlPrefix = (String) emailProperties.get("email.urlprefix");
            String subject = (String) emailProperties.get("email.newUser.subject");

            VelocityContext context = new VelocityContext();
            context.put("urlPrefix", urlPrefix);
            context.put("secretKey", secretyKey);

            logger.debug("[SES EMAIL] send new user activation email for " + user.getUsername());

            // send the email
            sendEmail(user.getUsername(), from, subject, "new_user_activation_email", context);

        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
    }


    @Override
    public void sendInviteUserActivationEmail(User user) {
        try {
            String id = URLEncoder.encode(user.getAccount().getId().toString(), DEFAULT_ENCODING);
            String key = URLEncoder.encode(user.getSecretKey(), DEFAULT_ENCODING);
            String secretyKey = key + "/" + id;

            String from = (String) emailProperties.get("email.from");
            String urlPrefix = (String) emailProperties.get("email.urlprefix");
            String subject = (String) emailProperties.get("email.inviteUser.subject");

            VelocityContext context = new VelocityContext();
            context.put("urlPrefix", urlPrefix);
            context.put("secretKey", secretyKey);

            logger.debug("[SES EMAIL] send invited user activation email for " + user.getUsername());

            sendEmail(user.getUsername(), from, subject, "invite_user_activation_email", context);

        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void sendUserAccountSetupCompleteEmail(User user) {
        String from = (String) emailProperties.get("email.from");
        String subject = (String) emailProperties.get("email.userSetup.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");
        VelocityContext context = new VelocityContext();
        context.put("urlPrefix", urlPrefix);

        logger.debug("[SES EMAIL] send account setup complete email for " + user.getUsername());

        sendEmail(user.getUsername(), from, subject, "user_account_setup_complete_email", context);
    }


    @Override
    public void sendInsightsAvailableEmail(List<User> users) {
        String from = (String) emailProperties.get("email.from");
        String subject = (String) emailProperties.get("email.accountInsight.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");
        VelocityContext context = new VelocityContext();
        context.put("urlPrefix", urlPrefix);

        for (User user : users) {
            logger.debug("[SES EMAIL] send new account insights are available email for " + user.getUsername());
            sendEmail(user.getUsername(), from, subject, "new_account_insight_available", context);
        }
    }

    @Override
    public void sendBugReport(String username, String company, String summary, String details, String debugInfo) {

        // from support
        String to = (String) emailProperties.get("email.support.from");
        String subject = "[BugTracker] " + summary;

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("uuid", UUID.randomUUID().toString());
        velocityContext.put("dateCreated", new Date().toString());
        velocityContext.put("company", company);
        velocityContext.put("username", username);
        velocityContext.put("summary", summary);
        velocityContext.put("details", details);
        velocityContext.put("debugInfo", debugInfo);

        sendEmail(to, username, subject, "bug_report_email", velocityContext);
    }

//    @Override
//    public void sendDirectMessageEmail(List<User> userList, SobaMessage sobaMessage) {
//
//        String from = (String) emailProperties.get("email.from");
//        String subject = "Nodable: direct message test";
//
//        VelocityContext velocityContext = new VelocityContext();
//        velocityContext.put("sobaMessage", sobaMessage);
//
//        String body = createBodyFromTemplate("/templates/direct_message_email_html.vm", velocityContext);
//
//        logger.debug("[SES EMAIL] send direct message notification");
//
//        for (User user : userList) {
//            // don't send a copy to the user who created it.
//            if (!sobaMessage.getSenderId().equals(user.getId())) {
//                sendEmail(user.getUsername(), from, subject, body);
//            }
//        }
//    }

    @Override
    public void sendCommentAddedEmail(Account account, SobaMessage sobaMessage, MessageComment comment) {
        User owner = null;
        if (sobaMessage.getOwnerId() != null) {
            try {
                owner = userService.getUserById(sobaMessage.getOwnerId());
            }
            catch (UserNotFoundException e) {
                logger.error("User not found for ID {}. Cannot send email.", sobaMessage.getOwnerId());
                return;
            }
        }

        String from = (String) emailProperties.get("email.noreply.from");
        String subject = (String) emailProperties.get("email.comment.added.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");

        // try to use a sensible default if the details or title are null
        String title = "User Comment";
        if (sobaMessage.getDetails() != null) {
            AbstractMessageDetails details = (AbstractMessageDetails) sobaMessage.getDetails();
            if (details.getTitle() != null) {
                title = details.getTitle();
            }
        }

        VelocityContext context = new VelocityContext();
        context.put("messageId", sobaMessage.getId());
        context.put("commenter", comment.getFullName());
        context.put("messageType", getFriendlyMessageTypeLabel(sobaMessage.getType()));
        context.put("urlPrefix", urlPrefix);
        context.put("title", title);
        context.put("comment", comment.getComment());

        List<User> enabledUsers = userService.allEnabledUsersForAccount(account);
        for (User user : enabledUsers) {
            // don't send if the commenter is the owner or if the user has elected to not receive notifications
            if (owner == null || !user.getId().equals(owner.getId()) || !receivesCommentNotifications(user)) {
                sendEmail(user.getUsername(), from, subject, "comment_added", context);
            }
        }
    }

    @Override
    public void sendUserMessageAddedEmail(User sender, SobaMessage sobaMessage) {
        String from = (String) emailProperties.get("email.noreply.from");
        String subject = (String) emailProperties.get("email.user.message.added.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");

        VelocityContext context = new VelocityContext();
        context.put("sender", sender.getFullname());
        context.put("messageId", sobaMessage.getId());
        context.put("messageType", getFriendlyMessageTypeLabel(sobaMessage.getType()));
        context.put("urlPrefix", urlPrefix);
        context.put("message", sobaMessage.getTransformedMessage());

        List<User> enabledUsers = userService.allEnabledUsersForAccount(sender.getAccount());
        for (User user : enabledUsers) {
            // don't send if the sender is the owner or if the user has elected to not receive notifications
            if (!user.getId().equals(sender.getId()) && receivesNewMessageNotifications(user)) {
                sendEmail(user.getUsername(), from, subject, "user_message_added", context);
            }
        }
    }

    @Override
    public void sendUserMessageEmail(User sender, SobaMessage sobaMessage, JSONObject payload) {
        String from = (String) emailProperties.get("email.noreply.from");
        String subject = (String) emailProperties.get("email.user.message.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");

        VelocityContext context = new VelocityContext();
        context.put("sender", sender.getFullname());
        context.put("messageId", sobaMessage.getId());
        context.put("messageType", getFriendlyMessageTypeLabel(sobaMessage.getType()));
        context.put("urlPrefix", urlPrefix);
        context.put("message", payload.getString("body"));

        // the payload may have a custom subject line
        if (payload.containsKey("subject")) {
            subject = payload.getString("subject");
        }

        String recipients = payload.getString("recipient");
        for (String recipient : recipients.split(",")) {
            sendEmail(recipient, from, subject, "user_message", context);
        }
    }

    @Override
    public void sendConnectionBrokenEmail(Connection connection) {
        String from = (String) emailProperties.get("email.noreply.from");
        String subject = (String) emailProperties.get("email.connection.error.subject");
        String urlPrefix = (String) emailProperties.get("email.urlprefix");

        User accountAdmin = userService.getAccountAdmin(connection.getAccount());

        VelocityContext context = new VelocityContext();
        context.put("urlPrefix", urlPrefix);
        context.put("connectionName", connection.getAlias());
        context.put("connectionId", connection.getId());

        sendEmail(accountAdmin.getUsername(), from, subject, "connection_error", context);
    }

    /**
     * Sends an email using the default Sender Name.
     *
     * @param recipients  recipient email addresses
     * @param fromAddress the email address we are sending the email from (
     *                    The full "Sender Name &lt;sender@host.com&gt;" defaults to the email.from.name property)
     * @param subject     Subject Line, also passed through Velocity
     * @param templateName  the base Velocity template. The method will look for "/templates/[templateName]_html.vm" and "/templates/[templateName]_text.vm"
     * @param context     Velocity context used to populate the templates
     */
    protected void sendEmail(Collection<String> recipients, String fromAddress, String subject, String templateName, VelocityContext context) {
        String htmlBody = createBodyFromTemplate("/templates/"+templateName+"_html.vm", context);
        String textBody = createBodyFromTemplate("/templates/"+templateName+"_text.vm", context);
        String templateSubject = createStringFromTemplate(subject, context);

        String senderName = (String) emailProperties.get("email.from.name");
        String senderNameWithAddress = senderName == null ? fromAddress :
                senderName + "<" + fromAddress + ">";

        for (String recipient : recipients) {
            sendEmail(recipient, senderNameWithAddress, fromAddress, templateSubject, htmlBody, textBody);
        }
    }

    protected void sendEmail(String recipient, String fromAddress, String subject, String templateName, VelocityContext context) {
        Set<String> recipients = Sets.newHashSet(recipient);
        sendEmail(recipients, fromAddress, subject, templateName, context);
    }


    /**
     * Sends an email.
     *
     * @param to      the address to send the email to.
     * @param from    A String email address or a String in the form of "Name &lt;email_address&gt;"
     * @param replyTo the email address to replyTO
     * @param subject Subject Line
     * @param htmlBody    Message Body
     */
    protected void sendEmail(final String to, final String from, final String replyTo, final String subject, final String htmlBody, final String textBody) {
        if (!enabled) {
            logger.debug("EmailService is currently disabled so email not sent");
            return;
        }

        if (backgroundSend) {
            ListenableFuture<String> future = executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return internalSendEmail(to, from, replyTo, subject, htmlBody, textBody);
                }
            });
            // We don't really need this, but it's stubbed out for now.
            Futures.addCallback(future, new FutureCallback<String>() {
                @Override
                public void onSuccess(String s) { }
                @Override
                public void onFailure(Throwable throwable) { }
            });
        }
        else {
            internalSendEmail(to, from, replyTo, subject, htmlBody, textBody);
        }
    }

    private String internalSendEmail(String to, String from, String replyTo, String subject, String htmlBody, String textBody) {
        SendEmailRequest request = new SendEmailRequest().
                withSource(from).
                withReplyToAddresses(replyTo);

        List<String> toAddresses = new ArrayList<String>();
        toAddresses.add(to);

        Destination dest = new Destination().withToAddresses(toAddresses);
        request.setDestination(dest);

        Content subjContent = new Content().withData(subject);
        Message msg = new Message().withSubject(subjContent);

        // Include a body in both text and HTML formats
        Body theBody = new Body();
        if (textBody != null) {
            theBody.withText(new Content().withData(textBody));
        }
        if (htmlBody != null) {
            theBody.withHtml(new Content().withData(htmlBody));
        }
        msg.setBody(theBody);

        request.setMessage(msg);

        // Call Amazon SES to send the message
        String messageId = null;
        try {
            SendEmailResult result = simpleEmailServiceClient.sendEmail(request);
            messageId = result.getMessageId();
        } catch (AmazonClientException e) {
            logger.debug("[SES EMAIL] AWS AmazonClientException " + e.getMessage());
            logger.error("Caught an AddressException, which means one or more of your "
                    + "addresses are improperly formatted." + e.getMessage());
        } catch (Exception e) {
            logger.debug("[SES EMAIL] AWS General Exception " + e.getMessage());
        }
        return messageId;
    }

    private String createBodyFromTemplate(String templateLocation, VelocityContext context) {
        Template template = Velocity.getTemplate(templateLocation);
        template.setEncoding(DEFAULT_ENCODING);

        StringWriter body = new StringWriter();
        template.merge(context, body);

        return body.toString();
    }

    private String createStringFromTemplate(String stringTemplate, VelocityContext context) {
        StringWriter body = new StringWriter();
        Velocity.evaluate(context, body, "SUBJECT", stringTemplate);
        return body.toString();
    }

    private String getFriendlyMessageTypeLabel(MessageType type) {
        switch (type) {
            case ACTIVITY:
                return "activity";
            case AGENT:
                return "agent";
            case SYSTEM:
                return "system";
            case USER:
                return "user";
            case CONNECTION:
                return "connection";
            case INVENTORY_ITEM:
                return "inventory";
            case GATEWAY:
                return "gateway";
            case NODEBELLY:
                return "insight";
            default: return "";
        }
    }

    private boolean receivesCommentNotifications(User user) {
        return Boolean.valueOf(user.getConfig().get(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS).toString());
    }

    private boolean receivesNewMessageNotifications(User user) {
        return Boolean.valueOf(user.getConfig().get(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS).toString());
    }
}
