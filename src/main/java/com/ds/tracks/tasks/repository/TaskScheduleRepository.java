package com.ds.tracks.tasks.repository;

import com.ds.tracks.tasks.model.TaskSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskScheduleRepository extends MongoRepository<TaskSchedule, String> {
    void deleteAllByTaskId(String taskId);
}
