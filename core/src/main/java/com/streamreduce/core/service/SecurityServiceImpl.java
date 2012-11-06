package com.streamreduce.core.service;

import com.streamreduce.ValidationException;
import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.core.dao.ConnectionDAO;
import com.streamreduce.core.dao.RoleDAO;
import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.model.APIAuthenticationToken;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.resource.ErrorMessages;

import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityServiceImpl extends AbstractService implements SecurityService {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private RoleDAO roleDAO;
    @Autowired
    private ConnectionDAO connectionDAO;

    @Override
    public User getCurrentUser() {
        return getShiroUser();
    }

    @Override
    public Connection getCurrentGatewayConnection() {
        final String apiKey = (String) SecurityUtils.getSubject().getPrincipal();
        if (apiKey != null) {
            return getByApiKey(apiKey, GatewayProvider.TYPE);
        } else {
            throw new AuthenticationException("A gateway connection must be logged in!");
        }
    }

    @Override
    public void logoutCurrentUser(String token) {
        Subject s = SecurityUtils.getSubject();
        if (s != null) {
            logger.debug("Logout user: kill the Shiro Session");
            s.logout();
        } else {
            logger.debug("Logout user, subject not found in SecurityUtils");
        }
    }


    @Override
    public APIAuthenticationToken issueAuthenticationToken(User user) throws ValidationException, UserNotFoundException {
        APIAuthenticationToken newToken = new APIAuthenticationToken();
        user.setAuthenticationToken(newToken);
        userDAO.save(user);
        return newToken;
    }


    @Override
    public boolean hasRole(String roleName) {
        Subject currentUser = SecurityUtils.getSubject();
        return (currentUser.hasRole(roleName));
    }

    @Override
    public Role findRole(String role) {
        return roleDAO.findRole(role);
    }

    @Override
    public Set<User> getActiveUsers(Account account, Long maxInactivity) {
        // TODO: cache this
        return userDAO.getActiveLoggedInUsers(account, maxInactivity);
    }

    @Override
    public User getUserFromAuthenticationToken(final String token) {
        return userDAO.findByAuthToken(token);
    }

    @Override
    public Connection getByApiKey(final String token, final String type) {
        return connectionDAO.getByAPIKey(token, type);
    }

    /**
     * Sort of a fragile wrapper to get the User from the User Realm or the Gateway Realm
     * TODO: we need a better way to store these
     *
     * @return - The User who is logged in, or the User who is owns the IMG connection request.
     */
    private User getShiroUser() {
        try {
            final Object id = SecurityUtils.getSubject().getPrincipal();
            if (id != null) {
                if (id instanceof ObjectId) {
                    return userDAO.get((ObjectId) id);
                } else if (id instanceof String) {  // must be IMG
                    Connection connection = connectionDAO.getByAPIKey((String) id, GatewayProvider.TYPE);
                    return connection.getUser();
                }
            }
        } catch (UnavailableSecurityManagerException e) {
            throw new AuthenticationException(e.getMessage());
        }
        throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
    }


}
