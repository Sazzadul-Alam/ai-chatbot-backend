package com.ds.tracks.user.model;

import com.ds.tracks.commons.models.enums.ManagementRoles;
import com.ds.tracks.commons.models.enums.PermissionLayer;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Builder
@Document("users_permission")
@NoArgsConstructor
@AllArgsConstructor
public class UsersPermission {

    @Id
    private String id;
    private String userId;
    private String workspaceId;
    private String spaceId;
    private String subSpaceId;
    private String taskId;
    private String subtaskId;
    private String loginId;
    private PermissionLayer permissionFor;
    private ManagementRoles role;
    private Date createdAt;
    private String createdBy;
    private Integer capacity;
}
