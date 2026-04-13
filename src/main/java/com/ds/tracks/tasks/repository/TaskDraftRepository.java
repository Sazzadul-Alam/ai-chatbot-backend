package com.ds.tracks.tasks.repository;

import com.ds.tracks.tasks.model.DraftDto;
import com.ds.tracks.tasks.model.TaskDraft;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDraftRepository extends MongoRepository<TaskDraft, String>{
    DraftDto findFirstById(String id);



    @Query(value="{ 'id': ?0 }", fields="{ 'position': 1 }")
    TaskDraft findPosition(String id);
}
