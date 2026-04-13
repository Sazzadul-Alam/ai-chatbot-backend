package com.ds.tracks.tasks.dao;

import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.tasks.model.TaskDependency;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskDependencyDaoImpl implements TaskDependencyDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Document> dependantTasks(String taskId) {
        String query = "{ aggregate: '" + CollectionName.task_dependency + "', \n" +
                "pipeline: [ \n" +
                "   { $match:{ taskId:'"+taskId+"' } }, \n" +
                "   { $lookup:{  \n" +
                "       from:'"+ CollectionName.task +"', \n" +
                "       let:{ id:{$toObjectId:'$taskId'} }, \n" +
                "       as:'tasks', \n" +
                "       pipeline:[  \n" +
                "           { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] } } }, \n" +
                "           { $project:{ name:1, priority:1, status:1, id:{ $toString: '$_id' }, _id:0 } } \n" +
                "       ] \n" +
                "    }}, \n" +
                "    { $unwind:'$tasks' }, \n" +
                "    { $replaceRoot:{ newRoot:'$tasks' } } \n" +
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) (((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch"));
    }

    @Override
    public List<Document> canCloseTask(String taskId) {
        String query = "{ aggregate: '" + CollectionName.task_dependency + "', \n" +
                "pipeline: [ \n" +
                "   { $match:{ taskId:'"+taskId+"' } }, \n" +
                "   { $lookup:{  \n" +
                "       from:'"+ CollectionName.task +"', \n" +
                "       let:{ id:{$toObjectId:'$dependency'} }, \n" +
                "       as:'tasks', \n" +
                "       pipeline:[  \n" +
                "           { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] }, status:{$ne:'Completed' } } }, \n" +
                "           { $project:{ name:1, priority:1, status:1, id:{ $toString: '$_id' }, _id:0 } } \n" +
                "       ] \n" +
                "    }}, \n" +
                "    { $unwind:'$tasks' }, \n" +
                "    { $replaceRoot:{ newRoot:'$tasks' } } \n" +
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) (((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch"));
    }

    @Override
    public Object findAllLinkedTaskIds(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("taskId").is(id));
        query.fields().include("dependency");
        return mongoTemplate.find(query, TaskDependency.class).stream().map(TaskDependency::getDependency).collect(Collectors.toList());
    }
}
