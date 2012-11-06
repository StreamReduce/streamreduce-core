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

package com.streamreduce.security.realm;

import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.User;

import java.util.Collection;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class NodeableAuthorizingRealm extends AuthorizingRealm {

    @Autowired
    protected UserDAO userDAO;
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    protected NodeableAuthorizingRealm() {
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String name = getName();
        Collection c = principals.fromRealm(name);
        if (c.size() < 1) {
            return null;
        }
        ObjectId userId = (ObjectId) c.iterator().next();
        User user = userDAO.get(userId);
        if (user != null) {
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            for (Role role : user.getRoles()) {
                logger.debug("Add Role of " + role.getName() + " to user " + user.getUsername());
                info.addRole(role.getName());
                info.addStringPermissions(role.getPermissions());
            }
            return info;
        } else {
            return null;
        }
    }
}
