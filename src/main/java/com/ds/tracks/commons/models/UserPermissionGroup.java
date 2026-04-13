package com.ds.tracks.commons.models;

import com.ds.tracks.commons.models.enums.ManagementRoles;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPermissionGroup {
    private ManagementRoles role;
    private String userId;

    public UserPermissionGroup(ManagementRoles role, String userId) {
        this.role = role;
        this.userId = userId;
    }

    public UserPermissionGroup(String userId) {
        this.role =  ManagementRoles.USER;
        this.userId = userId;
    }

    public UserPermissionGroup() {
    }
}
