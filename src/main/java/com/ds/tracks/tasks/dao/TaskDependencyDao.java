package com.ds.tracks.tasks.dao;

import org.bson.Document;

import java.util.List;

public interface TaskDependencyDao {
    List<Document> dependantTasks(String taskId);
    List<Document> canCloseTask(String taskId);

    Object findAllLinkedTaskIds(String id);
}
