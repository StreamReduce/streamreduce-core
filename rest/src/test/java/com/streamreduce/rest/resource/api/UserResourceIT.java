package com.streamreduce.rest.resource.api;


import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserResourceIT extends AbstractServiceTestCase {

    @Autowired
    UserResource userResource;

    @Test
    public void testChangeUserProfileDoesNotAllowDuplicateAlias() {
        User existingAliasUser = new User.Builder().username("maynard@toolband.com").fullname("MJK").alias("maynard")
                .password("vocals").account(testAccount).build();
        userService.createUser(existingAliasUser);

        User anotherUser = new User.Builder().username("justinc@toolband.com").fullname("Justin Chancellor").password("bass")
                .alias("justinc").account(testAccount).build();
        userService.createUser(anotherUser);

        SecurityService mockSecurityService = mock(SecurityService.class);
        when(mockSecurityService.getCurrentUser()).thenReturn(anotherUser);

        userResource.securityService = mockSecurityService;

        JSONObject updateUserJson = new JSONObject();
        updateUserJson.put("fullname", "justinc");
        updateUserJson.put("alias", "maynard"); //Justin is tired of playing bass and wants to do vocals

        Response actualResponse = userResource.changeUserProfile(updateUserJson);
        Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), actualResponse.getStatus());
        ConstraintViolationExceptionResponseDTO resp = (ConstraintViolationExceptionResponseDTO) actualResponse.getEntity();
        Assert.assertEquals("maynard is already in use", resp.getViolations().get("alias"));
    }

    @Test
    public void testChangeUserProfileDoesNotAllowAtSymbolInFront() {
        //Tests another condition of SOBA-1875 specifically - @ isn't allowed as first character in a user alias.
        SecurityService mockSecurityService = mock(SecurityService.class);
        when(mockSecurityService.getCurrentUser()).thenReturn(testUser);

        userResource.securityService = mockSecurityService;

        JSONObject updateUserJson = new JSONObject();
        updateUserJson.put("fullname", "testUser");
        updateUserJson.put("alias", "@maynard");

        Response actualResponse = userResource.changeUserProfile(updateUserJson);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), actualResponse.getStatus());
        ConstraintViolationExceptionResponseDTO resp = (ConstraintViolationExceptionResponseDTO) actualResponse.getEntity();
        Assert.assertEquals("User alias contains characters that aren't alphanumeric, dashes, or underscores",
                resp.getViolations().get("alias"));
    }




}
