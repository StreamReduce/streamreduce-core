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

package com.streamreduce.rest.dto.response;

import com.streamreduce.core.model.User;

import java.util.Map;
import java.util.Set;

public class UserResponseDTO extends AbstractOwnableResponseSobaDTO {

    private String username;
    private String fullname;
    private User.UserStatus status;
    private boolean accountOriginator;
    private Map<String,Object> userConfig;
    private Set<RoleResponseDTO> roles;
    private Long lastLogin;

    public UserResponseDTO() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User.UserStatus getStatus() {
        return status;
    }

    public void setStatus(User.UserStatus status) {
        this.status = status;
    }

    public boolean isAccountOriginator() {
        return accountOriginator;
    }

    public void setAccountOriginator(boolean accountOriginator) {
        this.accountOriginator = accountOriginator;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String name) {
        this.fullname = name;
    }

    public Map<String,Object>getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(Map<String,Object> userConfig) {
        this.userConfig = userConfig;
    }

    public Set<RoleResponseDTO> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleResponseDTO> roles) {
        this.roles = roles;
    }

    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }

}
