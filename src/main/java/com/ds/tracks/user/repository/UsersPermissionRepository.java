package com.ds.tracks.user.repository;

import com.ds.tracks.commons.models.enums.ManagementRoles;
import com.ds.tracks.commons.models.enums.PermissionLayer;
import com.ds.tracks.user.dao.UserPermissionDao;
import com.ds.tracks.user.model.UsersPermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UsersPermissionRepository extends MongoRepository<UsersPermission, String>, UserPermissionDao {

    @Query(value="{ 'loginId': ?0, 'spaceId': ?1, 'permissionFor': ?2 }", fields="{ 'role' : 1, 'id':0 }")
    UsersPermission findRoleByLoginIdAndSpaceIdAndPermissionFor(String loginId, String spaceId, PermissionLayer space);

    @Query(value="{ 'spaceId': ?0, 'loginId' : ?1, 'permissionFor': 'SPACE' }", fields="{ 'role' : 1, 'id':0 }")
    UsersPermission findRoleBySpaceIdAndLoginId(String spaceId, String name);

    boolean existsBySpaceIdAndLoginId(String spaceId, String email);

    boolean existsByWorkspaceIdAndLoginIdAndPermissionFor(String id, String email, PermissionLayer workspace);

    UsersPermission findFirstById(String id);

    @Query(value="{ 'workspaceId': ?0, 'permissionFor': ?1 }", fields="{ 'role' : 1, 'userId':1, 'loginId':1, 'id':0 }")
    List<UsersPermission> findAllByWorkspaceIdAndPermissionFor(String workspaceId, PermissionLayer permissionLayer);

    void deleteBySpaceIdAndUserIdAndPermissionFor(String space, String id, PermissionLayer space1);

    boolean existsBySpaceIdAndLoginIdAndPermissionForAndRoleIn(String spaceId, String name, PermissionLayer space, List<ManagementRoles> asList);

    boolean existsByWorkspaceIdAndLoginIdAndPermissionForAndRole(String workspaceId, String name, PermissionLayer space, ManagementRoles admin);

    UsersPermission findOneBySpaceIdAndLoginId(String spaceId, String email);


    List<UsersPermission> findAllBySpaceId(String space);
}
