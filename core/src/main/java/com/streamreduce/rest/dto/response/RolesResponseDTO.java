package com.streamreduce.rest.dto.response;

import java.util.List;

public class RolesResponseDTO {

    List<RoleResponseDTO> roles;

    public List<RoleResponseDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleResponseDTO> roles) {
        this.roles = roles;
    }

}
