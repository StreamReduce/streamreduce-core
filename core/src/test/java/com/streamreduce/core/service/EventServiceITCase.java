package com.streamreduce.core.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.Constants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.ObjectWithId;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.InventoryItemNotFoundException;
import com.streamreduce.core.service.exception.MessageNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import org.apache.shiro.authc.AuthenticationException;
import org.bson.types.ObjectId;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test to ensure {@link EventService} works as expected.
 */
public class EventServiceITCase extends AbstractServiceTestCase {

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    /**
     * Tests that the events created to this point for the test account and test user
     * are as expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExpectedEventsForTestAccountAndTestUser() throws Exception {
        EventService eventService = applicationManager.getEventService();
        List<Event> events = eventService.getEventsForAccount(getTestAccount());

        // At this point, there should be exactly 3 CREATE events
        List<Event> createEvents = Lists.newArrayList(Iterables.filter(events, new Predicate<Event>() {
            @Override
            public boolean apply(@Nullable Event input) {
                return input != null && input.getEventId() == EventId.CREATE;
            }
        }));

        Assert.assertEquals(3,createEvents.size());
    }


        /**
         * Tests that {@link EventService#createEvent(com.streamreduce.core.event.EventId, com.streamreduce.core.model.ObjectWithId,
         *                                            java.util.Map)}
         * works as expected when a user is not logged in and we are creating an event.
         */
        @Test
        public void testCreateEvent_ReadEventWithNoLoggedInUser ()throws Exception {
            // Test creating an event with no logged in user
            EventService eventService = applicationManager.getEventService();
            SecurityService mockSecurityService = mock(SecurityService.class);
            when(mockSecurityService.getCurrentUser()).thenThrow(new AuthenticationException());
            ReflectionTestUtils.setField(eventService, "securityService", mockSecurityService);


            Event event = eventService.createEvent(EventId.READ, testUser, null);
            Assert.assertNull(event);
        }

        /**
         * Tests that {@link EventService#createEvent(com.streamreduce.core.event.EventId, com.streamreduce.core.model.ObjectWithId,
         *                                            java.util.Map)}
         * works as expected when a user is not logged in and we are creating a SobaMessage
         */
        @Test
        public void testCreateEvent_CreateSobaMessageWithNoLoggedInUser ()throws Exception {
            // Test creating an event with no logged in user
            EventService eventService = applicationManager.getEventService();
            SecurityService mockSecurityService = mock(SecurityService.class);
            when(mockSecurityService.getCurrentUser()).thenThrow(new AuthenticationException());
            ReflectionTestUtils.setField(eventService, "securityService", mockSecurityService);


            // Test creating a SobaObject event with no logged in user
            Connection connection = new Connection.Builder()
                    .provider(connectionProviderFactory.connectionProviderFromId(ProviderIdConstants.GITHUB_PROVIDER_ID))
                    .account(testAccount)
                    .alias("Test GitHub Connection")
                    .description("This is a test GitHub connection.")
                    .user(testUser)
                    .authType(AuthType.USERNAME_PASSWORD)
                    .credentials(new ConnectionCredentials("somegithubusername", "somegithubpassword"))
                    .build();

            Event event = eventService.createEvent(EventId.CREATE, connection, null);

            Assert.assertNotNull(event);
        }


        /**
         * Tests that {@link EventService#createEvent(com.streamreduce.core.event.EventId, com.streamreduce.core.model.ObjectWithId,
         *                                            java.util.Map)}
         */
        @Test
        public void testCreateEvent_CreateEventWithLoggedInUser ()throws Exception {
            // Test creating an event as a logged in user
            // (We have to mock a few things since logging a user in programmatically isn't an option)
            EventService esMock = getEventServiceMock();

            Event event = esMock.createEvent(EventId.CREATE_GLOBAL_MESSAGE, null, null);

            verifyEvent(event, EventId.CREATE_GLOBAL_MESSAGE, testUser, testAccount, null, null);
        }

        /**
         * Test that will attempt to retrieve all {@link Event} objects and verify them.
         *
         * @throws Exception if anything goes wrong
         */
        @Test
        public void testAllGeneratedEvents ()throws Exception {
            EventService eventService = applicationManager.getEventService();
            List<Event> allEvents = eventService.getAllEvents();

            // TODO: It would be nice to test every event scenario when time permits

            for (Event event : allEvents) {
                verifyEvent(event);
            }
        }

