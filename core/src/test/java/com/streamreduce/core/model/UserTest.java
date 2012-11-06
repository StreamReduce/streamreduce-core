package com.streamreduce.core.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserTest {
    
    @Test
    public void testCreateUserFromBuilder() {
        //Happy path test that builds a user
        new User.Builder()
            .username("maynard_keenan")
            .account(new Account.Builder().name("ABC").build()).build();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testCreateUserWithoutUsername() {
        new User.Builder().account(new Account.Builder().name("ABC").build()).build();
    }

    @Test
    public void testCreateUserWithoutAccount() {
        //Exercises that a user can be built without being associated with an account
        new User.Builder().username("adam_jones").build();
    }

    @Test
    public void testCreateUserFromBuilderBasedOnExistingUser() {
        //Exercises the User.Builder constructor that accepts a User instance and populates the object being built with
        //copies of that User's properties.
        User original = new User.Builder()
                .username("maynard_keenan")
                .alias("maynard")
                .fullname("Maynard James Keenan")
                .roles(Sets.newHashSet(new Role("badass")))
                .account(new Account.Builder().name("ABC").build()).build();

        User copy = new User.Builder(original).build();
        Assert.assertEquals(original,copy);
    }

    @Test
    public void testCreateUserFromBuilderCreatesValidAliasIfNotSpecified() {
        //When alias isn't specified on a user builder, make sure we make the alias a
        User original = new User.Builder()
                .username("maynard@toolband.com")
                .fullname("Maynard James Keenan")
                .roles(Sets.newHashSet(new Role("badass")))
                .account(new Account.Builder().name("ABC").build()).build();

        Assert.assertEquals("maynard_at_toolband_com",original.alias);
    }

    @Test
    public void testCreateUserFromBuilderCreatesValidAliasIfNotSpecifiedRetainsDashes() {
        //When alias isn't specified on a user builder, make sure we make the alias a
        User original = new User.Builder()
                .username("maynard-keenan@toolband.com")
                .fullname("Maynard James Keenan")
                .roles(Sets.newHashSet(new Role("badass")))
                .account(new Account.Builder().name("ABC").build()).build();

        Assert.assertEquals("maynard-keenan_at_toolband_com",original.alias);
    }

    @Test
    public void testUserNotificationsInConfigByDefault() {
        //Makes sure that new user has notifications in config by default
        User user = createValidUser();
        Map<String,Object> config = user.getConfig();
        assertTrue(Boolean.valueOf(config.get(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS).toString()));
        assertTrue(Boolean.valueOf(config.get(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS).toString()));
    }


    @Test
    @SuppressWarnings("unused")
    public void testSetConfigValue() {
        User user = createValidUser();

        //String
        user.setConfigValue("foo","bar");
        assertEquals("bar", user.getConfig().get("foo"));

        //Boolean
        user.setConfigValue("foo",true);
        assertEquals(true, user.getConfig().get("foo"));

        //Number
        user.setConfigValue("foo",5.5);
        assertEquals(5.5, user.getConfig().get("foo"));


        List list = Lists.newArrayList(1,2,3,4);
        user.setConfigValue("foo",list );
        assertEquals(list , user.getConfig().get("foo"));

        //Map (based on JSONObject)
        JSONObject jsonObject = new JSONObjectBuilder().add("a","bar").add("b",5).build();
        user.setConfigValue("foo",jsonObject );
        assertEquals(new HashMap(jsonObject), user.getConfig().get("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveConfigValue_gravatarHash() {
        User user = createValidUser();
        user.removeConfigValue(User.ConfigKeys.GRAVATAR_HASH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveConfigValue_receivesCommentNotifications() {
        User user = createValidUser();
        user.removeConfigValue(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveConfigValue_receivesNewMessageNotifications() {
        User user = createValidUser();
        user.removeConfigValue(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS);
    }

    @Test
    public void testRemoveConfigValue() {
        User user = createValidUser();
        user.setConfigValue("foo","bar");
        assertEquals("bar",user.getConfig().get("foo"));
        user.removeConfigValue("foo");
        assertEquals(null,user.getConfig().get("foo"));
    }


    private User createValidUser() {
        User user = new User.Builder()
                .username("maynard-keenan@toolband.com")
                .fullname("Maynard James Keenan")
                .roles(Sets.newHashSet(new Role("badass")))
                .account(new Account.Builder().name("ABC").build()).build();
        return user;
    }
}
