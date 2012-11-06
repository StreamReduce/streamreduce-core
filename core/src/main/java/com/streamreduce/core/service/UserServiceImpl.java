package com.streamreduce.core.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.streamreduce.Constants;
import com.streamreduce.InvalidUserAliasException;
import com.streamreduce.core.dao.AccountDAO;
import com.streamreduce.core.dao.DAODatasourceType;
import com.streamreduce.core.dao.EventLogDAO;
import com.streamreduce.core.dao.GenericCollectionDAO;
import com.streamreduce.core.dao.RoleDAO;
import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.EventLog;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.core.service.exception.UsernameUnavailableException;
import com.streamreduce.security.Roles;
import com.streamreduce.util.MessageUtils;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service("userService")
public class UserServiceImpl extends AbstractService implements UserService {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private RoleDAO roleDAO;
    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private EventLogDAO eventLogDAO;
    @Autowired
    private GenericCollectionDAO genericCollectionDAO;
    @Autowired
    private SearchService searchService;
    @Autowired
    private EventService eventService;
    @Autowired
    private MessageService messageService;

    private Cache<String, User> superUserCache =
            CacheBuilder.newBuilder().maximumSize(1).expireAfterAccess(5, TimeUnit.MINUTES).build();

    @Override
    public boolean isUsernameAvailable(String name) {
        User u = userDAO.findUser(name);
        return u == null;
    }

    @Override
    public boolean isAliasAvailable(Account account, String alias) {
        return userDAO.findUserForAlias(account, alias) == null;
    }

    @Override
    public Account getAccount(ObjectId accountId) throws AccountNotFoundException {
        if (accountId == null) {
            throw new AccountNotFoundException("Passed account ID was null.");
        }
        Account a = accountDAO.get(accountId);
        if (a == null) {
            throw new AccountNotFoundException(accountId.toString());
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, a, null);
        return a;
    }

    @Override
    public Set<Role> getAccountRoles(ObjectId accountId) throws AccountNotFoundException {
        return roleDAO.findAccountRoles(accountId);
    }