        /**
         * Creates a mock {@link EventService} that is mocked so a user is logged in.
         *
         * @return the mock ApplicationManager
         */

    private EventService getEventServiceMock() {
        SecurityService ssMock = Mockito.mock(SecurityService.class);
        UserService usMock = Mockito.spy(applicationManager.getUserService());
        EventService esMock = Mockito.spy(applicationManager.getEventService());

        ReflectionTestUtils.setField(esMock, "userService", usMock);
        ReflectionTestUtils.setField(esMock, "securityService", ssMock);
        ReflectionTestUtils.setField(usMock, "eventService", esMock);
        Mockito.when(ssMock.getCurrentUser()).thenReturn(testUser);

        return esMock;
    }

    /**
     * Validates historical events, where possible, by looking up the event's user, account and target
     * and then using that information in calling {@link #verifyEvent(com.streamreduce.core.model.Event,
     * com.streamreduce.core.event.EventId, com.streamreduce.core.model.User, com.streamreduce.core.model.Account,
     * com.streamreduce.core.model.ObjectWithId, java.util.Map)}.
     *
     * @param event the event to verify
     */
    private void verifyEvent(Event event) {
        Map<String, Object> eventMetadata = event.getMetadata();
        String eventType = (String) eventMetadata.get("targetType");
        User eventUser = null;
        Account eventAccount = null;
        ObjectWithId eventTarget = null;

        // TODO: Bubble the event participants lookup to event service methods

        if (event.getUserId() != null) {
            try {
                eventUser = applicationManager.getUserService().getUserById(event.getUserId());
            } catch (UserNotFoundException e) {
                // Do nothing
            }
        }

        if (event.getAccountId() != null) {
            try {
                eventAccount = applicationManager.getUserService().getAccount(event.getAccountId());
            } catch (AccountNotFoundException e) {
                // Do nothing
            }
        }

        if (event.getTargetId() != null) {
            ObjectId eventTargetId = event.getTargetId();

            if (eventType.equals(Account.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getUserService().getAccount(eventTargetId);
                } catch (AccountNotFoundException e) {
                    // Do nothing
                }
            } else if (eventType.equals(InventoryItem.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getInventoryService().getInventoryItem(eventTargetId);
                } catch (InventoryItemNotFoundException e) {
                    // Do nothing
                }
            } else if (eventType.equals(Connection.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getConnectionService().getConnection(eventTargetId);
                } catch (ConnectionNotFoundException e) {
                    // Do nothing
                }
            } else if (eventType.equals(SobaMessage.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getMessageService().getMessage(eventAccount, eventTargetId);
                } catch (MessageNotFoundException e) {
                    // Do nothing
                }
            } else if (eventType.equals(User.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getUserService().getUserById(eventTargetId);
                } catch (UserNotFoundException e) {
                    // Do nothing
                }
            } else if (eventType.equals(SobaMessage.class.getSimpleName())) {
                try {
                    eventTarget = applicationManager.getMessageService().getMessage(eventAccount, eventTargetId);
                } catch (MessageNotFoundException e) {
                    // Do nothing
                }
            } else {
                Assert.fail("Unable to handle target class of type: " + eventType + ".");
            }
        }

        // We cannot actually validate events where we cannot lookup the object by id for historical events
        if (event.getUserId() != null && eventUser != null &&
                event.getAccountId() != null && eventAccount != null &&
                event.getTargetId() != null && eventTarget != null) {
            verifyEvent(event, event.getEventId(), eventUser, eventAccount, eventTarget, eventMetadata);
        } else {
            System.out.println("[TEST WARN] Historical Event (" + event.getId() + ") could not be validated for " +
                    "content due to the user, account and/or target no longer being in the " +
                    "database.");
        }
    }

