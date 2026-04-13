package com.ds.tracks.user.dao;

import com.ds.tracks.commons.models.enums.PermissionLayer;
import com.ds.tracks.user.model.dto.UserPermissionDto;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.bulk.UpdateRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserPermissionDao {
    long updateCapacity(UserPermissionDto userPermissionDto);
    UpdateResult changeRole(String id, String spaceId, String role);
    UpdateResult changeDesignation(String id, String spaceId, String designation);
    List<Map<String, Object>> getUsersForSpace(String spaceId);

    List<String> findAllSpaceIdByWorkspaceIdAndPermissionForAndUserId(String workspaceId, String userId);
    Object getUsersInWorkspace(String id);

    List<String> findAllSpaceIdByWorkspaceIdAndUserIdAndPermissionForAndRoleNe(String workspaceId, String userId, String space, String observer);
}
