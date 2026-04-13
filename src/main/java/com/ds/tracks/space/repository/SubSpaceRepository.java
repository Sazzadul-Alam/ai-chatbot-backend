package com.ds.tracks.space.repository;

import com.ds.tracks.space.model.SubSpace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubSpaceRepository extends MongoRepository<SubSpace, String> {
    Optional<SubSpace> findFirstById(String id);

    @Query(value = "{ 'spaceId': ?0 }", fields = "{ 'name': 1 }")
    List<SubSpace> findAllBySpaceId(String spaceId);


    @Query(value = "{ 'spaceId': ?0, 'folderId': null }", fields = "{ 'name': 1, 'color':1 }")
    List<SubSpace> findAllBySpaceIdAndFolderIdIsNull(String spaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'configurations':1, 'id':0 }")
    List<SubSpace> findConfigurationsById(String subSpaceId);

    @Query(value="{ 'id': ?0 }", fields="{ 'spaceId' : 1, 'id':0 }")
    SubSpace findSpaceId(String id);

    @Query(value="{ 'id': ?0 }", fields="{ 'spaceId' : 1, 'workspaceId':1 }")
    SubSpace findSpaceIdAndWorkspaceId(String id);

    @Query(value="{ 'spaceId': ?0 }", fields="{ 'id': 1 }")
    List<SubSpace> findAllIdBySpaceId(String id);
}
