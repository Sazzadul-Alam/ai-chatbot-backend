package com.ds.tracks.workload;

import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.tasks.model.dto.TasksDto;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

import static com.ds.tracks.commons.utils.Utils.*;

@Repository
@RequiredArgsConstructor
public class WorkloadDaoImpl implements WorkloadDao {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Map<String, Object>> findByDateAndAssignedToForSpaceOrSubSpace(String taskDate, String assignedTo,String workspaceId, String spaceId, String subSpaceId) {
//        String spaceSubSpaceFilter = "spaceId:'" + spaceId+ "' ";
//        if(isValidString(subSpaceId)){
//            spaceSubSpaceFilter = spaceSubSpaceFilter.concat("subspaceId:'" + subSpaceId + "'");
//        } else {
//            spaceSubSpaceFilter = spaceSubSpaceFilter.concat("spaceId:'" + spaceId+ "', subSpaceId:{$eq:null}");
//        }
        final String query = "{\n" +
                "    aggregate: '"+CollectionName.task_schedule+"',\n" +
                "    pipeline: [" +
                "    { $match: {" +
//                "        "+spaceSubSpaceFilter+"," +
                "        assignedTo: '"+assignedTo+"'," +
                "        scheduleDate: {" +
                "            $gte:ISODate('"+taskDate+"T00:00:00.000+06:00')," +
                "            $lte:ISODate('"+taskDate+"T23:59:59.999+06:00')" +
                "       }," +
                "    }}," +
                "    {$project:{\"taskId\":1, duration:1, _id:0, id:{$toString:'$_id'}}},\n" +
                "    {$lookup:{\n" +
                "        from:'tasks',\n" +
                "        let:{'taskId':'$taskId', 'duration':'$duration', 'id':'$id'},\n" +
                "        pipeline:[\n" +
                "            { $project:{\n" +
                "                'taskId':{$toString:'$_id'},\n" +
                "                _id:0,\n" +
                "                'id':'$$id',\n" +
                "                'name':1,\n" +
                "                'spaceId':1,\n" +
                "                'priority':1,\n" +
                "                'generatedId':1,\n" +
                "                'severity':1,\n" +
                "                'startDate':1,\n" +
                "                'deadline':1,\n" +
                "                'duration':'$$duration'\n" +
                "            }},\n" +
                "            { $match:{\n" +
                "                $expr:{$eq:['$taskId', '$$taskId']},\n" +
                "            }}\n" +
                "        ],\n" +
                "        as:'tasks'\n" +
                "    }},\n" +
                "    {$unwind:{path:'$tasks'}},  \n" +
                "    { $replaceRoot: { newRoot: '$tasks' }  },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ id:'$spaceId' },\n" +
                "        as:'space',\n" +
                "        pipeline:[\n" +
                "            { $project:{ id:{ $toString:'$_id'}, name:1, _id:0  } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    {$unwind:{path:'$space', preserveNullAndEmptyArrays:true}},\n" +
                "    { $project:{ \n" +
                "        'id':1,\n" +
                "        'name':1,\n" +
                "        'taskId':1,\n" +
                "        'spaceId':1,\n" +
                "        'priority':1,\n" +
                "        'generatedId':1,\n" +
                "        'severity':1,\n" +
                "        'startDate':1,\n" +
                "        'deadline':1,\n" +
                "        'duration':1 ,\n" +
                "        'space':'$space.name'\n" +
                "       \n" +
                "    } }" +
//                "    { $group:{_id:'$taskId', 'data':{$push:'$$ROOT'}}},\n" +
                "], allowDiskUse: true, cursor: { batchSize: 20000000000 }}";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Map<String, List<Map<String, Object>>> findWorkloadForSpaceOrSubspaceByDateBetween(String spaceId, String subspaceId, String startDate, String endDate){
        String spaceSubSpaceFilter = "spaceId:'" + spaceId+ "'";
//        if(isValidString(subspaceId)){
//            spaceSubSpaceFilter = spaceSubSpaceFilter.concat("subspaceId:'" + subspaceId + "'");
//        } else {
//            spaceSubSpaceFilter = spaceSubSpaceFilter.concat("spaceId:'" + spaceId+ "'"); //, subspaceId:{$eq:null}
//        }

        final String query =  "{ aggregate: '"+CollectionName.task_schedule+"'," +
                " pipeline:[\n" +
                "    {$match:{\n" +
                "        "+spaceSubSpaceFilter+"," +
                "        scheduleDate:{'$gte':ISODate('"+startDate+"T00:00:00.000+06:00'), '$lte':ISODate('"+endDate+"T23:59:59.999+06:00')}\n" +
                "    }},\n" +
                "    {$project:{\n" +
                "        assignedTo:1, \n" +
                "        workload:'$duration', \n" +
                "        task:'$taskId', \n" +
                "        id:{$toString:'$_id'}, \n" +
                "        _id:0,\n" +
                "        day:{$dayOfWeek:'$scheduleDate'}\n" +
                "    }},\n" +
                "    {$group:{_id:'$assignedTo', data:{$push:'$$ROOT'}}},\n" +
                "    {$group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: \"$_id\" , v: \"$data\"  }]  ]} }}},\n" +
                "        {\n" +
                "      $replaceRoot: { newRoot: \"$data\" }\n" +
                "    }\n" +
                "], allowDiskUse: true, cursor: { batchSize: 20000000000 }}";
        List<Map<String, List<Map<String, Object>>>> list = (List<Map<String, List<Map<String, Object>>>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return (Objects.isNull(list) || list.isEmpty()) ? new HashMap<>() : list.get(0) ;
    }

    @Override
    public Map<String, List<Document>> findWorkloadForWorkspace(List<String> spaces, String startDate, String endDate) {
        final String query =  "{ aggregate: '"+CollectionName.task_schedule+"'," +
                " pipeline:[\n" +
                "    {$match:{ "+listToStringQuery("spaceId", spaces) +" scheduleDate:"+dateRange(startDate, endDate)+ " }},\n" +
                "    {$project:{\n" +
                "        assignedTo:1, \n" +
                "        workload:'$duration', \n" +
                "        task:'$taskId', \n" +
                "        id:{$toString:'$_id'}, \n" +
                "        _id:0,\n" +
                "        day:{$dayOfWeek:'$scheduleDate'}\n" +
                "    }},\n" +
                "    {$group:{_id:'$assignedTo', data:{$push:'$$ROOT'}}},\n" +
                "    {$group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: \"$_id\" , v: \"$data\"  }]  ]} }}},\n" +
                "        {\n" +
                "      $replaceRoot: { newRoot: \"$data\" }\n" +
                "    }\n" +
                "], allowDiskUse: true, cursor: { batchSize: 20000000000 }}";
        List<?> list = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return (Objects.isNull(list) || list.isEmpty()) ? new HashMap<>() : (Map<String, List<Document>>) list.get(0);
    }
}
