package com.streamreduce.rest.resource.api;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.EmailService;
import com.streamreduce.core.service.exception.MessageNotFoundException;
import com.streamreduce.core.service.exception.TargetNotFoundException;
import com.streamreduce.rest.dto.response.MessageCommentResponseDTO;
import com.streamreduce.rest.dto.response.MessageCommentsResponseDTO;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.util.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("api/messages")
@Produces(MediaType.APPLICATION_JSON)
public class MessageResource extends AbstractTagableSobaResource {

    @Autowired
    private EmailService emailService;

    /**
     * Creates a new USER generated message.  Use created messages will show up as "from" the user who created it, either as a DM or an
     * Account level message.
     *
     * @param json - The message to create
     * @return - Response.Status.CREATED and a DTO of SobaMessage that was created.
     * @response.representation.201.doc Returned when the message was successfully created and sent
     * @response.representation.400.doc Returned when the client attempts to create a user message that is empty or sends to an invalid target.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMessage(JSONObject json) {

        String message = getJSON(json, "message");

        if (isEmpty(message)) {
            return error("Message payload can not be empty", Response.status(Response.Status.BAD_REQUEST));
        }

        SobaMessage sobaMessage;
        User sender = applicationManager.getSecurityService().getCurrentUser();
        Map<String, Object> eventContext = new HashMap<String, Object>();

        eventContext.put("message", message);
        eventContext.put("payload", json);

        Event event = applicationManager.getEventService().createEvent(EventId.USER_MESSAGE, null, eventContext);

        try {
            sobaMessage = applicationManager.getMessageService().sendUserMessage(event, sender, message);
        } catch (TargetNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }

        return Response
                .status(Response.Status.CREATED)
                .entity(SobaMessageResponseDTO.fromSobaMessage(sobaMessage))
                .build();
    }

    /**
     * Retrieves all messages for the client.
     *
     * @param after     - time in ms, after this date
     * @param before    - time in ms, before this date
     * @param limit     - only return this value, optional, but capped at something like 100 if set to 0
     * @param ascending - default to true
     * @param search    - string to search on
     * @param fullText  - get the full message payload, or else truncate it
     * @param hashtags  - limits results to those that only contain all of the specified hashtags
     * @param sender    - limits results to only those messages whose sender is specified.  sender can represent the id or alias of the sender
     * @return DTO of messages
     * @response.representation.200.doc Returned when all messages were successfully rendered
     */
    @GET
    public Response getAllMessages(@QueryParam("after") Long after, @QueryParam("before") Long before,
                                   @QueryParam("limit") int limit, @DefaultValue("true") @QueryParam("ascending") boolean ascending,
                                   @QueryParam("search") String search, @QueryParam("fullText") boolean fullText,
                                   @QueryParam("hashtag") List<String> hashtags, @QueryParam("sender") String sender,
                                   @QueryParam("excludeInsights") boolean excludeInsights) {

        User user = applicationManager.getSecurityService().getCurrentUser();
        List<SobaMessage> allMessages = applicationManager.getMessageService().getAllMessages(user, after, before, limit, ascending, search, hashtags, sender, excludeInsights);

        return Response
                .ok(SobaMessageResponseDTO.fromSobaMessages(allMessages, fullText))
                .build();

    }

