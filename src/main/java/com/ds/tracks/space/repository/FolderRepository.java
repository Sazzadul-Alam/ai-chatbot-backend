package com.ds.tracks.space.repository;

import com.ds.tracks.space.model.Folder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FolderRepository extends MongoRepository<Folder, String> {
    @Query(value="{ 'id': ?0 }", fields="{ 'spaceId' : 1, 'subspaces':1, 'id':0 }")
    Folder findSpaceAndSubspaces(String id);

    Folder findBySpaceIdAndName(String spaceId, String category);
}
