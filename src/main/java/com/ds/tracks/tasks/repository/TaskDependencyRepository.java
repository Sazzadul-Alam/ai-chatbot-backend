package com.ds.tracks.tasks.repository;

import com.ds.tracks.tasks.dao.TaskDependencyDao;
import com.ds.tracks.tasks.model.TaskDependency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDependencyRepository extends MongoRepository<TaskDependency, String>, TaskDependencyDao {
    void deleteAllByTaskId(String taskId);
}
