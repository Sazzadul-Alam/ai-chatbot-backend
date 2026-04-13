package com.ds.tracks.backlog;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacklogRepository extends MongoRepository<Backlog, String>, BacklogDao {
    List<Backlog> findAllBySpaceIdAndSubSpaceIdIsNull(String spaceId);
    List<Backlog> findAllBySubSpaceId(String subSpaceId);

    Backlog findFirstById(String id);
    List<Backlog> findAllBySpaceIdAndSubSpaceId(String spaceId, String subspaceId);

    boolean existsByParentIdAndSubSpaceId(String id, String subspaceId);
}
