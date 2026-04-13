package com.ds.tracks.dashboard.dao;


import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.dashboard.model.DashboardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DashboardDaoImpl implements DashboardDao {
    private final MongoTemplate mongoTemplate;

    @Override
    public Document generate(String spaceId, String startDate, String endDate, String defaultCapacity, String workdays) {
        final String dateRange = "" +
                " $gte:ISODate('"+startDate+"T00:00:00.000+06:00')," +
                " $lte:ISODate('"+endDate+"T23:59:59.999+06:00')";
        final String query = "{ aggregate: '" + CollectionName.users_permission + "', \n" +
                "pipeline:[   \n" +
                "{ $match:{ spaceId:'"+spaceId+"', permissionFor:\"SPACE\", role:{$ne:'OBSERVER'}  } },\n" +
                "{ $project:{ userId:1, _id:0, capacity:{ $ifNull:[ '$capacity', "+defaultCapacity+" ] }, spaceId:1 } },\n" +
                "{ $lookup:{\n" +
                "        from:'task_schedule',\n" +
                "        let:{ id:'$userId', spaceId:'$spaceId' },\n" +
                "        pipeline:[\n" +
                "            { $match:{ \n" +
                "                $expr:{ $and:[{ $eq:['$assignedTo','$$id'] }, { $eq:['$spaceId','$$spaceId'] } ]},\n" +
                "                scheduleDate:{ "+dateRange+"}\n" +
                "            }},\n" +
                "            { $project:{ _id:0, taskId:1, duration:1 } }" +
                "        \n" +
                "        ],\n" +
                "        as:'estimated'\n" +
                "} },\n" +
                "{ $unwind:{path:'$estimated', preserveNullAndEmptyArrays:true} },\n" +
                "{ $lookup:{\n" +
                "        from:'effort_log',\n" +
                "        let:{ id:'$userId', taskId:'$estimated.taskId'  },\n" +
                "        pipeline:[\n" +
                "            { $match:{\n" +
                "                $expr:{ $and:[{$eq:['$createdBy','$$id'] }, {$eq:['$taskId','$$taskId'] }]},\n" +
                "                logDate:{ "+dateRange+" }\n" +
                "            } },\n" +
                "            \n" +
                "        ],\n" +
                "        as:'effort'\n" +
                "} },\n" +
                "{ $unwind:{path:'$effort', preserveNullAndEmptyArrays:true} },\n" +
                "{ $group:{ _id:{userId: '$userId', capacity:'$capacity' }, effort:{ $sum:'$effort.duration' }, estimated:{ $sum:'$estimated.duration' } } }," +
                "\n" +
                "{ $lookup:{\n" +
                "    from:'user',\n" +
                "    let:{ 'id':{ '$toObjectId':'$_id.userId' } },\n" +
                "    pipeline:[\n" +
                "        { $match:{ \n" +
                "            $expr:{ $eq:['$_id','$$id'] },\n" +
                "        }},\n" +
                "        { $project:{ name:'$fullName', email:'$loginId', _id:0 } }\n" +
                "    ],\n" +
                "    as:'user'\n" +
                "} },\n" +
                "{ $unwind:{path:'$user', preserveNullAndEmptyArrays:true} },\n" +
                "{ $project:{ \n" +
                "    name:'$user.name',\n" +
                "    email:'$user.email',\n" +
                "    capacity:{ $multiply:['$_id.capacity', "+workdays+"] }, \n" +
                "    estimated:{ $ifNull:['$estimated', 0] }, \n" +
                "    actual:{ $ifNull:['$effort', 0] }, \n" +
                "}}," +
                "{ $sort:{ name:1 } }," +
                "{ $facet:{ " +
                "   workload:[" +
                "       { $group:{ _id:null, " +
                "           names:{$push:'$name'}, " +
                "           actual:{$push:'$actual'}, " +
                "           estimated:{$push:'$estimated'}, " +
                "           capacity:{$push:'$capacity'} " +
                "       }}," +
                "       { $project:{_id:0} }" +
                "   ]," +
                "   overall:[" +
                "       { $group:{ _id:null, " +
                "           actual:{$sum:'$actual'}, " +
                "           estimated:{$sum:'$estimated'}, " +
                "       }}," +
                "       { $project:{_id:0} }" +
                "   ]," +
                "   table:[" +
                "       { $project:{ " +
                "           actual:1, capacity:1, estimated:1, name:1, " +
                "           efficiency:{ $cond: [ { $eq: [ '$actual', 0 ] }, 0, {$divide:['$estimated', '$actual']} ] }, " +
                "           workload:{ $cond: [ { $eq: [ '$capacity', 0 ] }, 0, {$divide:['$estimated','$capacity']} ] }, " +
                "       }}" +
                "   ]," +
                " }}" +
                "{ $unwind:{path:'$workload', preserveNullAndEmptyArrays:true} },\n" +
                "{ $unwind:{path:'$overall', preserveNullAndEmptyArrays:true} },\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Document> res = (List<Document>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return Objects.nonNull(res) && !res.isEmpty() ? res.get(0) : new Document();
    }
    @Override
    public Document generateOverall(List<String> spaceIds, String startDate, String endDate, String userId, String workdays) {
        final String dateRange = "" +
                "{ $gte:ISODate('"+startDate+"T00:00:00.000+06:00')," +
                " $lte:ISODate('"+endDate+"T23:59:59.999+06:00')}";
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "', \n" +
                " pipeline: [\n" +
                "    { $match:{ scheduleDate:"+dateRange+", "+Utils.listToStringQuery("spaceId",spaceIds)+"  }},\n" +
                "    { $group: { _id: {taskId:'$taskId', spaceId:'$spaceId'}, sum: { $sum: '$duration' } } },\n" +
                "    { $lookup: {\n" +
                "        from: 'effort_log',\n" +
                "        let: { id: '$_id.taskId' },\n" +
                "        pipeline: [\n" +
                "            { $match: {\n" +
                "                $expr: { $eq: ['$taskId', '$$id'] },\n" +
                "                logDate: " + dateRange +
                "            }},\n" +
                "            { $group: { _id: '', sum: { $sum: '$duration' } } }\n" +
                "        ],\n" +
                "        as: 'efforts'\n" +
                "        }\n" +
                "    },\n" +
                "    { $unwind: { path: '$efforts', preserveNullAndEmptyArrays: true } },\n" +
                "    { $project: {\n" +
                "        spaceId: '$_id.spaceId',\n" +
                "        scheduledDuration: { $ifNull: ['$sum', 0] },\n" +
                "        actualDuration: { $ifNull: ['$efforts.sum', 0] }\n" +
                "    }},\n" +
                "    { $group: {\n" +
                "        _id: '$spaceId',\n" +
                "        scheduledDuration: { $sum: '$scheduledDuration' },\n" +
                "        actualDuration: { $sum: '$actualDuration' }\n" +
                "    }},\n" +
                "    { $lookup: {\n" +
                "        from: 'spaces',\n" +
                "        let: { id: { $toObjectId: '$_id' } },\n" +
                "        as: 'space',\n" +
                "        pipeline: [\n" +
                "            { $project: { name: 1, workHour: '$configurations.workHour' } },\n" +
                "            { $match: { $expr: { $eq: ['$_id', '$$id'] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind: { path: '$space' } },\n" +
                "    { $lookup:{\n" +
                "        from:'users_permission',\n" +
                "        let:{ id:'$_id', name: '$space.name', workhour:'$space.workHour' },\n" +
                "        as:'configs',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$spaceId', '$$id' ] }, permissionFor:'SPACE', role:{ $ne:'OBSERVER' } } },\n" +
                "            { $project:{ capacity:{ $ifNull:['$capacity', '$$workhour'] }, name:'$$name' } },\n" +
                "            { $group:{ _id:'$name', capacity:{ $sum:'$capacity' } } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind: { path: '$configs' } },\n" +
                "    { $project: {\n" +
                "        _id: 0,\n" +
                "        name: '$configs._id',\n" +
                "        capacity: { $multiply: [{ $ifNull: ['$configs.capacity', 0] }, "+workdays+" ] },\n" +
                "        estimated: '$scheduledDuration',\n" +
                "        actual: '$actualDuration'\n" +
                "    }},\n" +
                "    { $sort:{ name:1} },\n" +
                "    { $facet:{    \n" +
                "        workload:[       \n" +
                "            { $group:{ \n" +
                "                _id:null, \n" +
                "                names:{$push:'$name'},  \n" +
                "                actual:{$push:'$actual'},  \n" +
                "                estimated:{$push:'$estimated'}, \n" +
                "                capacity:{$push:'$capacity'} \n" +
                "            }}, \n" +
                "            { $project:{_id:0} } \n" +
                "        ],   \n" +
                "        overall:[ \n" +
                "            { $group:{ \n" +
                "                _id:null,  \n" +
                "                actual:{$sum:'$actual'}, \n" +
                "                estimated:{$sum:'$estimated'}, \n" +
                "            }}, \n" +
                "            { $project:{_id:0} } \n" +
                "        ], \n" +
                "        table:[ \n" +
                "            { $project:{ \n" +
                "                actual:1, \n" +
                "                capacity:1, \n" +
                "                estimated:1, \n" +
                "                name:1, \n" +
                "                efficiency:{ $cond: [ { $eq: [ '$actual', 0 ] }, 0, {$divide:['$estimated', '$actual']} ] }, \n" +
                "                workload:{ $cond: [ { $eq: [ '$capacity', 0 ] }, 0, {$divide:['$estimated','$capacity']} ] }, \n" +
                "            }} \n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$workload', preserveNullAndEmptyArrays:true} },\n" +
                "    { $unwind:{path:'$overall', preserveNullAndEmptyArrays:true} },\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Document> res = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return Objects.nonNull(res) && !res.isEmpty() ? res.get(0) : new Document();
    }

    @Override
    public List<Document> workload(List<String> spaceIds, String startDate, String endDate, String currentUserId, String valueOf) {
        final String query = "{ aggregate: '" + CollectionName.spaces + "', \n" +
                "pipeline:[\n" +
                "    { $project:{ _id:0, id:{ $toString:'$_id' }, name:1, workHour:'$configurations.workHour' } },\n" +
                "    { $match:{ "+Utils.listToStringQuery("id",spaceIds)+" } },\n" +
                "    { $lookup:{ \n" +
                "        from:'users_permission',\n" +
                "        let:{ id:'$_id' },\n" +
                "        as:'user',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$spaceId' ] }, userId:'"+currentUserId+"', permissionFor:'SPACE' } },\n" +
                "            { $project:{ capacity:1, _id:0 } }\n" +
                "        ]          \n" +
                "    } },\n" +
                "    { $unwind:{ path:'$user', preserveNullAndEmptyArrays:true } },\n" +
                "    { $lookup:{\n" +
                "        from:'task_schedule',\n" +
                "        let:{ id:'$id' },\n" +
                "        as:'task',\n" +
                "        pipeline:[\n" +
                "            { $match:{ \n" +
                "                $expr:{ $eq:[ '$$id', '$spaceId' ] }, \n" +
                "                assignedTo:'"+currentUserId+"', \n" +
                "                scheduleDate:"+Utils.dateRange(startDate, endDate)+"" +
                "            }},\n" +
                "            { $project:{ capacity:1, _id:0, duration:1 } },\n" +
                "            { $group:{ _id:'$$id', workload:{ $sum:'$duration' } } }\n" +
                "        ]              \n" +
                "    } },\n" +
                "    { $unwind:{ path:'$task', preserveNullAndEmptyArrays:true } },\n" +
                "    { $project:{\n" +
                "        space:'$name',\n" +
                "        capacity:{ $ifNull:[ { $ifNull:[ '$user.capacity', '$workHour' ] }, 0 ] },\n" +
                "        workload:{ $ifNull:[ '$task.workload', 0 ] }\n" +
                "    }}\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }


    @Override
    public List<Document> tasks(List<String> spaceIds, String startDate, String endDate, String currentUserId) {
        final String query = "{ aggregate: '" + CollectionName.task_schedule + "', \n" +
                "pipeline:[\n" +
                "    { $match:{ "+Utils.listToStringQuery("spaceId",spaceIds)+" assignedTo:'"+currentUserId+"', scheduleDate:"+Utils.dateRange(startDate, endDate)+" } },\n" +
                "    { $group:{ _id:'$taskId', workload:{ $sum:'$duration' } } },\n" +
                "    { $lookup:{\n" +
                "        from:'tasks',\n" +
                "        let:{ id:{ $toObjectId:'$_id'}, duration:'$workload'  },\n" +
                "        as:'tasks',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$_id' ] } } },\n" +
                "            { $project:{ name:1, priority:1, status:1, spaceId:1, subspaceId:1, completion:1, duration:'$$duration', _id:0, id:{ $toString:'$_id' } } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$tasks' },\n" +
                "    { $replaceRoot:{ newRoot:'$tasks' } },\n" +
                "    { $lookup:{ \n" +
                "        from:'spaces',\n" +
                "        let:{ id:{ $toObjectId:'$spaceId'}},\n" +
                "        as:'location',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "            { $project:{ name:1, _id:0, color:1 } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$location' }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
    public Document effort(List<String> spaceIds, String startDate, String endDate, String currentUserId) {
        final String query = "{ aggregate: '" + CollectionName.effort_log + "', \n" +
                "pipeline:[\n" +
                "    { $match:{ "+Utils.listToStringQuery("spaceId",spaceIds)+" createdBy:'"+currentUserId+"', logDate:"+Utils.dateRange(startDate, endDate)+" } },\n" +
                "    { $group:{_id:'', duration:{ $sum:'$duration' }} }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Document> documents = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return Objects.nonNull(documents) && !documents.isEmpty() ? documents.get(0): new Document("duration", 0);
    }

    @Override
    public Object tasksSummary(List<String> spaceIds, String currentUserId) {
        final String query = "{ aggregate: '" + CollectionName.task + "', \n" +
                "pipeline:[\n" +
                "    { $match:{ "+Utils.listToStringQuery("spaceId",spaceIds)+" } },\n" +
                "    { $project:{ status:1, _id:0 }},\n" +
                "    { $group:{ _id:'$status', count:{ $sum:1 } } },\n" +
                "    { $project:{ _id:0, name:'$_id', y:'$count' } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
}