    /**
     * Validates the event against expected event pieces.
     *
     * @param event                 the event to validate
     * @param expectedEventId       the expected event id
     * @param expectedUser          the expected user
     * @param expectedAccount       the expected account
     * @param expectedTarget        the expected target
     * @param expectedExtraMetadata the expected extra metadata
     */
    private <T extends ObjectWithId> void verifyEvent(Event event, EventId expectedEventId, User expectedUser,
                                                      Account expectedAccount, T expectedTarget,
                                                      Map<String, Object> expectedExtraMetadata) {
        Map<String, Object> eventMetadata = event.getMetadata();

        // Validate the event id
        Assert.assertEquals(expectedEventId, event.getEventId());

        // Validate the user
        Assert.assertEquals(expectedUser != null ? expectedUser.getId() : null, event.getUserId());

        // Validate the account
        Assert.assertEquals(expectedAccount != null ? expectedAccount.getId() : null, event.getAccountId());

        // Validate the target
        Assert.assertEquals(expectedTarget != null ? expectedTarget.getId() : null, event.getTargetId());

        // Validate the timestamp
        Assert.assertNotNull(event.getTimestamp());

        // Validate the extra metadata
        if (expectedExtraMetadata != null) {
            for (String key : expectedExtraMetadata.keySet()) {
                Assert.assertEquals(expectedExtraMetadata.get(key), eventMetadata.get(key));
            }
        }

        // Validate metadata automatically generated at event creation

        // Validate user generated event metadata
        Map<String, Object> expectedUserMetadata = new HashMap<String, Object>();

        expectedUserMetadata.put("sourceAlias", expectedUser != null ? expectedUser.getAlias() : null);
        expectedUserMetadata.put("sourceFuid", expectedUser != null ? expectedUser.getFuid() : null);
        expectedUserMetadata.put("sourceFullname", expectedUser != null ? expectedUser.getFullname() : null);
        expectedUserMetadata.put("sourceUsername", expectedUser != null ? expectedUser.getUsername() : null);
        expectedUserMetadata.put("sourceVersion", expectedUser != null ? expectedUser.getVersion() : null);

        validateEventMetadata(eventMetadata, expectedUserMetadata, expectedUser != null);

        // Validate account generated event metadata
        Map<String, Object> expectedAccountMetadata = new HashMap<String, Object>();

        expectedAccountMetadata.put("accountFuid", expectedAccount != null ? expectedAccount.getFuid() : null);
        expectedAccountMetadata.put("accountName", expectedAccount != null ? expectedAccount.getName() : null);
        expectedAccountMetadata.put("accountVersion", expectedAccount != null ? expectedAccount.getVersion() : null);

        validateEventMetadata(eventMetadata, expectedAccountMetadata, expectedAccount != null);

        // Validate target generated metadata
        if (expectedTarget != null) {
            // Validate object version
            Assert.assertEquals(eventMetadata.get("targetVersion"), expectedTarget.getVersion());

            // Validate object type (Class's simple name)
            Assert.assertEquals(eventMetadata.get("targetType"), expectedTarget.getClass().getSimpleName());

            // Validate SobaObject generated metadata
            if (expectedTarget instanceof SobaObject) {
                SobaObject tSobaObject = (SobaObject) expectedTarget;
                Map<String, Object> sExpectedMetadata = new HashMap<String, Object>();

                sExpectedMetadata.put("targetAlias", tSobaObject.getAlias());
                sExpectedMetadata.put("targetHashtags", tSobaObject.getHashtags());

                validateEventMetadata(eventMetadata, sExpectedMetadata, true);
            }

            // Validate the generated metadata based on object type

            Map<String, Object> expectedObjectMetadata = new HashMap<String, Object>();

            if (expectedTarget instanceof Account) {
                Account tAccount = (Account) expectedTarget;

                expectedObjectMetadata.put("targetFuid", tAccount.getFuid());
                expectedObjectMetadata.put("targetName", tAccount.getName());
            } else if (expectedTarget instanceof InventoryItem) {
                InventoryItem tInventoryItem = (InventoryItem) expectedTarget;
                Connection tConnection = tInventoryItem.getConnection();
                ConnectionProvider tConnectionProvider =
                        applicationManager.getConnectionProviderFactory()
                                .connectionProviderFromId(tConnection.getProviderId());
                Map<String, Object> expectedInventoryItemMetadata = new HashMap<String, Object>();
                InventoryService inventoryService = applicationManager.getInventoryService();

                expectedObjectMetadata.put("targetExternalId", tInventoryItem.getExternalId());

                expectedInventoryItemMetadata.put("targetConnectionId", tConnection.getId());
                expectedInventoryItemMetadata.put("targetConnectionAlias", tConnection.getAlias());
                expectedInventoryItemMetadata.put("targetConnectionHashtags", tConnection.getHashtags());
                expectedInventoryItemMetadata.put("targetConnectionVersion", tConnection.getVersion());
                expectedInventoryItemMetadata.put("targetProviderId", tConnectionProvider.getId());
                expectedInventoryItemMetadata.put("targetProviderDisplayName", tConnectionProvider.getDisplayName());
                expectedInventoryItemMetadata.put("targetProviderType", tConnectionProvider.getType());

                if (tInventoryItem.getConnection().getProviderId().equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                    Location zone = inventoryService.getLocationByScope(tInventoryItem, LocationScope.ZONE);
                    Location region = inventoryService.getLocationByScope(tInventoryItem, LocationScope.REGION);
                    Set<String> iso3166Codes = zone != null ? zone.getIso3166Codes() : null;
                    String iso3166Code = iso3166Codes != null && iso3166Codes.size() >= 1 ?
                            iso3166Codes.iterator().next() :
                            null;

                    if (tInventoryItem.getType().equals(Constants.COMPUTE_INSTANCE_TYPE)) {
                        expectedObjectMetadata.put("targetIP",
                                inventoryService.getComputeInstanceIPAddress(tInventoryItem));
                        expectedObjectMetadata.put("targetOS",
                                inventoryService.getComputeInstanceOSName(tInventoryItem));
                    }

                    if (iso3166Code != null) {
                        expectedObjectMetadata.put("targetISO3166Code", iso3166Code);
                    }
                    if (zone != null) {
                        expectedObjectMetadata.put("targetZone", zone.getId());
                    }
                    if (region != null) {
                        expectedObjectMetadata.put("targetRegion", region.getId());
                    }
                }

                validateEventMetadata(eventMetadata, expectedInventoryItemMetadata, true);
            } else if (expectedTarget instanceof Connection) {
                Connection tConnection = (Connection) expectedTarget;
                ConnectionProvider tConnectionProvider = applicationManager.getConnectionProviderFactory()
                        .connectionProviderFromId(
                                tConnection.getProviderId());

                expectedObjectMetadata.put("targetProviderId", tConnectionProvider.getId());
                expectedObjectMetadata.put("targetProviderDisplayName", tConnectionProvider.getDisplayName());
                expectedObjectMetadata.put("targetProviderType", tConnectionProvider.getType());
            } else if (expectedTarget instanceof User) {
                User tUser = (User) expectedTarget;

                expectedObjectMetadata.put("targetFuid", tUser.getFuid());
                expectedObjectMetadata.put("targetFullname", tUser.getFullname());
                expectedObjectMetadata.put("targetUsername", tUser.getUsername());
            }

            if (expectedObjectMetadata.size() > 0) {
                validateEventMetadata(eventMetadata, expectedObjectMetadata, true);
            }
        }
    }

