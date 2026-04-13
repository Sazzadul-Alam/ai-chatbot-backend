package com.ds.tracks.tasks.dao;

import com.ds.tracks.comments.Comment;
import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.files.FileInfo;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.model.SpaceConfigurations;
import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.tasks.model.SubTasks;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.model.TaskSchedule;
import com.ds.tracks.tasks.model.Tasks;
import com.ds.tracks.tasks.model.dto.ModificationDto;
import com.ds.tracks.tasks.model.dto.TaskPositionDto;
import com.ds.tracks.tasks.model.dto.TaskStatus;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.*;


@Slf4j
@Repository
@RequiredArgsConstructor
public class TasksDaoImpl implements TasksDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Map<String, Object>> getAllIssueGroupedByStatus(String spaceId, String subSpaceId) {
        String query = "{ aggregate: '" + CollectionName.task + "', \n" +
                "pipeline: [\n" +
                "   { $project:{_id:0,id:{\"$toString\":\"$_id\"},name:1,status:1,priority:1,issuedTo:1,type:1,deadline:1, subSpaceId:1, spaceId:1, storyPoint:{$ifNull:[\"$storyPoint\", 0]}}},\n" +
                    Utils.spaceSubSpaceAggStage(spaceId,subSpaceId)+
                "    { $lookup: {\n" +
                "        from:\"user\",\n" +
                "        let: { \"issuedTo\": \"$issuedTo\" },\n" +
                "        pipeline: [\n" +
                "          { $project: {_id:0, \"uid\": { \"$toString\": \"$_id\" },fullName:1,image:1}},\n" +
                "          { $match: { $expr: { $eq: [ \"$uid\", \"$$issuedTo\" ] } } },\n" +
                "        ],\n" +
                "        as: \"users\"\n" +
                "      },  \n" +
                "    },\n" +
                "    { $lookup: {\n" +
                "        from:\"sub_tasks\",\n" +
                "        let: { \"parentTaskId\": \"$id\" },\n" +
                "        pipeline: [\n" +
                "          { $project: {_id:0, \"parentTaskId\": 1,\"storyPoint\":{$ifNull:[\"$storyPoint\", 0]}}},\n" +
                "          { $match: { $expr: { $eq: [ \"$parentTaskId\", \"$$parentTaskId\" ] } } },\n" +
                "          { $group:{\"_id\":\"totalStoryPoint\", \"value\":{$sum:\"$storyPoint\"}}},\n" +
                "          { $project:{\"totalStoryPoint\":\"$value\", _id:0}}" +
                "        ],\n" +
                "        as: \"subTask\"\n" +
                "      },\n" +
                "    },\n" +
                "    { $unwind : {\"path\": \"$subTask\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "    { $project:{_id:0,id:\"$id\",name:\"$name\",status:\"$status\",priority:\"$priority\",type:\"$type\",deadline:\"$deadline\",issuedTo:\"$users.fullName\", storyPoint:{ $max:[\"$storyPoint\", \"$subTask.totalStoryPoint\"] }}},\n" +
                "    { $unwind : {\"path\": \"$issuedTo\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "    { $unwind : {\"path\": \"$image\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "    { $group:{ _id:\"$status\", tasks:{$push: \"$$ROOT\"}, count:{\"$sum\":1}}},\n" +
                "    { $project:{ _id:0, \"title\":\"$_id\", \"tasks\":1 } } "+
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";

        return (List<Map<String, Object>>) (((Map<Object, Object>) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch"));
    }



    @Override
    public Map<String, Object> getGroupedTasks(String spaceId, String subSpaceId, String userId, String startDate, String endDate, String tracker, String priority, List<String> tags) {
        String match = isValidString(subSpaceId) ? "subspaceId: '"+subSpaceId+"' " : "spaceId: '"+spaceId+"' subspaceId:{ $eq:null } ";
        if(isValidString(userId)){
            match = match +", assignedTo:'"+userId+"' ";
        }
        match = match + ", scheduleDate: {" +
                " $gte:ISODate('"+startDate+"T00:00:00.000+06:00')," +
                " $lte:ISODate('"+endDate+"T23:59:59.999+06:00') }";
        String filter = "";
        if(isValidString(priority)){
            filter = filter.concat(", ").concat("priority:").concat(priority);
        }
        if(isValidString(tracker)){
            filter = filter.concat(", ").concat("generatedId:{$regex:").concat(quotedString(tracker.trim())).concat(", $options:'i'}");
        }
        filter = filter.concat(", ").concat(listToStringQuery("tags", tags));
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "', \n" +
                "pipeline: [\n" +
                "    { $match: { "+match+" }},  \n" +
                "    { $group: { _id:{ id:'$taskId', assigned:'$assignedTo' } } },\n" +
                "    { $replaceRoot: { newRoot:'$_id' } },\n" +
                "    { $lookup:{\n" +
                "        from:'"+ CollectionName.task +"',\n" +
                "        let:{ 'id':{$toObjectId:'$id'}, 'issued':'$assigned' },\n" +
                "        pipeline:[\n" +
                "            {$project:{ name:1, tags:1, position:1, generatedId:1, type:1,  priority:1, locked:1, issuedTo:'$$issued', _id:1, completion:1, status:1, startDate:1, id:{$toString:'$_id'}, deadline:1 }},\n" +
                "            {$match:{ $expr:{ $eq:['$_id', '$$id'] } "+filter+" }},\n" +
                "            { $sort:{ _id:-1} }," +
                "        ],\n" +
                "        as:'task'\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$task' } },\n" +
                "    { $replaceRoot: { newRoot:'$task' } }, \n" +
                "    { $facet:{\n" +
                "        assigned:[\n" +
                "            { $group:{ _id:'$id', data:{ $push:'$issuedTo' } } },\n" +
                "            { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}},\n" +
                "            { $replaceRoot: { newRoot:'$data' } }, \n" +
                "        ],\n" +
                "        data:[\n" +
                "            { $project:{issuedTo:0} }," +
                "            { $group:{ _id:'$id', data:{ $push:'$$ROOT' } } },\n" +
                "            { $project:{ _id:0, data:{ $arrayElemAt: [ '$data', 0 ] } } }," +
                "            { $replaceRoot:{ newRoot:'$data' } }, \n" +
                "            { $sort:{ position:1, _id:1 } },\n" +
                "            { $group:{ _id:'$status', tasks:{ $push:'$$ROOT' } }},\n"+
                "            { $group:{ _id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$tasks'}]  ]} }}},\n" +
                "            { $replaceRoot: { newRoot:'$data' } }," +
                "            { $sort:{ _id:-1} },\n" +
                "        ]," +
                "       users:[\n" +
                "            { $project:{ issuedTo:1 } },\n" +
                "            { $group: { _id:'$issuedTo'} },\n" +
                "            { $lookup:{\n" +
                "                from:'user',\n" +
                "                let:{ userId:{ $toObjectId:'$_id' }  },\n" +
                "                pipeline:[\n" +
                "                    { $match:{ $expr: { $eq: [ \"$_id\", \"$$userId\" ] } } },\n" +
                "                    { $project:{ _id:0, 'name':'$fullName', image:1, id:{$toString:'$_id'}} },\n" +
                "                ],\n" +
                "                as:'users'\n" +
                "             } },\n" +
                "             { $unwind:{ path:'$users'} },\n" +
                "             { $replaceRoot: { newRoot:'$users' } }, \n" +
                "             { $group:{_id:'$id', data:{$push:{ name:'$name', image:'$image' }}} },\n" +
                "             { $unwind:{path:'$data'}},\n" +
                "             { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}},\n" +
                "             { $replaceRoot: { newRoot:'$data' } }, \n" +
                "             \n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$assigned', preserveNullAndEmptyArrays:true }},\n" +
                "    { $unwind:{ path:'$data', preserveNullAndEmptyArrays:true }},\n" +
                "    { $unwind:{ path:'$users', preserveNullAndEmptyArrays:true }}," +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List list = (List) (((Map<Object, Object>) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch"));
        if(Objects.isNull(list) || list.isEmpty()){
            return new HashMap<>();
        }
        return (Map<String, Object>) list.get(0);
    }
    @Override
    public Map<String, Object> findDraftById(String taskId) {
        final String query = "{ aggregate: '" + CollectionName.tasks_draft + "', \n" +
                "pipeline: [" +
                "    { $match:{ _id:ObjectId('"+taskId+"') } }"+
                "    { $project:{\n" +
                "        id:{\"$toString\": \"$_id\"}, \n" +
                "        name:1,\n" +
                "        description:1,\n" +
                "        generatedId:1,\n" +
                "        spaceId:1,\n" +
                "        type:1,\n" +
                "        priority:1,\n" +
                "        startDate:1,\n" +
                "        deadline:1,\n" +
                "        issuedTo:1,\n" +
                "        duration:1,\n" +
                "        status:1,\n" +
                "        category:1,\n" +
                "        tags:1,\n" +
                "        testCases:1,\n" +
                "        issues:1,\n" +
                "        tasks:1,\n" +
                "        issuedBy:1,\n" +
                "        severity:1,\n" +
                "        storyPoint:1,\n" +
                "        locked:1,\n" +
                "        history:1\n" +
                "        backlogs:1\n" +
                "    }}," +
                " ],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? null : list.get(0);
    }
    @Override
    public Map<String, Object> findTaskById(String taskId) {
        final String query = "{ aggregate: '" + CollectionName.task + "', \n" +
                "pipeline: [" +
                "    { $match:{ _id:ObjectId('"+taskId+"') } }"+
                "    { $project:{\n" +
                "        id:{\"$toString\": \"$_id\"}, \n" +
                "        name:1,\n" +
                "        description:1,\n" +
                "        generatedId:1,\n" +
                "        spaceId:1,\n" +
                "        type:1,\n" +
                "        priority:1,\n" +
                "        startDate:1,\n" +
                "        deadline:1,\n" +
                "        issuedTo:1,\n" +
                "        duration:1,\n" +
                "        status:1,\n" +
                "        category:1,\n" +
                "        tags:1,\n" +
                "        testCases:1,\n" +
                "        issues:1,\n" +
                "        tasks:1,\n" +
                "        issuedBy:1,\n" +
                "        severity:1,\n" +
                "        storyPoint:1,\n" +
                "        locked:1,\n" +
                "        history:1\n" +
                "        backlogs:1\n" +
                "    }}," +
                " ],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Document findTaskLocationById(String taskId) {
        final String query = "{ aggregate: '" + CollectionName.task + "', \n" +
                "pipeline: [\n" +
                "    { $match:{ _id:ObjectId('"+taskId+"') }},\n" +
                "    { $project:{ _id:0, spaceId:1, subSpaceId:1 } },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ id:{ $toObjectId:'$spaceId' } },\n" +
                "        as:'space',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:['$$id', '$_id'] } } },\n" +
                "            { $project:{ _id:0, name:1 } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$space' },\n" +
                "    { $lookup:{\n" +
                "        from:'sub_spaces',\n" +
                "        let:{ id:{ $toObjectId:'$subSpaceId' } },\n" +
                "        as:'segment',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:['$$id', '$_id'] } } },\n" +
                "            { $project:{ _id:0, name:1, folderId:1 } },            \n" +
                "            { $lookup:{\n" +
                "                from:'folder',\n" +
                "                let:{ id:{$convert: { input: '$folderId', to: 'objectId', onError: '', onNull: '' } } },\n" +
                "                as:'folder',\n" +
                "                pipeline:[\n" +
                "                    { $match:{ $expr:{ $eq:['$$id', '$_id'] } } },\n" +
                "                    { $project:{ _id:0, name:1 } }\n" +
                "                ]\n" +
                "            }},\n" +
                "            { $unwind:{ path:'$folder', preserveNullAndEmptyArrays:true} }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$segment' },\n" +
                "    { $project:{ space:'$space.name', segment:'$segment.name', folder:'$segment.folder.name' } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Document> list = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return Objects.isNull(list) || list.isEmpty() ? new Document() : list.get(0);
    }


    @Override
    public Map<String, Object> findSubTaskById(String subTaskId) {
        final String query = "{ aggregate: '" + CollectionName.subtask + "', \n" +
                "pipeline: [" +
                "    { $project:{\n" +
                "        id:{\"$toString\": \"$_id\"}, \n" +
                "        name:1,\n" +
                "        description:1,\n" +
                "        type:1,\n" +
                "        priority:1,\n" +
                "        spaceId:1,\n" +
                "        startDate:1,\n" +
                "        deadline:1,\n" +
                "        generatedId:1,\n" +
                "        issuedTo:1,\n" +
                "        storyPoint:1,\n" +
                "        duration:1,\n" +
                "        status:1,\n" +
                "        history:1,\n" +
                "        completion:1,\n" +
                "        category:1,\n" +
                "        severity:1,\n" +
                "        locked:1,\n" +
                "        actualDuration:1\n" +
                "    }}," +
                "   { $match:{id:\"" + subTaskId + "\"  } }," +
                "   { $lookup: {\n" +
                "        from:\"" + CollectionName.effort_log + "\",\n" +
                "        let: { \"subTaskId\": { \"$toString\": \"$_id\" } },\n" +
                "        pipeline: [\n" +
                "           { $project: {\n" +
                "               _id:0, \n" +
                "               \"id\": { \"$toString\": \"$_id\" }, \n" +
                "               taskId:1,\n" +
                "               subTaskId:1,\n" +
                "               duration:1, \n" +
                "               completion:1,\n" +
                "               description:1,\n" +
                "               logDate:1,\n" +
                "               createdAt:1,\n" +
                "               createdBy:1\n" +
                "           }},\n" +
                "           { $match: {  $expr: { $eq: [ \"$subTaskId\", \"$$subTaskId\" ] } } },\n" +
                "        ],\n" +
                "        as: \"efforts\"\n" +
                "    }},     \n" +
                "    { $project:{\n" +
                "        _id:0, \n" +
                "        id:{\"$toString\": \"$_id\"}, \n" +
                "        name:\"$name\",\n" +
                "        description:\"$description\",\n" +
                "        type:\"$type\",\n" +
                "        priority:\"$priority\",\n" +
                "        startDate:\"$startDate\",\n" +
                "        deadline:\"$deadline\",\n" +
                "        issuedTo:\"$issuedTo\",\n" +
                "        spaceId:\"$spaceId\",\n" +
                "        duration:\"$duration\",\n" +
                "        status:\"$status\",\n" +
                "        efforts:\"$efforts\",\n" +
                "        history:\"$history\",\n" +
                "        generatedId:\"$generatedId\",\n" +
                "        severity:\"$severity\",\n" +
                "        storyPoint:\"$storyPoint\",\n" +
                "        category:\"$category\",\n" +
                "        completion:\"$completion\",\n" +
                "        locked:'$locked',\n" +
                "        actualDuration:\"$actualDuration\"\n" +
                "    }}" +
                " ],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public UpdateResult update(String id, String field, Object value, TasksHistory log) {
        Update update = new Update();
        update.set(field, value);
        if(Objects.nonNull(log)){
            update.push("history", log);
        }
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, Tasks.class);
    }

    @Override
    public UpdateResult update(String source, String id, List<IdNameRelationDto> updates, TasksHistory log) {
        Update update = new Update();
        updates.forEach(up->update.set(up.getId().toString(), up.getName()));
        update.push("history", log);
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, Objects.equals(source, "task") ? CollectionName.task : CollectionName.subtask);
    }

    @Override
    public UpdateResult updateSubtask(String id, String field, Object value, TasksHistory log) {
        Update update = new Update();
        update.set(field, value);
        update.push("history", log);
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, SubTasks.class);
    }
    @Override
    public UpdateResult updateSubtaskByTaskId(String id, String field, Object value, TasksHistory log) {
        Update update = new Update();
        update.set(field, value);
        update.push("history", log);
        return mongoTemplate.updateMulti(new Query(Criteria.where("parentTaskId").is(id)), update, SubTasks.class);
    }

    @Override
    public List<TaskStatus> getAllStatusBySpaceOrSubSpace(String spaceId, String subSpaceId){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(isValidString(subSpaceId) ? subSpaceId : spaceId));
        query.fields().include("configurations.status");
        SpaceConfigurations config = null;
        try {
            if(isValidString(subSpaceId)){
                SubSpace subspace = mongoTemplate.findOne(query, SubSpace.class);
                if(Objects.nonNull(subspace) ){
                    config = subspace.getConfigurations();
                }
            } else {
                Space space = mongoTemplate.findOne(query, Space.class);
                if(Objects.nonNull(space) ){
                    config = space.getConfigurations();
                }
            }

        } catch (Exception ignore){}
        return Objects.nonNull(config) ? config.getStatus() : Collections.emptyList();
    }

    @Override
    public UpdateResult reassignForWorkload(String id, String userId) {
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), new Update().set("assignedTo", userId), TaskSchedule.class);
    }

    @Override
    public UpdateResult changeDateOfSchedule(String id, Date date, String taskDate) {
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), new Update().set("scheduleDate", date).set("dateString", taskDate), TaskSchedule.class);
    }

    @Override
    public Map<String, List<Object>> findTasksForCalenderView(String startDate, String endDate, String spaceId, String subspaceId, String userId){
        String filter = isValidString(subspaceId) ? " subspaceId:'"+subspaceId+"', " : " spaceId:'"+spaceId+"', subspaceId:{$eq:null},";
        filter = filter.concat(isValidString(userId) ? " assignedTo: '"+userId+"', " : "");
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "'," +
                " pipeline: [" +
                "    { $match:{" + filter +
                "        scheduleDate:{'$gte':ISODate('"+startDate+"T00:00:00.000+06:00'), '$lte':ISODate('"+endDate+"T23:59:59.999+06:00')}\n" +
                "    }},\n" +
                "    { $project:{_id:0, taskId:1,  dateString:1}},\n" +
                "    { $group:{ _id:'$taskId', dates:{$push:'$dateString'} }},\n" +
                "    { $lookup:{\n" +
                "            from:'tasks',\n" +
                "            let:{id:{$toObjectId:'$_id'}},\n" +
                "            pipeline:[\n" +
                "                {$match:{$expr:{$eq:['$_id', '$$id']}}},\n" +
                "                {$project:{_id:0, priority:1}}\n" +
                "            ],\n" +
                "            as:'task'\n" +
                "    }},\n" +
                "    {$unwind:{path:'$task'}},\n" +
                "    {$unwind:{path:'$dates'}},\n" +
                "    {$project:{'date':'$dates', priority:'$task.priority', _id:0}},\n" +
                "    { $group:{_id:'$date', data:{$push:'$priority'}}},\n" +
                "    { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}},\n" +
                "    { $replaceRoot:{ newRoot:'$data' }},\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Object> list = (List)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new HashMap<>() : (Map<String, List<Object>>) list.get(0);
    }

    @Override
    public List<Map<String, Object>> getAllTaskByDate(String spaceId, String subspaceId, String taskDate, String priority, String userId) {
        String filter = isValidString(subspaceId) ? " subspaceId:'"+subspaceId+"', " : " spaceId:'"+spaceId+"', subspaceId:{$eq:null},";
        if(isValidString(userId)){
            filter = filter.concat(" assignedTo:'"+userId+"',");
        }
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "'," +
                " pipeline:[\n" +
                "    { $match:{\n" + filter +
                "        scheduleDate:{\n" +
                "            $gte:ISODate('"+taskDate+"T00:00:00.000+06:00'), \n" +
                "            $lte:ISODate('"+taskDate+"T23:59:59.999+06:00')\n" +
                "        }\n" +
                "    }},\n" +
                "    { $project:{_id:0, taskId:1}},\n" +
                "    { $group:{ _id:'$taskId' }},\n" +
                "    { $lookup:{\n" +
                "            from:'tasks',\n" +
                "            let:{id:{$toObjectId:'$_id'}},\n" +
                "            pipeline:[\n" +
                "                {$match:{$expr:{$and:[{$eq:['$_id', '$$id']},"+(isValidString(priority) ? "{$eq:['$priority',"+priority+"]}":"")+"] }}},\n" +
                "                {$project:{_id:0, id:{$toString:'$_id'}, generatedId:1, name:1, type:1, category:1, priority:1, deadline:1, status:1}}\n" +
                "            ],\n" +
                "            as:'task'\n" +
                "    }},\n" +
                "    {$unwind:{path:'$task'}},\n" +
                "    {$replaceRoot:{newRoot:'$task'}}\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<Map<String, String>> getAllAssignedUsers(String taskId) {
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "'," +
                " pipeline:[\n" +
                "    {$match:{ taskId:'"+taskId+"' }},\n" +
                "    {$project:{assignedTo:1, _id:0}},\n" +
                "    {$group:{_id:'$assignedTo'}},\n" +
                "    {$lookup:{\n" +
                "        from:'user',\n" +
                "        let:{'id':{$toObjectId:'$_id'}},\n" +
                "        pipeline:[\n" +
                "            { $match:{$expr:{$eq:['$_id', '$$id']}}},\n" +
                "            { $project:{_id:0, id:{$toString:'$_id'},name:'$fullName', email:'$loginId', image:1}}\n" +
                "        ],\n" +
                "        as:'users' \n" +
                "   }},\n" +
                "   { $unwind:{path:'$users'} },\n" +
                "   {$replaceRoot:{newRoot:'$users'}}\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, String>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<Map<String, Object>> getAllAssignedTasks(String userId, String startDate, String endDate) {
        final String dateRange = "" +
                " $gte:ISODate('"+startDate+"T00:00:00.000+06:00')," +
                " $lte:ISODate('"+endDate+"T23:59:59.999+06:00')";
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "'," +
                " pipeline:[\n" +
                "    { $match:{ \n" +
                "        assignedTo:'"+userId+"', \n" +
                "        scheduleDate:{"+dateRange+"} \n" +
                "    }},\n" +
                "    { $project:{ taskId:1 }},\n" +
                "    { $group:{ _id:'$taskId' }},\n" +
                "    { $lookup:{\n" +
                "        from:'tasks',\n" +
                "        let:{ 'id':{$toObjectId:'$_id'} },\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] } } },\n" +
                "            { $project:{ name:1, type:1, priority:1, deadline:1, spaceId:1, completion:1, status:1, id:{$toString:'$_id'}, _id:0}}\n" +
                "        \n" +
                "        ],\n" +
                "            as:'tasks'\n" +
                "        \n" +
                "    }},\n" +
                "    { $unwind:{ path:'$tasks'} },\n" +
                "    { $replaceRoot:{ newRoot:'$tasks'} },\n" +
                "    { $group:{ _id:'$spaceId', tasks:{$push:'$$ROOT'}, count:{ $sum:1 } } },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ 'id':{$toObjectId:'$_id'} },\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] } } },\n" +
                "            { $project:{ name:1, color:1}},\n" +
                "        ],\n" +
                "            as:'space'\n" +
                "     } },\n" +
                "    { $unwind:{ path:'$space'} },\n" +
                "    { $project:{ tasks:1, _id:0, name:'$space.name', color:'$space.color', count:1} }\n" +
                "     \n" +
                "\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
    @Override
    public List<Map<String, Object>> getAllSpaces(String userId, String workspaceId) {
        final String query= "{ aggregate: '" + CollectionName.users_permission + "'," +
                " pipeline:[" +
                "   { $match:{ \n" +
                "        workspaceId:'"+workspaceId+"', \n" +
                "        userId:'"+userId+"',\n" +
                "        spaceId:{$ne:null}\n" +
                "    }},\n" +
                "    { $project:{ spaceId:1, _id:0} },\n" +
                "    { $group:{ _id:'$spaceId'} },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ id: { $toObjectId:'$_id' } },\n" +
                "        pipeline:[\n" +
                "             { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] } } },\n" +
                "             { $project:{ name:1, color:1, _id:0, id:{ $toString:'$_id' }}}\n" +
                "         ],\n" +
                "         as:'space'\n" +
                "     }},\n" +
                "     { $unwind:{ path:'$space'} } ,\n" +
                "     { $replaceRoot: { newRoot:'$space' } }], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public UpdateResult updateTaskOrSubtaskForEffortLog(String id, Double completion, Double duration, boolean isSubtask) {
        Update update = new Update();
        update.set("completion", completion);
        update.inc("actualDuration", duration);
        return mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)), update, isSubtask ? CollectionName.subtask : CollectionName.task);
    }

    @Override
    public Object getPagedResponse(PagedResponseRequest responseRequest) {
        if(Objects.isNull(responseRequest.getSize())){
            responseRequest.setSize(30);
        }
        if(Objects.isNull(responseRequest.getPage())){
            responseRequest.setPage(0);
        }
        if(Objects.isNull(responseRequest.getSortBy())){
            responseRequest.setSortBy("_id");
        }
        String criteria = isValidString(responseRequest.getId())?  "_id:{ $ne:ObjectId('"+responseRequest.getId()+"') }, " : "";
        if(isValidString(responseRequest.getSpaceId())){
            criteria = criteria.concat("spaceId:'").concat(responseRequest.getSpaceId()).concat("', ");
        }
//        if(isValidString(responseRequest.getSubSpaceId())){
//            criteria = criteria.concat("subSpaceId:'").concat(responseRequest.getSubSpaceId()).concat("', ");
//        }
        if(Objects.nonNull(responseRequest.getStartDate()) && Objects.nonNull(responseRequest.getEndDate()) ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            criteria = criteria.concat("deadline:").concat(dateRange(sdf.format(responseRequest.getStartDate()), sdf.format(responseRequest.getEndDate()))).concat(", ");
        }
        criteria = criteria.concat(listToStringQuery("priority", responseRequest.getPriorities()));
        criteria = criteria.concat(listToStringQuery("tags", responseRequest.getTags()));
        String limit = "{ $limit: "+responseRequest.getSize()+" },";
        String skip = "{ $skip: "+responseRequest.getSize()* responseRequest.getPage()+" },";
        String sort = "{ $sort: {'"+responseRequest.getSortBy()+"':"+( Objects.equals(responseRequest.getSortOrder(), "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.task +"', \n" +
                "pipeline: [" +
                " { $match:{ "+criteria+" } }," +
                " { $facet:{ " +
                "       data:[" +
                "           { $project:{ _id:0, id:{$toString:'$_id'}, name:1, priority:1, status:1, deadline:1 }}," +
                sort+skip+limit+
                "       ]," +
                "       metaData: [ { $count: 'total' } ]" +
                " } }," +
                "{ $unwind:{ path:'$metaData' } }," +
                "{ $project:{ data:1, totalData:'$metaData.total' } } " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? noDataResponse() : res.get(0);
    }

    @Override
    public Object getByIds(List<String> ids) {
        Query query =new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        query.fields().include("name", "priority", "completion", "deadline");
        return  mongoTemplate.find(query, Tasks.class);
    }

    @Override
    public UpdateResult link(List<String> tasks, String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(tasks));
        Update update = new Update();
        update.addToSet("tasks", id);
        return mongoTemplate.updateMulti(query, update, CollectionName.task);
    }

    @Override
    public List<String> getLinks(String type, String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.fields().include(type);
        Tasks task = mongoTemplate.findOne(query, Tasks.class);
        if(Objects.nonNull(task)){
            if(Objects.equals(type, "tasks")){
                return  Objects.nonNull(task.getTasks()) ? task.getTasks() : Collections.EMPTY_LIST;
            } else if(Objects.equals(type, "backlogs")){
                return  Objects.nonNull(task.getBacklogs()) ? task.getBacklogs() : Collections.EMPTY_LIST;
            } else if(Objects.equals(type, "issues")){
                return  Objects.nonNull(task.getIssues()) ? task.getIssues() : Collections.EMPTY_LIST;
            } else if(Objects.equals(type, "testCases")){
                return  Objects.nonNull(task.getTestCases()) ? task.getTestCases() : Collections.EMPTY_LIST;
            }
        }
        return null;
    }

    @Override
    public void unlink(List<String> list, String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(list));
        Update update = new Update();
        update.pull("tasks", taskId);
        mongoTemplate.updateMulti(query, update, Tasks.class);

    }

    @Override
    public List<Document> report(ReportDto request) {
        final String query = "{ aggregate: '"+ CollectionName.task_schedule +"', \n" +
                "pipeline: [ "+reportQuery(request) +
                "    { $group:{ _id:'$spaceName', data:{ $push:'$$ROOT' } } },\n" +
                "    { $project:{ _id:0,spaceName:'$_id', data:1} }, \n"+" " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object reportPaged(ReportDto request) {
        String limit = "{ $limit: "+request.getSize()+" },";
        String skip = "{ $skip: "+request.getSize()* request.getPage()+" },";
        String sort = "{ $sort: {'"+request.getSortBy()+"':"+( Objects.equals(request.getSortOrder(), "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.task_schedule +"', \n" +
                "pipeline: [" + reportQuery(request)+
                "   { $facet:{ " +
                "       data:[" + skip + limit + sort + "]," +
                "       metaData: [ { $count: 'total' } ]" +
                "   } }," +
                "   { $unwind:{ path:'$metaData' } }," +
                "   { $project:{ data:1, totalData:'$metaData.total' } } " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? noDataResponse() : res.get(0);
    }

    @Override
    public Object getGroupedDrafts(String spaceId, String subSpaceId, String currentUserId) {
        String subspaceFilter = isValidString(subSpaceId) ? quotedString(subSpaceId) : "{ $exists:false }";
        final String query = "{ aggregate: '"+ CollectionName.tasks_draft +"', \n" +
                "pipeline: [" +
                "    { $match:{  spaceId:'"+spaceId+"', subSpaceId:"+subspaceFilter+" } },\n" +
                "    { $project:{ _id:0, id:{$toString:'$_id'}, \n" +
                "       name:1, tags:1, type:1, priority:1, deadline:1, position:1, \n" +
                "       status:{ $ifNull:['$status', 'Draft'] }, isDraft:true\n" +
                "    }},\n" +
                "    { $sort:{ position:1, _id:1 }},\n" +
                "    { $group:{ _id:'$status', data:{ $push:'$$ROOT'} }},\n" +
                "    { $group:{_id:'', data:{ $mergeObjects: \n" +
                "        { $arrayToObject: [  [{ k: '$_id', v: '$data'}]  ]} }}},\n" +
                "    { $replaceRoot: { newRoot:'$data' } }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? Collections.emptyMap() : res.get(0);
    }

    @Override
    @Transactional
    public void deleteByTaskId(String taskId) {
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("taskId").is(taskId)), TaskSchedule.class).wasAcknowledged(), "Failed To delete Task Schedule");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("_id").is(taskId)), Tasks.class).wasAcknowledged(), "Failed To delete Task");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("taskId").is(taskId)), EffortLog.class).wasAcknowledged(), "Failed To delete EffortLog");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("sourceId").is(taskId)), Comment.class).wasAcknowledged(), "Failed To delete Comments");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("sourceId").is(taskId)), FileInfo.class).wasAcknowledged(), "Failed To delete File Info");
    }

    @Override
    public Object getOverallBoard(String startDate, String endDate, String userId, List<String> spaceIds, PagedResponseRequest responseRequest) {
        String spaces = Utils.listToStringQuery("spaceId", responseRequest.getSpaces());
        if(spaces.isEmpty()){
            spaces =Utils.listToStringQuery("spaceId", spaceIds);
        }
        String criteria = "";
        criteria = criteria.concat(listToStringQuery("tags", responseRequest.getTags()));
        criteria = criteria.concat(listToStringQuery("priority", responseRequest.getPriorities()));
        String query =  "{ aggregate: '"+ CollectionName.task_schedule +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ scheduleDate:"+Utils.dateRange(startDate, endDate)+", "+spaces+" } },\n"+
                "    { $project:{ taskId:1, spaceId:1, assignedTo:1 } },\n" +
                "    { $group:{ _id:{ taskId:'$taskId', spaceId:'$spaceId' }, users:{ $addToSet:'$assignedTo' } } },\n" +
                "    { $lookup:{\n" +
                "        from:'tasks',\n" +
                "        let:{ id:{ $toObjectId:'$_id.taskId' } },\n" +
                "        as:'task',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] }, "+criteria+" } },\n" +
                "            { $project:{ _id:0, name:1, type:1, category:1, tags:1, completion:1, priority:1, deadline:1, status:1 } }\n" +
                "        \n" +
                "        ]        \n" +
                "    } },\n" +
                "    { $unwind:'$task' },\n" +
                "    { $project:{\n" +
                "        id:'$_id.taskId',\n" +
                "        project:'$_id.spaceId',\n" +
                "        users:1,\n" +
                "        name:'$task.name',\n" +
                "        type:'$task.type',\n" +
                "        category:'$task.category',\n" +
                "        priority:'$task.priority',\n" +
                "        deadline:'$task.deadline',\n" +
                "        status:'$task.status',\n" +
                "        tags:'$task.tags',\n" +
                "        completion:'$task.completion',\n" +
                "        _id:0\n" +
                "    } },\n" +
                "    { $group:{ _id:'$project', data:{ $push:'$$ROOT' } } },\n" +
                "    { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}},\n" +
                "    { $replaceRoot:{ newRoot:'$data' } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? Collections.emptyMap() : res.get(0);
    }

    @Override
    public Object getAllUsers(String workspaceId) {
        String query = "{ aggregate: '"+ CollectionName.users_permission +"', \n" +
                "pipeline: [\n" +
                "   { $match:{ workspaceId:'"+workspaceId+"' } },\n" +
                "   { $group:{ _id:'$userId' } },\n" +
                "   { $lookup:{ \n" +
                "       from:'user',\n" +
                "       let:{ id:{ $toObjectId:'$_id' } },\n" +
                "       as:'user',\n" +
                "       pipeline:[\n" +
                "          { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "          { $project:{ name:'$fullName', image:1, _id:0 } }\n" +
                "       ]      \n" +
                "   } },\n" +
                "   { $unwind:'$user' },\n" +
                "   \n" +
                "   { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$user'  }]  ]} }}},\n" +
                "   { $replaceRoot:{newRoot:'$data'}}" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? Collections.emptyMap() : res.get(0);
    }

    @Override
    public void changeStageOfAllTasksInWorkspace(String workspaceId, String oldStage, String newStage) {
        mongoTemplate.updateMulti(
                new Query().addCriteria(Criteria.where("status").is(oldStage)),
                new Update().set("status", newStage),
                Tasks.class
        );
    }

    @Override
    public void changeStageOfAllTaskDraftsInWorkspace(String workspaceId, String oldStage, String newStage) {
        mongoTemplate.updateMulti(
                new Query().addCriteria(Criteria.where("status").is(oldStage)),
                new Update().set("status", newStage),
                TaskDraft.class
        );

    }


    private String reportQuery(ReportDto request){
        String match = "";
        String priorities = "";
        StringBuilder rString = new StringBuilder();
        if(Objects.nonNull(request.getPriority()) && !request.getPriority().isEmpty()){
            for (String each : request.getPriority()) {
                rString.append(each).append(", ");
            }
            priorities = "'priority':{ $in:["+rString.toString()+" ] },";
        }
        match = match.concat(listToStringQuery("type", request.getType()));
        match = match.concat(listToStringQuery("subspaceId", request.getSegment()));
        match = match.concat(listToStringQuery("category", request.getCategory()));
        match = match.concat(priorities);
        match = match.concat(listToStringQuery("status", request.getStatus()));
        match = match.concat(listToStringQuery("tags", request.getTags()));
        return  "    { $project:{ taskId:1, scheduleDate:1, spaceId:1, subspaceId:1 } },\n" +
                "    { $match:{ scheduleDate: " + dateRange(request.getStartDateString(), request.getEndDateString())+ ",\n"
                +       listToStringQuery("spaceId", request.getProject())+
                "    }},\n" +
                "    { $group:{ _id:'$taskId' } },\n" +
                "    { $lookup:{ \n" +
                "        from:'tasks',\n" +
                "        let:{id:{ $toObjectId:'$_id' }},\n" +
                "        pipeline:[ \n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id'] }, "+match+" } },\n" +
                "            { $project:{ \n" +
                "                spaceId:1, subSpaceId:1, \n" +
                "                name:1, type:1, category:1,  \n" +
                "                status:1, completion:1,\n" +
                "                priority:{\n" +
                "                  $switch: {\n" +
                "                    branches: [\n" +
                "                      { case: { $eq: [ '$priority', 4 ] }, then: 'Low' },\n" +
                "                      { case: { $eq: [ '$priority', 3 ] }, then: 'Normal' },\n" +
                "                      { case: { $eq: [ '$priority', 2 ] }, then: 'High' },\n" +
                "                      { case: { $eq: [ '$priority', 1 ] }, then: 'Urgent' }\n" +
                "                    ],\n" +
                "                    default: 'No Priority'\n" +
                "                  }\n" +
                "                },\n" +
                "                tags: { \n" +
                "                  $reduce: {\n" +
                "                      input: { $ifNull:['$tags', []] },\n" +
                "                      initialValue: '',\n" +
                "                      in: { $concat: [ '$$value', '$$this' ]}\n" +
                "                  }\n" +
                "                }\n" +
                "            }}\n" +
                "        ],\n" +
                "        as:'task'\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$task' } },\n" +
                "    { $replaceRoot:{ newRoot:'$task' } },\n" +
                "    { $group:{ _id:'$spaceId', data:{ $push:'$$ROOT' } } },\n" +
                "    { $lookup:{ \n" +
                "        from:'spaces',\n" +
                "        let:{ id: {$toObjectId:'$_id'} },\n" +
                "        as:'space',\n" +
                "        pipeline:[\n" +
                "            { $project:{ spaceName:'$name' } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$space' } },\n" +
                "    { $unwind:{ path:'$data' } },\n" +
                "    { $replaceRoot: { newRoot: { $mergeObjects: [ '$space', '$data' ] } }},\n" +
                "    { $project:{ _id:0 } },\n";
    }

    @Override
    public void reassign(String id, List<ModificationDto> list, Boolean fromToday) {
        Criteria criteria = Criteria.where("taskId").is(id);
        if(fromToday){
            Date date = new Date();
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            criteria = criteria.and("scheduleDate").gte(date);
        }
        for(ModificationDto modification : list){
            Query query = new Query();
            Update update = new Update();
            update.set("assignedTo",modification.getChanged());
            query.addCriteria(criteria.and("assignedTo").is(modification.getCurrent()));
            mongoTemplate.updateMulti(query, update, TaskSchedule.class);
        }
    }

    @Override
    public Object getAllTasksPendingApproval(String currentUserId) {
        final String query = "{ aggregate: '"+ CollectionName.approval_requests +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ source:'task', approvalType:'reopen_task', approvalStatus:'PENDING', supervisor:'"+currentUserId+"' } },\n" +
                "    { $lookup:{\n" +
                "        from:'tasks',\n" +
                "        as:'task',\n" +
                "        let:{ id:'$sourceId' },\n" +
                "        pipeline:[ \n" +
                "            { $project:{ name:1, spaceId:1, subSpaceId:1, id:{ $toString:'$_id' }, _id:0 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } },\n" +
                "            { $group:{ _id:'$spaceId', data:{ $push:'$$ROOT' } } },\n" +
                "            { $lookup:{ \n" +
                "                from:'spaces',\n" +
                "                let:{ id:'$_id' },\n" +
                "                as:'space',\n" +
                "                pipeline:[\n" +
                "                    { $project:{ _id:0, name:1, id:{ $toString:'$_id' } } },\n" +
                "                    { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }                \n" +
                "                ]\n" +
                "            } },\n" +
                "            { $unwind:{ path:'$space' } },\n" +
                "            { $unwind:{ path:'$data' } },\n" +
                "            { $project:{ _id:0, space:'$space.name', name:'$data.name', subspaceId:'$data.subSpaceId', spaceId:'$data.spaceId', taskId:'$data.id' } },\n" +
                "            { $group:{ _id:'$subspaceId', data:{ $push:'$$ROOT' } } },\n" +
                "            { $lookup:{ \n" +
                "                from:'folder',\n" +
                "                let:{ id:'$_id' },\n" +
                "                as:'folder',\n" +
                "                pipeline:[\n" +
                "                    { $project:{ _id:0, category:{ $ifNull:[ '$category', 'Uncategorized' ] }, subspaces:1 } },\n" +
                "                    { $unwind:{ path:'$subspaces' } },\n" +
                "                    { $match:{ $expr:{ $eq:[ '$subspaces', '$$id' ] } } }\n" +
                "                ]\n" +
                "            } },\n" +
                "            { $unwind:{ path:'$folder', preserveNullAndEmptyArrays:true } },\n" +
                "            { $unwind:{ path:'$data' } },\n" +
                "            { $lookup:{ \n" +
                "                from:'task_schedule',\n" +
                "                let:{ id:'$data.taskId' },\n" +
                "                as:'assigned',\n" +
                "                pipeline:[\n" +
                "                    { $match:{ $expr:{ $eq:[ '$taskId', '$$id' ] } } },\n" +
                "                    { $group:{ _id:'$assignedTo' } },\n" +
                "                    { $group:{ _id:'', assigned:{ $push:'$_id' } } }\n" +
                "                ]\n" +
                "            } },\n" +
                "            { $unwind:{ path:'$assigned' } },\n" +
                "            { $project:{ _id:0, assigned:'$assigned.assigned', \n" +
                "                space:'$data.space', name:'$data.name', subspaceId:'$data.subspaceId', \n" +
                "                spaceId:'$data.spaceId', taskId:'$data.taskId', category:{ $ifNull:[ '$folder.category', 'Uncategorized' ] } \n" +
                "            } },\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$task' },\n" +
                "    { $addFields: { mergedTask: { $mergeObjects: [\"$task\", \"$$ROOT\"] } }},\n" +
                "    { $replaceRoot:{ newRoot:'$mergedTask' } },\n" +
                "    { $project: { task: 0 }}\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object getMonitorList(PagedResponseRequest request) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String match = "startDate:".concat(dateRange(sdf.format(request.getStartDate()), sdf.format(request.getEndDate()))).concat(", ");
        match = match.concat(listToStringQuery("assignedUsers", request.getUsers()));
        match = match.concat(listToStringQuery("spaceId", request.getSpaces()));
        match = match.concat(listToStringQuery("status", request.getStatus()));

        String filterCategory = listToStringQuery("category", request.getCategories());
        if(isValidString(filterCategory)){
            filterCategory = "{ $match:{ ".concat(filterCategory).concat("}},");
        }
        String limit = "{ $limit: "+request.getSize()+" },";
        String skip = "{ $skip: "+request.getSize()* request.getPage()+" },";
        String sort = "{ $sort: {'"+request.getSortBy()+"':"+( Objects.equals(request.getSortOrder(), "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.task +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ "+match+" } },\n" +
                "    { $project:{ _id:0, id:{ $toString:'$_id' }, name:1, spaceId:1, subSpaceId:1, status:1, assignedUsers:1 } },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ id:'$spaceId' },\n" +
                "        as:'space',\n" +
                "        pipeline:[\n" +
                "            { $project:{ name:1, _id:0, id:{ $toString:'$_id' } } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $lookup:{\n" +
                "        from:'folder',\n" +
                "        let:{ id:'$subSpaceId' },\n" +
                "        as:'folder',\n" +
                "        pipeline:[\n" +
                "            { $project:{ name:1, _id:0, subspaces:1, category:{ $ifNull:['$category','Uncategorized'] } } },\n" +
                "            { $match:{ $expr:{ $in:[ '$$id', '$subspaces' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$space' },\n" +
                "    { $unwind:{ path:'$space', preserveNullAndEmptyArrays:true } },\n" +
                "    { $project:{\n" +
                "        id:1,\n" +
                "        name:1,\n" +
                "        status:1,\n" +
                "        assignedUsers:1,\n" +
                "        space:'$space.name',\n" +
                "        spaceId:'$space.id',\n" +
                "        category:{ $ifNull:['$folder.category', 'Uncategorized'] }\n" +
                "    } },\n" +
                "    { $unwind:'$category' },\n" +
                "    "+filterCategory+"\n" +
                "    { $facet:{\n" +
                "        analytics:[\n" +
                "            { $project:{ category:1, status:1  } },\n" +
                "            { $group:{\n" +
                "                _id:'$category',\n" +
                "                hold:{ $sum:{ $cond: [{ $eq: ['$status', 'Hold'] }, 1, 0] } } ,\n" +
                "                inprogress:{ $sum:{ $cond: [{ $eq: ['$status', 'In Progress'] }, 1, 0] } } ,\n" +
                "            } },\n" +
                "            { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v:{ hold: \"$hold\", inprogress: \"$inprogress\" }   }]  ]} }}},\n" +
                "            \n" +
                "            { $replaceRoot:{ newRoot:'$data'  } },\n" +
                "        ],            \n" +
                "        data:[ " + sort+skip+limit+
                "        ],\n" +
                "        metaData:[\n" +
                "           { $group:{ _id:'', total:{ $sum:1 } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$metaData' },\n" +
                "    { $project:{ \n" +
                "        analytics:{ $arrayElemAt:[ '$analytics', 0 ] },\n" +
                "        totalData:'$metaData.total',\n" +
                "        data:1\n" +
                "    } }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? noDataResponse() : res.get(0);
    }

    @Override
    public Integer findLastPosition(String subSpaceId) {
        long task = mongoTemplate.count(new Query().addCriteria(Criteria.where("subSpaceId").is(subSpaceId)), Tasks.class);
        long draft = mongoTemplate.count(new Query().addCriteria(Criteria.where("subSpaceId").is(subSpaceId)), TaskDraft.class);
        return Math.toIntExact(task+draft);
    }

    @Override
    public Object tasksListForReposition(PagedResponseRequest request) {
        final String query = "{ aggregate: '"+ CollectionName.sub_spaces +"', \n" +
                "pipeline: [" +
                "    { $match:{ _id:ObjectId('"+request.getSubSpaceId()+"') }  },\n" +
                "    { $project:{ _id:1 } },\n" +
                "    { $lookup:{ \n" +
                "        from:'tasks',\n" +
                "        as:'tasks',\n" +
                "        pipeline:[\n" +
                "            { $match:{ subSpaceId:'"+request.getSubSpaceId()+"' } },\n" +
                "            { $project:{ name:1,  position:{ $ifNull:['$position', 0] }, type:'Published', id:{ $toString:'$_id' }, date:{ $toDate:'$_id' }, _id:0 } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $lookup:{ \n" +
                "        from:'tasks_draft',\n" +
                "        as:'drafts',\n" +
                "        pipeline:[\n" +
                "            { $match:{ subSpaceId:'"+request.getSubSpaceId()+"' } },\n" +
                "            { $project:{ name:1,  position:{ $ifNull:['$position', 0] }, type:'Draft', id:{ $toString:'$_id' }, date:{ $toDate:'$_id' }, _id:0} },\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $project:{ _id:0, tasks:{ $concatArrays: ['$tasks', '$drafts'] } } },\n" +
                "    { $unwind:'$tasks' },\n" +
                "    { $replaceRoot:{ newRoot:'$tasks' } }, \n" +
                "    { $sort:{ position:1, date:1 } }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public void updatePositions(List<TaskPositionDto> positions) {
        for(TaskPositionDto position : positions){
            mongoTemplate.updateFirst(
                    new Query().addCriteria(Criteria.where("id").is(position.getId())),
                    new Update().set("position", position.getPosition()),
                    Objects.equals(position.getType(), "Published") ? Tasks.class :TaskDraft.class
            );
        }
    }

    @Override
    public void updateSchedules(String taskId, String userId, TasksHistory history) {
        mongoTemplate.updateFirst(
                new Query().addCriteria(Criteria.where("id").is(taskId)),
                new Update().set("assignedUsers", Arrays.asList(userId)).push("history", history),
                Tasks.class
        );
        mongoTemplate.updateMulti(
                new Query().addCriteria(Criteria.where("taskId").is(taskId)),
                new Update().set("assignedTo", userId),
                TaskSchedule.class
        );
    }

}
