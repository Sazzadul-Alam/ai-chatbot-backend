package com.ds.tracks.effort.dao;


import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EffortLogDaoImpl implements EffortLogDao {

    private final MongoTemplate mongoTemplate;


    @Override
    public List<Map<String, Object>> findEffortLogsById(String id, boolean isSubTask) {
        String matchCriteria = isSubTask ? " subTaskId:'"+id+"' " : " taskId:'"+id+"' ";
        List<Map<String, Object>> list= null;
        try{
            final String query = "{ aggregate: '"+ CollectionName.effort_log +"', \n" +
                    "pipeline: [\n" +
                    "{ $project:{_id:0, spaceId:0, subspaceId:0, createdAt:0}},\n" +
                    "{ $match:{"+matchCriteria+"}},\n" +
                    "{ $lookup:{ \n" +
                    "    from:'user',\n" +
                    "    let:{ id:{$toObjectId:'$createdBy'} },\n" +
                    "    pipeline:[\n" +
                    "        { $project:{fullName:1} },\n" +
                    "        { $match:{ $expr:{ $eq:['$_id', '$$id'] } } }\n" +
                    "    ],\n" +
                    "    as:'user'\n" +
                    "     }},\n" +
                    "     { $unwind:{ path:'$user' }},\n" +
                    "     {$project:{ description:1,duration:1, completion:1, loggedBy:'$user.fullName', logDate:1 }}\n" +
                    "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
            list = (List<Map<String, Object>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        } catch (Exception ignored) { }
        return list;
    }
}
