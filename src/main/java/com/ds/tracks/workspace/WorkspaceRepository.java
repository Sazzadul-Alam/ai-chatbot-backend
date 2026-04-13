package com.ds.tracks.workspace;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, String> {
    Workspace findFirstById(String id);
    @Query(value = "{ 'id': ?0 }", fields = "{ 'name':1, 'image':1, 'id': 1 }")
    Workspace findFirstNameAndImageById(String id);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'permissions':1, 'id':0 }")
    Optional<Workspace> findPermissionsById(String id);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'designations':1, 'id':1 }")
    Workspace findFirstDesignationsById(String id);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'configurations':1, 'id':1 }")
    Workspace findFirstConfiguration(String workspaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'vatPercentage':1, 'id':1 }")
    Workspace findVatPercentageById(String id);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'stages':1, 'types':1, 'categories': 1 }")
    Workspace findTaskConfigsById(String id);

    @Query(value = "{  }", fields = "{ 'stages':1, 'types':1, 'categories': 1 }")
    Workspace findFirst();
}