    /**
     * Retreives the full contents of a message given the message id.
     *
     * @param messageId -- messageId
     * @return -  will only return the message if you can access it (ie, if it's a DIRECT message to you
     *         or a subscriber message, etc.
     * @response.representation.200.doc Returned when all direct messages were successfully rendered
     * @response.representation.404.doc Returned when a message could not be found given the ID for the client
     */
    @GET
    @Path("{messageId}")
    public Response getMessageById(@PathParam("messageId") ObjectId messageId) {

        User user = applicationManager.getSecurityService().getCurrentUser();
        SobaMessage sobaMessage;

        try {
            sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok(SobaMessageResponseDTO.fromSobaMessage(sobaMessage))
                .build();

    }

    @DELETE
    @Path("{messageId}")
    public Response deleteMessage(@PathParam("messageId") ObjectId messageId) {
        User user = applicationManager.getSecurityService().getCurrentUser();

        try {
            SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);
            // users can only nullify a message from a resource they own
            if (sobaMessage.getOwnerId().equals(user.getId())) {
                applicationManager.getMessageService().nullifyMessage(user, sobaMessage);
            } else {
                return error(ErrorMessages.APPLICATION_ACCESS_DENIED, Response.status(Response.Status.BAD_REQUEST));
            }

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response.ok().build();
    }


    /**
     * Add a comment or a tag (via the comment) to a given message.
     *
     * @param messageId - messageId
     * @param json      - the comment payload
     * @return no content
     * @response.representation.204.doc Returned when a comment or tag was added to a message.
     * @response.representation.400.doc Returned when the client attempts to add an empty comment
     * @response.representation.404.doc Returned when a message being added could not be found given the ID for the client
     */
    @PUT
    @Path("{messageId}/comment")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCommentToMessage(@PathParam("messageId") ObjectId messageId, JSONObject json) {

        String comment = getJSON(json, "comment");

        if (isEmpty(comment)) {
            return error(ErrorMessages.MISSING_REQUIRED_FIELD + "comment", Response.status(Response.Status.BAD_REQUEST));
        }

        User user = applicationManager.getSecurityService().getCurrentUser();

        // check if the user added any tags
        MessageUtils.ParsedMessage parsedMessage = MessageUtils.parseMessage(comment);
        Set<String> hashtags = parsedMessage.getTags();

        MessageComment messageComment = new MessageComment(user, parsedMessage.getMessage());

        try {
            SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);
            // this is now a conversation, so add that tag
            hashtags.add("#conversation");
            applicationManager.getMessageService().addCommentToMessage(user.getAccount(), sobaMessage.getId(), messageComment, hashtags);

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    /**
     * Retrieves all comments that have been added to a given message
     *
     * @param messageId the id of the messsage that comments will be returned for
     * @return message comment DTO
     * @response.representation.200.doc Returned when all comments to a message were succesfully rendered
     * @response.representation.404.doc Returned when the message does not exist
     */
    @GET
    @Path("{messageId}/comment")
    public Response getCommentsForMessage(@PathParam("messageId") ObjectId messageId) {

        User user = applicationManager.getSecurityService().getCurrentUser();
        SobaMessage sobaMessage;

        try {
            sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        MessageCommentsResponseDTO dto = new MessageCommentsResponseDTO();
        List<MessageCommentResponseDTO> allComments = new ArrayList<MessageCommentResponseDTO>();
        for (MessageComment comment : sobaMessage.getComments()) {
            allComments.add(comment.toDTO());
        }

        dto.setComments(allComments);

        return Response
                .ok(dto)
                .build();
    }


    /**
     * Remove the comment from the User on the timestamp indicated, since we don't have a messageId
     *
     * @return
     */
    @DELETE
    @Path("{messageId}/comment/{created}")
    public Response deleteCommentForMessage(@PathParam("messageId") ObjectId messageId, @PathParam("created") Long created) {

        User user = applicationManager.getSecurityService().getCurrentUser();
        try {
            SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);
            for (MessageComment messageComment : sobaMessage.getComments()) {
                // find the message comment to update
                if (messageComment.getSenderId().equals(user.getId()) && messageComment.getCreated().equals(created)) {
                    applicationManager.getMessageService().nullifyMessageComment(user, sobaMessage, messageComment);
                    break;
                }
            }

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok()
                .build();

    }

    /**
     * Applies a hashtag directly to a message
     *
     * @param messageId - internal Id of message
     * @param json      - hasstag dto
     * @return - 200 OK
     * @response.representation.200.doc Returned when a hashtag was successfully added to a message
     * @response.representation.400.doc Returned when a client attempts to add an empty hashtag or the message can not be found
     */
    @POST
    @Path("{messageId}/hashtag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Response addTag(@PathParam("messageId") ObjectId messageId, JSONObject json) {

        String hashtag = getJSON(json, HASHTAG);

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        User user = applicationManager.getSecurityService().getCurrentUser();

        try {
            applicationManager.getMessageService().addHashtagToMessage(user.getAccount(), messageId, hashtag);

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }

        return Response
                .ok()
                .build();
    }


    /**
     * Retrieves a list of all hashtags currently applied to a given message.
     *
     * @param messageId
     * @response.representation.200.doc Returned when a hashtags for the given message were successfully rendered
     * @response.representation.400.doc Returned the client does not have access to the message
     * @response.representation.404.doc Returned when the message that hashtags are requested for can't be found
     */
    @GET
    @Path("{messageId}/hashtag")
    @Override
    public Response getTags(@PathParam("messageId") ObjectId messageId) {

        User user = applicationManager.getSecurityService().getCurrentUser();
        Set<String> tags;
        try {
            SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);
            tags = sobaMessage.getHashtags();

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        }

        return Response
                .ok(tags)
                .build();

    }


    /**
     * Remove a hashtag from a given message.
     *
     * @param messageId
     * @param hashtag
     * @return
     * @response.representation.200.doc Returned when the hashtag for the message was removed successfully.
     * @response.representation.400.doc Returned the client does not have access to the message
     * @response.representation.404.doc Returned when the message that hashtags are requested for can't be found
     */
    @DELETE
    @Path("{messageId}/hashtag/{tagname}")
    @Override
    public Response removeTag(@PathParam("messageId") ObjectId messageId, @PathParam("tagname") String hashtag) {

        if (isEmpty(hashtag)) {
            return error("Hashtag payload is empty", Response.status(Response.Status.BAD_REQUEST));
        }

        User user = applicationManager.getSecurityService().getCurrentUser();

        try {
            applicationManager.getMessageService().removeHashtagFromMessage(user.getAccount(), messageId, hashtag);

        } catch (MessageNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
        return Response.ok().build();
    }

    @POST
    @Path("{messageId}/email")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response email(@PathParam("messageId") ObjectId messageId, JSONObject json) {

        User user = applicationManager.getSecurityService().getCurrentUser();

        SobaMessage message;
        try {
            message = applicationManager.getMessageService().getMessage(user.getAccount(), messageId);
        }
        catch (MessageNotFoundException mnfe) {
            logger.error("Unable to find message with ID {}", messageId);
            return Response.status(404).build();
        }

        emailService.sendUserMessageEmail(user, message, json);

        return Response
                .ok()
                .build();
    }
}
