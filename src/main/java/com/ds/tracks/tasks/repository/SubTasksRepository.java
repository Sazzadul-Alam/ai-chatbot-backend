package com.ds.tracks.tasks.repository;

import com.ds.tracks.tasks.model.SubTasks;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubTasksRepository extends MongoRepository<SubTasks, String> {

    List<SubTasks> findAllByParentTaskId(String id);

    SubTasks findFirstById(String id);
    SubTasks findFirstByParentTaskId(String id);
}
