package com.ds.tracks.app;

import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.jfree.chart.util.TextUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

import static com.ds.tracks.commons.utils.Utils.isValidString;
import static com.ds.tracks.commons.utils.Utils.noDataResponse;

@Repository
@AllArgsConstructor
public class AppDaoImpl implements AppDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public Object analytics(String workspace, String project, String startDate, String endDate, String currentUserId, String workdays) {
        final String dateRange =  "{ $gte:ISODate('"+startDate+"T00:00:00.000+06:00'), $lte:ISODate('"+endDate+"T23:59:59.999+06:00')}";
        String filterProject = isValidString(project) ? "spaceId:'"+project+"'," : "";
        final String query = "{ aggregate: '" + CollectionName.users_permission + "', \n" +
                "pipeline:[\n" +
                "    { $match:{ userId:'"+currentUserId+"', workspaceId:'"+workspace+"',"+filterProject+" permissionFor:'SPACE' }},\n" +
                "    { $project:{ spaceId:1, _id:0} },\n" +
                "    {\n" +
                "        $facet: {\n" +
                "            projects: [\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'task_schedule',\n" +
                "                        let: { id: '$spaceId' },\n" +
                "                        pipeline: [\n" +
                "                            {\n" +
                "                                $match: {\n" +
                "                                    $expr: { $eq: ['$spaceId', '$$id'] },\n" +
                "                                    scheduleDate: "+dateRange+
                "                                }\n" +
                "                            },\n" +
                "                            { $group: { _id: '$taskId', sum: { $sum: '$duration' } } }\n" +
                "                        ],\n" +
                "                        as: 'schedules'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$schedules', preserveNullAndEmptyArrays: true } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'effort_log',\n" +
                "                        let: { id: '$schedules._id' },\n" +
                "                        pipeline: [\n" +
                "                            {\n" +
                "                                $match: {\n" +
                "                                    $expr: { $eq: ['$taskId', '$$id'] },\n" +
                "                                    logDate: "+dateRange+
                "                                }\n" +
                "                            },\n" +
                "                            { $group: { _id: '', sum: { $sum: '$duration' } } }\n" +
                "                        ],\n" +
                "                        as: 'efforts'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$efforts', preserveNullAndEmptyArrays: true } },\n" +
                "                {\n" +
                "                    $project: {\n" +
                "                        spaceId: 1,\n" +
                "                        scheduledDuration: { $ifNull: ['$schedules.sum', 0] },\n" +
                "                        actualDuration: { $ifNull: ['$efforts.sum', 0] }\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    $group: {\n" +
                "                        _id: '$spaceId',\n" +
                "                        scheduledDuration: { $sum: '$scheduledDuration' },\n" +
                "                        actualDuration: { $sum: '$actualDuration' }\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'spaces',\n" +
                "                        let: { id: { $toObjectId: '$_id' } },\n" +
                "                        pipeline: [\n" +
                "                            { $project: { name: 1, workHour: '$configurations.workHour' } },\n" +
                "                            { $match: { $expr: { $eq: ['$_id', '$$id'] } } }\n" +
                "                        ],\n" +
                "                        as: 'space'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$space' } },\n" +
                "                { $lookup:{\n" +
                "                    from:'users_permission',\n" +
                "                    let:{ id:'$_id', name: '$space.name', workhour:'$space.workHour' },\n" +
                "                    as:'configs',\n" +
                "                    pipeline:[\n" +
                "                        { $match:{ $expr:{ $eq:[ '$spaceId', '$$id' ] }, permissionFor:'SPACE', role:{ $ne:'OBSERVER' } } },\n" +
                "                        { $project:{ capacity:{ $ifNull:['$capacity', '$$workhour'] }, name:'$$name' } },\n" +
                "                        { $group:{ _id:'$name', capacity:{ $sum:'$capacity' } } }\n" +
                "                    ]\n" +
                "                } },\n" +
                "                { $unwind: { path: '$configs' } },\n" +
                "                {\n" +
                "                    $project: {\n" +
                "                        _id: 0,\n" +
                "                        name: '$configs._id',\n" +
                "                        capacity: { $multiply: [{ $ifNull: ['$configs.capacity', 0] }, "+workdays+"] },\n" +
                "                        estimated: '$scheduledDuration',\n" +
                "                        actual: '$actualDuration'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $sort:{ name:1} }" +
                "            ],\n" +
                "            efforts: [\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'users_permission',\n" +
                "                        let: { spaceId: '$spaceId' },\n" +
                "                        pipeline: [\n" +
                "                            { $match: { $expr: { $eq: ['$$spaceId', '$spaceId'] }, permissionFor: 'SPACE', role: { $ne: 'OBSERVER' } } },\n" +
                "                            { $project: { userId: 1, _id: 0, spaceId:1 } }\n" +
                "                        ],\n" +
                "                        as: 'users'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$users' } },\n" +
                "                { $replaceRoot: { newRoot: '$users' } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'effort_log',\n" +
                "                        let: { id: '$userId', spaceId: '$spaceId' },\n" +
                "                        pipeline: [\n" +
                "                            { $match: { $and:[{$expr: { $eq: ['$$id', '$createdBy'] }}, {$expr: { $eq: ['$$spaceId', '$spaceId'] }}], logDate: "+dateRange+" } },\n" +
                "                            { $project: { duration: 1, _id: 0, user: '$$id', spaceId: 1 } }\n" +
                "                        ],\n" +
                "                        as: 'effort'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$effort' } },\n" +
                "                { $replaceRoot: { newRoot: '$effort' } },\n" +
                "                { $group: { _id: '$user', duration: { $sum: '$duration' } } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'user',\n" +
                "                        let: { id: { $toObjectId: '$_id' }, actual: '$duration' },\n" +
                "                        pipeline: [\n" +
                "                            { $project: { name: '$fullName', actual: '$$actual' } },\n" +
                "                            { $match: { $expr: { $eq: ['$$id', '$_id'] } } },\n" +
                "                            { $project: { _id: 0 } },\n" +
                "                        ],\n" +
                "                        as: 'users'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$users' } },\n" +
                "                { $replaceRoot: { newRoot: '$users' } },\n" +
                "                { $sort:{ name:1} }" +
                "            ],\n" +
                "            workload: [\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'spaces',\n" +
                "                        let: { id: { $toObjectId: '$spaceId' } },\n" +
                "                        pipeline: [\n" +
                "                            { $project: { workHour: '$configurations.workHour', } },\n" +
                "                            { $match: { $expr: { $eq: ['$$id', '$_id'] } } },\n" +
                "                            { $project: { workHour: 1, spaceId: { $toString: '$_id' }, _id: 0 } }\n" +
                "                        ],\n" +
                "                        as: 'spaces'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$spaces' } },\n" +
                "                { $replaceRoot: { newRoot: '$spaces' } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'users_permission',\n" +
                "                        let: { spaceId: '$spaceId', workHour: '$workHour' },\n" +
                "                        pipeline: [\n" +
                "                            { $match: { $expr: { $eq: ['$$spaceId', '$spaceId'] }, permissionFor: 'SPACE', role: { $ne: 'OBSERVER' } } },\n" +
                "                            { $project: { userId: 1, capacity: { $ifNull: ['$capacity', '$$workHour'] }, spaceId: '$$spaceId', _id: 0 } }\n" +
                "                        ],\n" +
                "                        as: 'users'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$users' } },\n" +
                "                { $replaceRoot: { newRoot: '$users' } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'task_schedule',\n" +
                "                        let: { spaceId: '$spaceId', capacity: { $ifNull: ['$capacity', 0] }, userId: '$userId' },\n" +
                "                        pipeline: [\n" +
                "                            { $match: { $and: [{ $expr: { $eq: ['$spaceId', '$$spaceId'] } }, { $expr: { $eq: ['$assignedTo', '$$userId'] } }], scheduleDate: "+dateRange+" } },\n" +
                "                            { $project: { spaceId: 1, userId: '$$userId', capacity: '$$capacity', estimated: '$duration', _id: 0 } }\n" +
                "                        ],\n" +
                "                        as: 'schedules'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$schedules' } },\n" +
                "                { $replaceRoot: { newRoot: '$schedules' } },\n" +
                "                { $group: { _id: { userId: '$userId', spaceId: '$spaceId', capacity: '$capacity' }, estimated: { $sum: '$estimated' } } },\n" +
                "                { $project: { user: '$_id.userId', space: '$_id.spaceId', capacityPerDay: '$_id.capacity', estimated: '$estimated', _id: 0 } },\n" +
                "                { $group: { _id: '$user', capacityPerDay: { $sum: '$capacityPerDay' }, estimated: { $sum: '$estimated' } } },\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'user',\n" +
                "                        let: { id: { $toObjectId: '$_id' } },\n" +
                "                        pipeline: [\n" +
                "                            { $project: { fullName: 1 } },\n" +
                "                            { $match: { $expr: { $eq: ['$$id', '$_id'] } } },\n" +
                "                        ],\n" +
                "                        as: 'user'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$user' } },\n" +
                "                { $project: { estimated: 1, capacity: { $multiply: ['$capacityPerDay', "+workdays+"] }, name: '$user.fullName', _id: 0 } },\n" +
                "                { $sort:{ name:1} }" +
                "            ],\n" +
                "            issues: [\n" +
                "                {\n" +
                "                    $lookup: {\n" +
                "                        from: 'issues',\n" +
                "                        let: { spaceId: '$spaceId' },\n" +
                "                        pipeline: [\n" +
                "                            { $project: { status: 1, _id: 0, spaceId: 1, createdAt: 1 } },\n" +
                "                            { $match: { $expr: { $eq: ['$spaceId', '$$spaceId'] }, createdAt: "+dateRange+" } },\n" +
                "                            { $group: { _id: '$status', count: { $sum: 1 } } }\n" +
                "                        ],\n" +
                "                        as: 'issues'\n" +
                "                    }\n" +
                "                },\n" +
                "                { $unwind: { path: '$issues' } },\n" +
                "                { $replaceRoot: { newRoot: '$issues' } },\n" +
                "                { $group: { _id: '$_id', value: { $sum: '$count' } } },\n" +
                "                {\n" +
                "                    $project: {\n" +
                "                        name: '$_id', _id: 0, value: 1,\n" +
                "                        colorValue: {\n" +
                "                            $switch: {\n" +
                "                                branches: [\n" +
                "                                    { case: { $eq: ['$_id', 'Closed'] }, then: '#A8A4CE' },\n" +
                "                                    { case: { $eq: ['$_id', 'Open'] }, then: '#B762C1' },\n" +
                "                                    { case: { $eq: ['$_id', 'Re-Open'] }, then: '#EA99D5' },\n" +
                "                                    { case: { $eq: ['$_id', 'In-Progress'] }, then: '#FFCDDD' },\n" +
                "                                    { case: { $eq: ['$_id', 'Resolved'] }, then: '#495C83' },\n" +
                "                                    { case: { $eq: ['$_id', 'Not a Fault'] }, then: '#7A86B6' },\n" +
                "                                    { case: { $eq: ['$_id', 'Future'] }, then: '#8946A6' },\n" +
                "                                ],\n" +
                "                                default: '#B762C1'\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    }] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch")).get(0);
    }

    @Override
    public Object tasks(AppRequest request, String currentUserId) {
        boolean isSegment =isValidString(request.getSegment());
        String filterProject = isValidString(request.getProject()) ? "spaceId:'"+request.getProject()+"'," : "";
        String groupSegment = isSegment ? "{ $group:{ _id:'$status', tasks:{ $push:'$$ROOT' } } }, { $project:{ name:'$_id', tasks:1, _id:0 } }": "";
        String filterUser = Objects.equals(request.getAllTask(), true) ? "" : "assignedTo:'"+currentUserId+"'," ;
        String filterDate = "scheduleDate:" + Utils.dateRange(request.getStartDateString(), request.getEndDateString());
        String filter = isSegment ? "subspaceId:'"+request.getSegment()+"'," :"$expr:{ $eq:['$spaceId', '$$spaceId'] },";
        String taskSchedule = taskSchedulePipeline((filter + filterUser + filterDate), groupSegment);

        final String query = "{ aggregate: '" + (isSegment? CollectionName.task_schedule : CollectionName.users_permission) + "', \n" +
                "pipeline:"
                +(isSegment? taskSchedule : userPermissionPipeline(request.getWorkspace(), currentUserId, filterProject, taskSchedule))
                +" allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    private String userPermissionPipeline(String workspace, String currentUserId, String filterProject, String taskSchedulePipeline){
        return  "[\n" +
        "    { $match:{ \n" +
                "        workspaceId:'"+workspace+"',\n" +
                "        userId:'"+currentUserId+"', \n" + filterProject +
                "        permissionFor:'SPACE' \n" +
                "    } },\n" +
                "    { $project:{ spaceId:1, _id:0 }},\n" +
                "    { $lookup:{\n" +
                "        from: 'spaces',\n" +
                "        as:'space',\n" +
                "        let: { id:{ $toObjectId:'$spaceId' } },\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "            { $project:{ name:1, color:1, id:{ $toString:'$_id' }, _id:0 } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$space' },\n" +
                "    { $replaceRoot:{ newRoot:'$space' } },\n" +
                "    { $lookup:{\n" +
                "        from:'task_schedule',\n" +
                "        as:'tasks',\n" +
                "        let:{ spaceId:'$id' },\n" +
                "        pipeline:" +taskSchedulePipeline+
                "    } }" +
                "]";
    }
    private String taskSchedulePipeline(String match, String groupSegment) {
        return  "[" +
                "            { $match:{ "+match+" }},\n" +
                "            { $project:{ taskId:1, _id:0 } },\n" +
                "            { $group:{ _id:'$taskId' } },\n" +
                "            { $lookup:{\n" +
                "                from: 'tasks',\n" +
                "                as:'tasks',\n" +
                "                let: { id:{ $toObjectId:'$_id' } },\n" +
                "                pipeline:[\n" +
                "                    { $match:{ \n" +
                "                        $expr:{ $eq:[ '$$id', '$_id' ] },\n" +
                "                    }},\n" +
                "                    { $project:{\n" +
                "                        _id:0, id:{ $toString:'$_id' },\n" +
                "                        name:1, status:1,\n" +
                "                        priority:{ $switch: { branches: [\n" +
                "                            { case: { $eq: [ '$priority', 1 ] }, then: '#cc0018' },\n" +
                "                            { case: { $eq: [ '$priority', 2 ] }, then: '#cc6600' },\n" +
                "                            { case: { $eq: [ '$priority', 3 ] }, then: '#0096cc' },\n" +
                "                            { case: { $eq: [ '$priority', 4 ] }, then: '#00cc22' }\n" +
                "                            ], default: '#00cc22'\n" +
                "                        } }, \n" +
                "                        deadline:{ $dateToString: { format: \"%Y-%m-%d\", date: \"$deadline\" } },\n" +
                "                    } }\n" +
                "                ]\n" +
                "            } },\n" +
                "            { $unwind:'$tasks' },\n" +
                "            { $replaceRoot:{ newRoot:'$tasks' }}\n" + groupSegment+
                "        ]";
    }

    @Override
    public Object issues(AppRequest request, String currentUserId) {
        return null;
    }

    @Override
    public Object backlogs(AppRequest request, String currentUserId) {
        return null;
    }

    @Override
    public Object tasksList(AppRequest request, String currentUserId) {
        String matchQuery = "";
        if(isValidString(request.getSegment())){
            matchQuery = " subspaceId:'"+request.getSegment()+"'  ";
        } else if(!StringUtils.isEmpty(request.getProject())){
            matchQuery = " spaceId:'"+request.getProject()+"'  ";
        }
        String filterUser = Objects.equals(request.getAllTask(), true) ? "" : "assignedTo:'"+currentUserId+"'," ;
        String filterDate = "scheduleDate:" + Utils.dateRange(request.getStartDateString(), request.getEndDateString())+", " ;
        String query = "{ aggregate:'tasks'," +
                " pipeline:[\n" +
                " { $project:{ _id:0, temp:'temp' } }, { $limit:1 }," +
                " { $facet:{" +
                "   tasks:[" +
                "      { $lookup:{\n" +
                "        from:'task_schedule', as: 'tasks',\n" +
                "        pipeline:[ " + queryTasks(filterDate, filterUser, matchQuery) + " ]\n" +
                "      }},\n" +
                "      { $unwind:'$tasks' },\n" +
                "      { $replaceRoot:{ newRoot:'$tasks' } }" +
                "   ]," +
                "   drafts:[\n" +
                (!isValidString(request.getSegment()) ? " {$match:{ 'temp':0 }}  " :
                "      { $lookup:{\n" +
                "        from:'tasks_draft', as: 'draft',\n" +
                "        pipeline:[ " + queryTaskDraft(request.getSegment()) + " ]\n" +
                "      }},\n" +
                "      { $unwind:'$draft' },\n" +
                "      { $replaceRoot:{ newRoot:'$draft' } }") +
                "   ]," +
                " }},\n" +
                " { $project: { merged: { $concatArrays: ['$tasks','$drafts'] }}},\n" +
                " { $unwind:'$merged' },\n" +
                " { $replaceRoot:{ newRoot:'$merged' }}," +
                " { $sort:{ position:1 }}\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000}}";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object taskStatusPieChart(String spaceId) {
        return ((Document) mongoTemplate.executeCommand("{ "
                + "aggregate: "
                + "'"+CollectionName.task+"'"
                + "pipeline: [\n"
                + (isValidString(spaceId) ? "  { $match:{ spaceId:'"+spaceId+"' } },\n" : "")
                + "  { $group:{ _id:'$status', count:{ $sum:1 } } },\n"
                + "  { $project: { _id:0, name:'$_id', value:'$count'  } }\n"
                + "] allowDiskUse: true, cursor: {batchSize: 20000000000} "+
                "}").get("cursor")).get("firstBatch");
    }

    @Override
    public Object workloadChart(String workspace, String project, String startDate, String endDate, String currentUserId, String workdays) {
        final String dateRange =  "scheduleDate:"+Utils.dateRange(startDate, endDate);
        final String query = "{ aggregate: '" + CollectionName.user + "', \n" +
                "pipeline:[\n" +
                "  { $match:{ loginId:{ $ne:'support@datasoft-bd.com' } }},\n" +
                "  { $project: { _id:0, id:{ $toString:'$_id' }, name:'$fullName' }},\n" +
                "  { $lookup: {\n" +
                "    from: 'task_schedule',\n" +
                "    let: { assignedTo:'$id' },\n" +
                "    pipeline:[\n" +
                "      { $match:{ $expr:{ $eq:['$assignedTo', '$$assignedTo'] }, "+dateRange+" } },\n" +
                "      { $group:{ _id:'', duration:{ $sum:'$duration' } } }\n" +
                "    ],\n" +
                "    as: 'schedule'\n" +
                "  }},\n" +
                "  { $unwind: {\n" +
                "    path: '$schedule',\n" +
                "    preserveNullAndEmptyArrays: true\n" +
                "  }},\n" +
                "  { $project:{ \n" +
                "    name:1, \n" +
                "    capacity:{ $multiply:[ 8, "+workdays+" ] }, \n" +
                "    estimated:{ $ifNull:['$schedule.duration' , 0] } \n" +
                "  }},\n" +
                "  { $sort:{  name:-1  }},\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object invoiceList() {
        String query="{ aggregate: '"+ CollectionName.invoice_data +"', \n" +
                "pipeline: [" +
                "   { $sort:{ invoiceData:-1 } }," +
                "   { $project:{ _id:0, id:{ $toString:'$_id' }, name:'$invoiceNo' } }" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return  ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }


    private String queryTaskDraft(String id){
        return
                "    { $match:{ subSpaceId:'"+id+"' } },\n"+
                "    { $unwind:{ path:'$assignedUsers', preserveNullAndEmptyArrays:true } },\n" +
                "    { $lookup:{\n" +
                "       from:'user', as:'assignedUsers', let:{ id:'$assignedUsers' },\n" +
                "       pipeline:[  \n" +
                "            { $project:{ fullName:1, _id:0, id:{ $toString:'$_id' } } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "       ]\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$assignedUsers', preserveNullAndEmptyArrays:true } },\n" +
                "    { $addFields:{ assignedUsers:'$assignedUsers.fullName' } },\n" +
                "    \n" +
                "    { $lookup:{\n" +
                "       from:'spaces', as:'space', let:{ id:'$spaceId' },\n" +
                "       pipeline:[  \n" +
                "            { $project:{ name:1, _id:0, id:{ $toString:'$_id' } } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "       ]\n" +
                "    } },\n" +
                "    { $unwind:'$space' },\n" +
                "    { $addFields:{ space:'$space.name' } },\n" +
                "    { $project:{ \n" +
                "        id: {$toString:'$_id'},\n" +
                "        name:1,\n" +
                "        position:{$ifNull:['$position',0]},\n" +
                "        status:1,\n" +
                "        type: 'Draft',\n" +
                "        deadline: { $dateToString: { format: '%Y-%m-%d', date: '$deadline' }},\n" +
                "        priority: { $switch: {\n" +
                "            branches: [\n" +
                "              { case: { $eq: [ '$priority', 4 ] }, then: 'Low' },\n" +
                "              { case: { $eq: [ '$priority', 3 ] }, then: 'Normal' },\n" +
                "              { case: { $eq: [ '$priority', 2 ] }, then: 'High' },\n" +
                "              { case: { $eq: [ '$priority', 1 ] }, then: 'Urgent' }\n" +
                "            ],\n" +
                "            default: 'No Priority'\n" +
                "          } },\n" +
                "        space:1,\n" +
                "        user:'$assignedUsers',\n" +
                "      _id:0\n" +
                "  }}";
    }
    private String queryTasks(String filterDate, String filterUser, String matchQuery){
        return
                "  { $match:{ "+filterDate+ filterUser+" "+matchQuery+"   } },\n"+
                "  { $group:{ _id:'$taskId', data:{ $push:'$$ROOT' } } },\n" +
                "  { $project:{ data: { $arrayElemAt: ['$data', 0] } } },\n" +
                "  { $replaceRoot: {\n" +
                "    newRoot: '$data'\n" +
                "  } },\n" +
                "  { $lookup: {\n" +
                "    from: \"tasks\",\n" +
                "    let: {  id:{ $toObjectId:\"$taskId\" } },\n" +
                "    as: \"task\",\n" +
                "    pipeline:[\n" +
                "      { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "      { $project:{ _id:0, name:1, deadline:1, priority:1, status:1, position:1 } },      \n" +
                "    ]\n" +
                "  } },\n" +
                "  { $lookup: {\n" +
                "    from: \"spaces\",\n" +
                "    let: {  id:{ $toObjectId:\"$spaceId\" } },\n" +
                "    as: \"space\",\n" +
                "    pipeline:[\n" +
                "      { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "      { $project:{ _id:0, name:1 } },      \n" +
                "    ]\n" +
                "  } },\n" +
                "  { $lookup: {\n" +
                "    from: \"user\",\n" +
                "    let: {  id:{ $toObjectId:\"$assignedTo\" } },\n" +
                "    as: \"user\",\n" +
                "    pipeline:[\n" +
                "      { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "      { $project:{ _id:0, name:'$fullName' } },      \n" +
                "    ]\n" +
                "  } },\n" +
                "  { $unwind:'$task' },\n" +
                "  { $unwind:'$space' },\n" +
                "  { $unwind:'$user' },\n" +
                "  { $project:{\n" +
                "    \tid:'$taskId',\n" +
                "    \tname:'$task.name',\n" +
                "    \tposition:'$task.position',\n" +
                "    \tstatus: '$task.status',\n" +
                "    \ttype: 'Published',\n" +
                "    \tdeadline: {\n" +
                "        $dateToString: {\n" +
                "          format: '%Y-%m-%d',\n" +
                "          date: '$task.deadline'\n" +
                "        }\n" +
                "      },\n" +
                "    \tpriority: { $switch: {\n" +
                "        branches: [\n" +
                "          { case: { $eq: [ '$task.priority', 4 ] }, then: 'Low' },\n" +
                "          { case: { $eq: [ '$task.priority', 3 ] }, then: 'Normal' },\n" +
                "          { case: { $eq: [ '$task.priority', 2 ] }, then: 'High' },\n" +
                "          { case: { $eq: [ '$task.priority', 1 ] }, then: 'Urgent' }\n" +
                "      \t],\n" +
                "        default: 'No Priority'\n" +
                "      } },\n" +
                "    \tspace:'$space.name',\n" +
                "    \tuser:'$user.name',\n" +
                "      _id:0\n" +
                "  } }" ;
    }

}