    @Override
    public User getUserById(ObjectId userId) throws UserNotFoundException {
        User u = userDAO.get(userId);
        if (u == null) {
            throw new UserNotFoundException(userId.toString());
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }

    public List<User> getUsersById(Set<ObjectId> userIds) {
        List<User> userList = new ArrayList<User>();
        for (ObjectId id : userIds) {
            try {
                userList.add(getUserById(id));
            } catch (UserNotFoundException e) {
                // whatever...
            }
        }
        return userList;
    }

    @Override
    public User getUserById(ObjectId userId, Account account) throws UserNotFoundException {
        User u = userDAO.get(userId);
        User user = userDAO.findUserForUsername(account, u.getUsername());
        if (user == null) {
            throw new UserNotFoundException(u.getUsername());
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return user;
    }

    @Override
    public User getUserByAuthenticationToken(String authToken) throws UserNotFoundException {
        User user = userDAO.findByAuthToken(authToken);
        if (user == null) {
            throw new UserNotFoundException(authToken);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, user, null);
        return user;
    }

    @Override
    public User getUser(String username, Account account) throws UserNotFoundException {
        User u = userDAO.findUserForUsername(account, username);
        if (u == null) {
            throw new UserNotFoundException(username);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }

    @Override
    public User getUser(String username) throws UserNotFoundException {
        User u = userDAO.findUser(username);
        if (u == null) {
            throw new UserNotFoundException(username);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }

    @Override
    public User getTargetUser(Account account, String name) throws UserNotFoundException {
        User u = userDAO.findUserInAccount(account, name);
        if (u == null) {
            throw new UserNotFoundException(name);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }

    /**
     * Create an account. Also assign the default product
     *
     * @param account - properly formed account to create
     * @return - the account with the accountId populated
     */
    @Override
    public Account createAccount(Account account) {
        accountDAO.save(account);
        // bootstrap the metric inbox
        createMetricInbox(account);
        // once the inbox has been created, start a river so that we can search on it
        searchService.createRiverForAccount(account);
        // Bootstrap sample messages
        messageService.copyArchivedMessagesToInbox(account);
        // Create the event stream entry
        eventService.createEvent(EventId.CREATE, account, null);
        return account;
    }

    @Override
    public void updateAccount(Account account) {
        accountDAO.save(account);
        // Create the event stream entry
        eventService.createEvent(EventId.UPDATE, account, null);
    }

    @Override
    public void updateUser(User user) {
        validateUserAlias(user);
        userDAO.save(user);
        // Create the event stream entry
        eventService.createEvent(EventId.UPDATE, user, null);
    }

    @Override
    public void resetUserPassword(User user, boolean mobile) {
        userDAO.save(user);
        // Create the event stream entry
        eventService.createEvent(EventId.USER_PASSWORD_RESET_REQUEST, user, null);
    }

    @Override
    public void deleteAccount(ObjectId accountId) {
        try {
            // you can't delete the super user account
            User superUser = getSuperUser();
            if (superUser.getAccount().getId().equals(accountId)) {
                logger.error("You can not delete the Nodeable account");
                return;
            }
            Account toBeDeleted = getAccount(accountId);
            // Create the event stream entry
            eventService.createEvent(EventId.DELETE, toBeDeleted, null);
            accountDAO.deleteById(accountId);
        } catch (AccountNotFoundException anf) {
            logger.error("AccountNotFoundException", anf);
        }
    }


    @Override
    public void deleteUser(User user) {

        // you can't delete the super user
        if (user.getUsername().equals(Constants.NODEABLE_SUPER_USERNAME)) {
            logger.error("You can not delete the Nodebelly user");
            return;
        }
        // Create the event stream entry
        Event event = eventService.createEvent(EventId.DELETE, user, null);
        if (user.getAccount() != null) {
            // Send a user is being deleted message
            messageService.sendNodeableAccountMessage(event, user.getAccount(), null);
        }
        // TODO: remove resoruces they own?
        userDAO.deleteById(user.getId());
    }

    @Override
    public User getUserFromInvite(String inviteKey, String accountId) throws UserNotFoundException {
        User u = userDAO.findInvitedUser(inviteKey, accountId);
        if (u == null) {
            throw new UserNotFoundException(inviteKey + "+" + accountId);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }

    @Override
    public User getUserFromSignupKey(String signupKey, String userId) throws UserNotFoundException {
        User u = userDAO.findUser(signupKey, userId);
        if (u == null) {
            throw new UserNotFoundException(signupKey + "+" + userId);
        }
        // Create the event stream entry
        eventService.createEvent(EventId.READ, u, null);
        return u;
    }


    /**
     * User is already reserved via the #createUserRequest or #createInviteUserRequest calls,
     * this just finishes by firing the event and saving the user again if any changes were made
     *
     * @param user
     * @return
     */
    @Override
    public User createUser(User user) {
        user.setUserStatus(User.UserStatus.ACTIVATED);
        validateUserAlias(user);
        userDAO.save(user);
        // Create the event stream entry
        Event event = eventService.createEvent(EventId.CREATE, user, null);
        // Create a user created message
        messageService.sendNodeableAccountMessage(event, user.getAccount(), null);
        return user;
    }


    /**
     * A new user has requested to sign up. This also sets the AccountOriginator value to true.
     *
     * @param user
     * @return
     * @throws UsernameUnavailableException
     */
    @Override
    public User createUserRequest(User user) throws UsernameUnavailableException {
        if (isUsernameAvailable(user.getUsername())) {
            user.setAccountOriginator(true);
            validateUserAlias(user);
            userDAO.save(user);
        } else {
            throw new UsernameUnavailableException(user.getUsername());
        }
        handleUserRequest(user, false);
        return user;
    }

    @Override
    public void recreateUserRequest(User user) throws UserNotFoundException {
        handleUserRequest(user, true);
    }

    private void handleUserRequest(User user, boolean isRecreate) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("userRequestIsNew", !isRecreate);
        // Create the event stream entry
        eventService.createEvent(EventId.CREATE_USER_REQUEST, user, metadata);
    }

    @Override
    public void deleteUserInvite(User user) throws UserNotFoundException {
        userDAO.delete(user);
        // Create the event stream entry
        eventService.createEvent(EventId.DELETE_USER_INVITE_REQUEST, user, null);
    }

    @Override
    public void deleteUsersForAccount(Account account) {
        for (User user : allUsersForAccount(account)) {
            deleteUser(user);
        }
    }

    @Override
    public List<User> allUsersForAccount(Account account) {
        return userDAO.allUsersForAccount(account);
    }

    @Override
    /**
     * SOBA-1401 Does not include disabled users (this is our psuedo "deleted" status)
     */
    public List<User> allEnabledUsersForAccount(Account account) {
        return userDAO.allEnabledUsersForAccount(account);
    }

    @Override
    public Set<Role> getUserRoles() {
        Set<Role> roles = new HashSet<Role>();
        Role r = roleDAO.findRole(Roles.USER_ROLE);
        roles.add(r);
        return roles;
    }

    @Override
    public Set<Role> getAdminRoles() {
        Set<Role> roles = new HashSet<Role>();
        Role r = roleDAO.findRole(Roles.ADMIN_ROLE);
        roles.add(r);
        r = roleDAO.findRole(Roles.USER_ROLE);
        roles.add(r);
        return roles;
    }

    @Override
    public List<Account> getAccounts() {
        return accountDAO.find().asList();
    }

    @Override
    public void addRole(User user, ObjectId roleId) throws UserNotFoundException {
        Role role = roleDAO.get(roleId);
        // don't allow dupes if the same Id
        if (!(user.getRoles().contains(role))) {
            user.addRole(role);
            updateUser(user);
        }
    }

    @Override
    public void removeRole(User user, ObjectId roleId) throws UserNotFoundException {
        Set<Role> newRoles = new HashSet<Role>();
        Set<Role> roleSet = user.getRoles();
        for (Role r : roleSet) {
            if (!(r.getId().equals(roleId))) {
                newRoles.add(r);
            }
        }
        user.setRoles(newRoles);
        updateUser(user);
    }

    @Override
    public ObjectId addToEventLog(User user, String name, JSONObject value, Long timestamp) {
        EventLog eventLog = new EventLog.Builder()
                .user(user)
                .keyValue(name, value, timestamp)
                .build();
        eventLogDAO.save(eventLog);
        return eventLog.getId();
        // Fire event?
    }

    @Override
    public void deletePendingUser(User user) {
        userDAO.delete(user);
        // do not fire event, this user has no resources
    }

    /**
     * <p>Returns a copy of a User object representing the Nodeable SuperUser.  A cached copy of the SuperUser user from
     * the data tier is used as the base object from all copies.  Periodically this cached copy is expired and retrieved
     * again from the data tier.</p>
     * <p/>
     * <p>Callers of this method are allowed to modify the User object returned for purposes such as sending a message
     * from the SuperUser into a specific account.  However, instances of the SuperUser User should not be saved with
     * {@link UserService#updateUser(com.streamreduce.core.model.User)} unless the intention is to modify the authoritative
     * persisted SuperUser user.</p>
     *
     * @return A copy of the SuperUser object
     */
    @Override
    public User getSuperUser() {
        try {
            User cachedSuperUser = superUserCache.get("superuser", new Callable<User>() {
                @Override
                public User call() throws Exception {
                    return userDAO.findUser(Constants.NODEABLE_SUPER_USERNAME);
                }
            });
            return new User.Builder(cachedSuperUser).build();
        } catch (ExecutionException e) {
            logger.warn("Exception encountered when accessing superUser from cache. Falling back to UserDAO", e);
            return userDAO.findUser(Constants.NODEABLE_SUPER_USERNAME);
        }
    }

    @Override
    public User getSystemUser() {
        return userDAO.findUser(Constants.NODEABLE_SYSTEM_USERNAME);
    }

    @Override
    public User getAccountAdmin(Account account) {
        List<User> users = allUsersForAccount(account);
        for (User user : users) {
            for (Role role : user.getRoles()) {
                if (role.getName().equals(Roles.ADMIN_ROLE)) {
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * SOBA-1617 -- bootstrap the Metric collections with proper indexes
     *
     * @param account - a valid account
     */
    private void createMetricInbox(Account account) {

        // create a bogus object
        DB db = genericCollectionDAO.getDatabase(DAODatasourceType.MESSAGE);
        DBCollection collection = db.getCollection(MessageUtils.getMetricInboxPath(account));
        BasicDBObject dummyObj = new BasicDBObject();
        collection.insert(dummyObj);

        // add indexes
        collection.ensureIndex("metricGranularity");
        collection.ensureIndex("metricName");

        // remove bogus object
        collection.remove(dummyObj);
    }

    @Override
    public void handleInitialInsightForAccount(Account account) {
        account.setConfigValue(Account.ConfigKey.RECIEVED_INSIGHTS, true);
        updateAccount(account);
    }

    /**
     * Tests that a user alias is filled with only alphanumeric characters, "-", and "_"
     *
     * @param user - The user whose alias is being tested
     * @throws IllegalArgumentException if user.getAlias contains characters other than alphanumeric characters, "_" and
     *                                  "-"
     */
    void validateUserAlias(User user) {
        if (!User.isValidUserAlias(user.getAlias())) {
            throw new InvalidUserAliasException("User alias contains characters that aren't alphanumeric, dashes, or " +
                    "underscores");
        }
    }
}