    /**
     * Helper method to check the event metadata for values or make sure the values aren't there.
     *
     * @param eventMetadata         the event's actual metadata
     * @param expectedEventMetadata the expected event metadata
     * @param keysExpected          whether the expected event metadata keys should or should not exist
     */
    private void validateEventMetadata(Map<String, Object> eventMetadata, Map<String, Object> expectedEventMetadata,
                                       boolean keysExpected) {
        if (keysExpected) {
            for (Map.Entry<String, Object> entry : expectedEventMetadata.entrySet()) {
                String key = entry.getKey();
                Object actualValue = eventMetadata.get(entry.getKey());
                Object expectedValue = expectedEventMetadata.get(key);

                try {
                    if (actualValue instanceof Collection || expectedValue instanceof Collection) {
                        Assert.assertEquals(actualValue != null ? ((Collection) actualValue).size() : 0,
                                expectedValue != null ? ((Collection) expectedValue).size() : 0);
                    } else {
                        Assert.assertEquals(actualValue, expectedValue);
                    }
                } catch (AssertionError ae) {
                    System.out.println("Expected event metadata key (" + key + ") did not match expected " +
                            "value: " + expectedEventMetadata.get(key));
                    throw ae;
                }
            }
        } else {
            for (String key : expectedEventMetadata.keySet()) {
                try {
                    Assert.assertFalse(eventMetadata.containsKey(key));
                } catch (AssertionError ae) {
                    System.out.println("Unexpected event metadata key was present: " + key);
                }
            }
        }
    }

}
