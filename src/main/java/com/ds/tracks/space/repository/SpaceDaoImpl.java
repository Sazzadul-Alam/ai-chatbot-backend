package com.ds.tracks.space.repository;


import com.ds.tracks.comments.Comment;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.files.FileInfo;
import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.reportData.model.InvoiceData;
import com.ds.tracks.space.model.Folder;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.space.model.dto.SpaceDto;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.model.TaskSchedule;
import com.ds.tracks.tasks.model.Tasks;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.workspace.Workspace;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.models.enums.PermissionLayer.SPACE;
import static com.ds.tracks.commons.models.enums.PermissionLayer.WORKSPACE;
import static com.ds.tracks.commons.utils.Utils.isValidString;
import static com.ds.tracks.commons.utils.Utils.listToStringQuery;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SpaceDaoImpl implements SpaceDao {
    private final MongoTemplate mongoTemplate;


    @Override
    public List<Map>  getSpacewiseUsers(String spaceId) {

        String query = "{ aggregate: '"+ CollectionName.users_permission +"', \n" +
                "pipeline: [\n" +
                "       {$project:{userId:\"$userId\",spaceId:1,_id:0, role:1}},\n" +
                "       {$match:{spaceId:\""+spaceId+"\", role:{$ne:'OBSERVER'}}},\n" +
                "       {$lookup: {\n" +
                "           from:\"user\",\n" +
                "           let: { \"userId\": \"$userId\" },\n" +
                "           pipeline: [\n" +
                "              {$project:{_id:0,\"id\":{ \"$toString\": \"$_id\" },\"fullName\":\"$fullName\",image:\"$image\"}},\n" +
                "             { $match: { $expr: { $eq: [ \"$id\", \"$$userId\" ] } } },\n" +
                "           ],\n" +
                "           as: \"users\"\n" +
                "         }\n" +
                "       },\n" +
                "       {$project:{\"id\":\"$users.id\",\"fullName\":\"$users.fullName\",\"image\":\"$users.image\",\"_id\":0}},\n" +
                "       { $unwind : {\"path\": \"$id\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "       { $unwind : {\"path\": \"$fullName\",\"preserveNullAndEmptyArrays\": true} },\n" +
                "       { $unwind : {\"path\": \"$image\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "       { $sort:{ fullName:1 } }" +
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";

        List<Map> result = (List<Map>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserPermissionsForSpace(String spaceId){
        final String query ="{ aggregate: '"+ CollectionName.users_permission +"', \n" +
                "pipeline: [\n" +
                "{ $match:{\"permissionFor\":\"SPACE\", \"spaceId\":\""+spaceId+"\", role:{$ne:'OBSERVER'}} },\n" +
                "{\n" +
                "   $lookup:\n" +
                "     {\n" +
                "        from: '"+CollectionName.user+"',\n" +
                "        let: { \"userId\": \"$userId\" },\n" +
                "        pipeline: [\n" +
                "          { $project:{_id:0, id:{ \"$toString\": \"$_id\" }, \"fullName\":\"$fullName\",image:\"$image\"}},\n" +
                "          { $match: { $expr: { $eq: [ \"$id\", \"$$userId\" ] } } },\n" +
                "       ],\n" +
                "       as: \"userInfo\"\n" +
                "     }\n" +
                "},\n" +
                "{ $unwind : {\"path\": \"$userInfo\",\"preserveNullAndEmptyArrays\": true}},\n" +
                "{ $project:{_id:0, 'id':'$userId', 'role':1, 'designation':'$designation', \"name\":\"$userInfo.fullName\", \"image\":\"$userInfo.image\", \"uid\":\"$userId\", \"email\":\"$loginId\", capacity:\"$capacity\", status:\"$status\"}}\n"+
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public UpdateResult configureWeekends(String workspaceId, String spaceId, String source, List<String> weekend) {
        if(Objects.equals(source, "space")){
            return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(spaceId)),
                    new Update().set("configurations.weekend", weekend), Space.class);

        } else {
            return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(workspaceId)),
                    new Update().set("configurations.weekend", weekend), Workspace.class);

        }
    }

    @Override
    public UpdateResult configureWorkHour(String workspaceId, String spaceId, String source, Double duration) {
        if(Objects.equals(source, "space")){
            return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(spaceId)),
                    new Update().set("configurations.workHour", duration), Space.class);

        } else {
            return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(workspaceId)),
                    new Update().set("configurations.workHour", duration), Workspace.class);

        }
    }

    @Override
    public Object getWorkConfigurations(String spaceId) {
        final String query = "{ aggregate: '"+ CollectionName.spaces +"', " +
                "  pipeline:[\n" +
                "    { $match:{ _id:ObjectId('"+spaceId+"') }},\n" +
                "    { $project: {'workHour':'$configurations.workHour', 'weekend':'$configurations.weekend', 'holiday':'$configurations.holiday'} },\n" +
                "    { $lookup:{\n" +
                "        from:\"users_permission\",\n" +
                "        let: { \"spaceId\":'"+spaceId+"', \"default\":\"$workHour\" },\n" +
                "        pipeline:[\n" +
                "            {$project:{ \"userId\":\"$userId\",\"space\":\"$spaceId\", \"for\":\"$permissionFor\", \"capacity\":{ $ifNull:[ \"$capacity\", \"$$default\" ] } }},\n" +
                "            {$match:{ $expr: { $and:[ { $eq: [ \"$space\", \"$$spaceId\" ] }, { $eq: [ \"$for\", \"SPACE\" ] } ] }, role:{$ne:'OBSERVER'} } }, \n" +
                "            {$lookup:{\n" +
                "                from:\"user\",\n" +
                "                let: { \"id\":{ \"$toObjectId\":\"$userId\"} },\n" +
                "                pipeline:[\n" +
                "                    { $project: { \"id\":\"$id\", \"name\":\"$fullName\", \"image\":\"$image\", \"email\":\"$loginId\", \"userId\":{\"$toString\":\"$_id\"} } },\n" +
                "                    {$match:{$expr: {  $eq: [ \"$_id\", \"$$id\" ] } }}\n" +
                "                    \n" +
                "                ],\n" +
                "                as:\"user\",\n" +
                "            } },\n" +
                "            { \"$unwind\":{ \"path\":\"$user\" }},\n" +
                "            { \"$project\":{ \n" +
                "                _id:0,\n" +
                "                \"id\":\"$user.userId\",\n" +
                "                \"name\":\"$user.name\",\n" +
                "                \"image\":\"$user.image\",\n" +
                "                \"email\":\"$user.email\",\n" +
                "                \"capacity\":\"$capacity\"}}\n" +
                "        ],\n" +
                "        as:\"users\"\n" +
                "        \n" +
                "        } },\n" +
                "        { $project: { \"spaceId\":0, \"_id\":0 }}\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> res =  (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? new HashMap<>() : res.get(0);
    }

    @Override
    public UpdateResult putSubSpacesIntoFolder(List<String> subspaces, String id) {
        return mongoTemplate.updateMulti(new Query(Criteria.where("id").in(subspaces)), new Update().set("folderId",id), SubSpace.class);
    }

    @Override
    public UpdateResult removeFolderFromSubspaces(List<String> subspaces) {

        return mongoTemplate.updateMulti(new Query(Criteria.where("_id").in(subspaces)), new Update().unset("folderId"), SubSpace.class);
    }

    @Override
    public UpdateResult saveHoliday(List<Holiday> holidayList, String spaceId) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(spaceId)),
                new Update().push("configurations.holiday").each(holidayList),
                Space.class
        );
    }
    @Override
    public UpdateResult saveWorkspaceHoliday(List<Holiday> holidayList, String workspaceId) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(workspaceId)),
                new Update().push("configurations.holiday").each(holidayList),
                Workspace.class
        );
    }

    @Override
    public Map<String, Object> getDetailsWithConfig(String id, Boolean forSpace) {
        if(Objects.isNull(id)){
            return null;
        }
        final String query = "{ aggregate: '"+ (forSpace ? CollectionName.spaces : CollectionName.sup_spaces) +"'," +
                "pipeline:[" +
                "{ $match:{ _id:ObjectId('"+id+"') } }," +
                "{$project:{id:{$toString:\"$_id\"},name:1, image:1, color:1, address:1,phone:1, mnemonic:1, _id:0, menus:1, categories:1, clientType:1, 'startDate':'$plannedStartDate', 'endDate':'$plannedEndDate', 'config':'$configurations'}},\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> res = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return (Objects.isNull(res) || res.isEmpty()) ? null : res.get(0);
    }

    @Override
    public void update(SpaceDto space) {
        Query query = new Query(Criteria.where("_id").is(space.getId()));
        Update update = new Update();
        update.set("name", space.getName());
        update.set("mnemonic", space.getMnemonic());
        space.getStartDate().setHours(6);
        update.set("plannedStartDate", space.getStartDate());
        space.getEndDate().setHours(6);
        update.set("plannedEndDate", space.getEndDate());
        update.set("color", space.getColor());
        update.set("menus", space.getMenus());
        if(Objects.nonNull(space.getCategory()) && !space.getCategory().isEmpty()){
            update.addToSet("configurations.category").each(space.getCategory());
        }
        if(Objects.nonNull(space.getRemoveCategories()) && !space.getRemoveCategories().isEmpty()){
            for(String item : space.getRemoveCategories()){
                update.pull("configurations.category", item);
            }
            update.addToSet("configurations.historicCategory").each(space.getRemoveCategories());
        }
        if(Objects.nonNull(space.getTypes()) && !space.getTypes().isEmpty()){
            update.push("configurations.type").each(space.getTypes());
        }
        if(Objects.nonNull(space.getRemoveTypes()) && !space.getRemoveTypes().isEmpty()){
            for(String item : space.getRemoveTypes()){
                update.pull("configurations.type", item);
            }
            update.addToSet("configurations.historicType").each(space.getRemoveTypes());
        }
        if(Objects.nonNull(space.getStatus()) && !space.getStatus().isEmpty()){
            update.push("configurations.status").each(space.getStatus());
        }
        mongoTemplate.updateFirst(query, update, Objects.equals(space.getSource(), "SPACE")
                ? CollectionName.spaces : CollectionName.sup_spaces);
    }


    @Override
    public String getFinalStage(String spaceId, String subspaceId) {
        String response = null;
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(isValidString(subspaceId) ? subspaceId : spaceId));
        query.fields().include("configurations.lockStatus");
        Map<String, Object> res = mongoTemplate.findOne(query, Map.class, isValidString(subspaceId) ? CollectionName.sup_spaces : CollectionName.spaces);
        if(Objects.nonNull(res) && res.containsKey("configurations")){
            response = ((Map<String, String>) res.get("configurations")).get("lockStatus");
        }
        return response;

    }

    @Override
    public Map<String, Object> getConfigurationsForTask(String id, Boolean forSubspace) {
        final String query = "{ aggregate: '"+ (forSubspace ? CollectionName.sup_spaces :CollectionName.spaces) + "'," +
                "pipeline:[\n" +
                "{ $project:{configurations:1}},\n" +
                "{ $match:{ _id:ObjectId('"+id+"')} },\n" +
                "{ $project:{ category:'$configurations.category', status:'$configurations.status', type:'$configurations.type', _id:0 } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> res = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return (Objects.isNull(res) || res.isEmpty()) ? new HashMap<>() : res.get(0);
    }




    @Override
    public List<?> getList(String currentUserId, String workspaceId) {
        final String query = "{ aggregate: '"+ CollectionName.users_permission + "'," +
                "pipeline:[\n" +
                "    { $match:{ \n" +
                "        workspaceId:'"+workspaceId+"', \n" +
                "        userId:'"+currentUserId+"',\n" +
                "        permissionFor:'SPACE'\n" +
                "    } },\n" +
                "    { $project:{ spaceId:1, _id:0 } },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{id:{$toObjectId:'$spaceId'}},\n" +
                "        pipeline:[\n" +
                "            {$project:{name:1}},\n" +
                "            {$match:{$expr:{$eq:['$_id', '$$id']}}}\n" +
                "        ],\n" +
                "        as:'space'\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$space' } },\n" +
                "    { $project:{ name:'$space.name', id:'$spaceId', _id:0 } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object getSubspaceList(String spaceId) {
        final String query = "{ aggregate: '"+ CollectionName.sub_spaces + "'," +
                "pipeline:[\n" +
                "    { $match:{ \n" +
                "        spaceId:'"+spaceId+"', \n" +
                "    } },\n" +
                "    { $project:{ id:{$toString:'$_id'}, name:1 } },\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getSegmentsList(String spaceId) {
        final String query = "{ aggregate: '"+ CollectionName.sub_spaces + "'," +
                "pipeline:[\n" +
                "    { $match:{ spaceId:'"+spaceId+"' } },\n" +
                "    { $project:{ \n" +
                "        name:1, \n" +
                "        id:{ $toString:'$_id' }, \n" +
                "        _id:0, \n" +
                "        color:1, \n" +
                "        folder:{ $convert: { input: '$folderId', to: 'objectId', onError: '', onNull: '' }} \n" +
                "    } },\n" +
                "    { $group:{ _id:'$folder', segments:{$push:'$$ROOT'} } },\n" +
                "        { $lookup:{ \n" +
                "        from:'folder',\n" +
                "        let:{ id:'$_id' },\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:['$_id', '$$id'] }  } },\n" +
                "            { $project:{ name:1, _id:0, id:{ $toString:'$_id' } } }\n" +
                "        ],\n" +
                "        as:'folder'  \n" +
                "    } },\n" +
                "    { $unwind:{ path:'$folder', preserveNullAndEmptyArrays:true } },\n" +
                "    \n" +
                "    { $project:{ name:'$folder.name', id:'$folder.id',  _id:0, segments:1 } }\n" +
                "    { $sort:{ name:1 } }\n" +
                "\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<Document> getSegmentsByFolderId(String folderId) {
        final String query= "{ aggregate: '"+ CollectionName.folder + "'," +
                "pipeline:[\n" +
                "    { $match:{ _id:ObjectId(\""+folderId+"\") } },\n" +
                "    { $unwind:'$subspaces' },\n" +
                "    { $lookup:{\n" +
                "        from:'sub_spaces',\n" +
                "        let:{ id:'$subspaces' },\n" +
                "        as:'subspace',\n" +
                "        pipeline:[\n" +
                "            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1, color:1 } },\n" +
                "            { $match:{ $expr:{ $eq:['$id', '$$id'] } } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:'$subspace' },\n" +
                "    { $replaceRoot:{ newRoot:'$subspace' } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object segments(String spaceId) {
        final String query = "{ aggregate: '"+ CollectionName.folder + "'," +
                "pipeline:[\n" +
                "    { $facet:{\n" +
                "         grouped:[\n" +
                "                { $match:{ spaceId:'"+spaceId+"'} },\n" +
                "                { $project:{ name:1, id:{ $toString:'$_id' }, _id:0}  },\n" +
                "                { $lookup:{ \n" +
                "                   from:'sub_spaces',\n" +
                "                    let:{ id:'$id' },\n" +
                "                    as:'segments',\n" +
                "                    pipeline:[\n" +
                "                        { $match:{ $expr:{ $eq:[ '$folderId', '$$id'] } }},\n" +
                "                        { $project:{ name:1, id:{ $toString:'$_id' }, color:1, folderId:1, _id:0, menus:1 } },\n" +
                "                    ],\n" +
                "                }},    \n" +
                "            ],\n" +
                "            ungrouped:[\n" +
                "                {  $project:{ limit:'set' } },\n" +
                "                { $limit:1 },\n" +
                "                { $lookup:{ \n" +
                "                   from:'sub_spaces',\n" +
                "                    as:'segments',\n" +
                "                    pipeline:[\n" +
                "                        { $match:{ spaceId:'"+spaceId+"', folderId:{ $eq:null } }},\n" +
                "                        { $project:{ name:1, id:{ $toString:'$_id' }, color:1, _id:0, menus:1 } },\n" +
                "                    ],\n" +
                "                }},\n" +
                "                { $unwind:{ path:'$segments'} },\n" +
                "                { $replaceRoot:{ newRoot:'$segments' } }\n" +
                "               \n" +
                "            ],\n" +
                "    } },\n" +
                "    { $project: { data: { $concatArrays: [ \"$ungrouped\", \"$grouped\" ] } } },\n" +
                "    { $unwind:{ path:'$data' } },\n" +
                "    { $replaceRoot:{ newRoot:'$data' } },\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object projects(String workspaceId, String currentUserId) {
        final String query = "{ aggregate: '"+ CollectionName.users_permission + "'," +
                "pipeline:[\n" +
                "    { $match:{ userId:'"+currentUserId+"', permissionFor:'SPACE' } },\n" +
                "    { $project:{ spaceId:1, role:1, _id:0 } },\n" +
                "    { $lookup:{\n" +
                "            from:'"+CollectionName.spaces+"',\n" +
                "            let:{ id:{$convert: { input: '$spaceId', to: 'objectId', onError: '', onNull: '' } }, role:'$role' },\n" +
                "            pipeline:[\n" +
                "                { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "                { $project:{ name:1, color:'$color', mnemonic:1, clientType:{ $ifNull:['$clientType','Normal'] },  id:{ $toString:'$_id'  }, _id:0, role:'$$role' } },\n" +
                "            ],\n" +
                "            as:'spaces'\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$spaces' } },\n" +
                "    { $replaceRoot:{ newRoot:'$spaces' } }," +
                "    { $sort:{ name:1 } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object allProjects(String workspaceId) {
        final String query = "{ aggregate: '"+ CollectionName.spaces + "'," +
                "pipeline:[\n" +
                "    { $match:{ workspaceId:'"+workspaceId+"' } }," +
                "    { $project:{ name:1, color:'$color', mnemonic:1, clientType:{ $ifNull:[{ $cond: { if: { $in: ['$clientType', [null, '']]}, then: 'Normal', else: '$clientType' }}, 'Normal']},  id:{ $toString:'$_id'  }, _id:0, role:'ADMIN' } },\n" +
                "    { $sort:{ name:1 } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    // Modified super user permission
    @Override
    public Object project(String spaceId, String currentUserId, boolean isSuperAdmin) {
        String query = "";
        if(isSuperAdmin){
            query = "{ aggregate: '"+ CollectionName.spaces + "'," +
                    "pipeline:[\n" +
                    "   { $match:{ _id:ObjectId('"+spaceId+"') } }," +
                    "   { $project:{" +
                    "       name:1, " +
                    "       color:'$color', " +
                    "       mnemonic:1, " +
                    "       image:1, " +
                    "       id:{ $toString:'$_id'  }, " +
                    "       _id:0, " +
                    "       role:'ADMIN' " +
                    "   } }" +
                    "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        } else {
            query = "{ aggregate: '"+ CollectionName.users_permission + "'," +
                    "pipeline:[\n" +
                    "    { $match:{ spaceId:'"+spaceId+"', userId:'"+currentUserId+"', permissionFor:'SPACE' } },\n" +
                    "    { $project:{ spaceId:1, role:1, _id:0 } },\n" +
                    "    { $lookup:{\n" +
                    "            from:'"+CollectionName.spaces+"',\n" +
                    "            let:{ id:{$convert: { input: '$spaceId', to: 'objectId', onError: '', onNull: '' } }, role:'$role' },\n" +
                    "            pipeline:[\n" +
                    "                { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                    "                { $project:{ name:1, color:'$color', mnemonic:1, image:1, id:{ $toString:'$_id'  }, _id:0, role:'$$role' } },\n" +
                    "            ],\n" +
                    "            as:'spaces'\n" +
                    "    } },\n" +
                    "    { $unwind:{ path:'$spaces' } },\n" +
                    "    { $replaceRoot:{ newRoot:'$spaces' } },\n" +
                    "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        }
        List<?> list = (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Object availableWorkspaces(String currentWorkspace, String currentUserId) {
        final String query = "{ aggregate: '"+ CollectionName.users_permission + "'," +
                "pipeline:[\n" +
                "    { $project:{ userId:1, workspaceId:1, _id:0 } },\n" +
                "    { $match:{ userId:'"+currentUserId+"', workspaceId:{ $ne:'"+currentWorkspace+"' }}},\n" +
                "    { $group:{ _id:'$workspaceId' } },\n" +
                "    { $lookup:{\n" +
                "        from:'"+CollectionName.workspace+"',\n" +
                "        let:{ id:{ $toObjectId:'$_id' } },\n" +
                "        pipeline:[\n" +
                "            { $project:{ name:1, color:1, image:1, id:{ $toString:'$_id' } }},\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } }\n" +
                "        ],\n" +
                "        as:'workspaces'\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$workspaces' } },\n" +
                "    { $replaceRoot:{ newRoot: '$workspaces' } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object getWorkspaceDetailsForConfig(String id) {
        final String query = "{ aggregate: '"+ CollectionName.workspace + "'," +
                "pipeline:[\n" +
                "    { $match:{ _id:ObjectId('"+id+"') } },\n" +
                "    { $project:{ _id:0, types:1, categories:1, name:1, stages:1, mnemonic:1, color:1, image:1, designations:1, bankName:1, corpAddr:1, bankAcc:1, bkashAcc:1, bankBranch:1, vatRegNo:1, vatPercentage:1, bankNameUAE: 1, bankAccUAE: 1, bankAccNameUAE: 1, bankBranchUAE: 1, bankSwiftCodeUAE: 1, bankIbanUAE: 1   } },\n" +
                "    { $lookup:{\n" +
                "        from:'"+ CollectionName.users_permission +"',\n" +
                "        pipeline:[\n" +
                "            { $match:{ workspaceId:'"+id+"', permissionFor:'WORKSPACE' } },\n" +
                "            { $project:{ userId:1, role:1, _id:0, id:{$toString:'$_id'} } },\n" +
                "            { $lookup:{\n" +
                "                from:'"+ CollectionName.user +"',\n" +
                "                let:{ id:{ $toObjectId:'$userId' }, role:'$role', permissionId:'$id' },\n" +
                "                pipeline:[\n" +
                "                    { $match:{ $expr:{ $eq:['$_id', '$$id' ] } } },\n" +
                "                    { $project:{ name:'$fullName', _id:0, role:'$$role', email:'$loginId', id:{ $toString:'$_id' } } }\n" +
                "                ],\n" +
                "                as:'users'\n" +
                "            } },\n" +
                "            { $unwind:{ path:'$users' } },\n" +
                "            { $replaceRoot:{ newRoot:'$users' } }\n" +
                "        ],\n" +
                "        as:'users'\n" +
                "    } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> list = (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? Collections.emptyMap() : list.get(0);
    }


    @Override
    public List<String> getAllProjectsUserNotAddedIn(String userId, String workspaceId) {
        String query= "{ aggregate: '"+ CollectionName.spaces + "'," +
                "pipeline:[\n" +
                "    { $project:{ workspaceId:1} },\n" +
                "    { $match:{ workspaceId:'"+workspaceId+"' } },\n" +
                "    { $group:{ _id:'$_id' } },\n" +
                "    { $group:{ _id:'', all:{$push:{$toString:'$_id'}} } },\n" +
                "    { $lookup:{\n" +
                "        from:'"+CollectionName.users_permission+"',\n" +
                "        let:{ id:'$_id' },\n" +
                "        pipeline:[\n" +
                "            { $match:{ permissionFor:'SPACE', userId:'"+userId+"' } },\n" +
                "            { $project:{ spaceId:'$spaceId', _id:0 } }\n" +
                "        ],\n" +
                "        as:'existing'\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$existing', preserveNullAndEmptyArrays:true } },\n" +
                "    { $group:{ _id:'$all', existing:{$push:{$toString:'$existing.spaceId'}} } },\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        if(Objects.nonNull(list) && !list.isEmpty()){
            List<String> all = (List<String>) list.get(0).get("_id");
            List<String> existing = (List<String>) list.get(0).get("existing");
            all.removeAll(existing);
            return all;
        }
        return Collections.emptyList();
    }

    @Override
    public void updateMenu(String id, List<String> menu) {
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), new Update().set("menus", menu), SubSpace.class);
    }

    @Override
    public Object getConfigs() {
        final String query = "{ aggregate: '"+ CollectionName.workspace + "'," +
                "pipeline:[\n" +
                "    { $project:{ category:'$categories', type:'$types', status:'$stages', tags:1, _id:0 } },\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> list = (List<?>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? Collections.emptyMap() : list.get(0);
    }

    @Override
    public String getFolderId(String subSpaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(subSpaceId));
        query.fields().include("folderId");
        SubSpace subSpace = mongoTemplate.findOne(query, SubSpace.class);
        if(Objects.nonNull(subSpace) && isValidString(subSpace.getFolderId())){
            return subSpace.getFolderId();
        }
        return null;
    }

    @Override
    public List<String> getSubspacesByFolderId(String folderId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("folderId").is(folderId));
        query.fields().include("id");
        List<SubSpace> subSpace = mongoTemplate.find(query, SubSpace.class);
        if(subSpace.isEmpty()){
            return Collections.emptyList();
        }
        return subSpace.stream().map(SubSpace::getId).collect(Collectors.toList());
    }


    @Override
    public Document findPrimaryWorkspace(String loginId) {
        final String query = "{ aggregate: '"+ CollectionName.users_permission + "'," +
                "pipeline:[\n" +
                "    { $match:{ loginId:'"+loginId+"' } },\n" +
                "    { $project:{ workspaceId:1, _id:0 } },\n" +
                "    { $limit:1 },\n" +
                "    { $lookup:{\n" +
                "        from:'workspaces',\n" +
                "        let:{ id:{ $toObjectId:'$workspaceId' } },\n" +
                "        as:'workspace',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } } },\n" +
                "            { $project:{\n" +
                "                 id:{ $toString:'$_id' },\n" +
                "                 name:1,\n" +
                "                 color:1,\n" +
                "                 image:1,\n" +
                "                 _id:0\n" +
                "            } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$workspace' },\n" +
                "    { $replaceRoot:{ newRoot:'$workspace' } }" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Document> list = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new Document() : list.get(0);
    }

    @Override
    @Transactional
    public void deleteSegments(List<String> ids) {
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subspaceId").in(ids)), TaskSchedule.class).wasAcknowledged(), "Failed To delete TaskSchedule");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subSpaceId").in(ids)), TaskDraft.class).wasAcknowledged(), "Failed To delete TaskDraft");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subSpaceId").in(ids)), Tasks.class).wasAcknowledged(), "Failed To delete Tasks");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subspaceId").in(ids)), EffortLog.class).wasAcknowledged(), "Failed To delete EffortLog");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subspaceId").in(ids)), Comment.class).wasAcknowledged(), "Failed To delete Comment");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("subspaceId").in(ids)), FileInfo.class).wasAcknowledged(), "Failed To delete FileInfo");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("_id").in(ids)), SubSpace.class).wasAcknowledged(), "Failed To delete SubSpace");
    }

    @Override
    @Transactional
    public void deleteSpace(List<String> ids) {
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), TaskSchedule.class).wasAcknowledged(), "Failed To delete TaskSchedule");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), TaskDraft.class).wasAcknowledged(), "Failed To delete TaskDraft");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), Tasks.class).wasAcknowledged(), "Failed To delete Tasks");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), EffortLog.class).wasAcknowledged(), "Failed To delete EffortLog");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), Comment.class).wasAcknowledged(), "Failed To delete Comment");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), FileInfo.class).wasAcknowledged(), "Failed To delete FileInfo");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), SubSpace.class).wasAcknowledged(), "Failed To delete SubSpace");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), Folder.class).wasAcknowledged(), "Failed To delete Folder");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("spaceId").in(ids)), UsersPermission.class).wasAcknowledged(), "Failed To delete UsersPermission");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("_id").in(ids)), Space.class).wasAcknowledged(), "Failed To delete Space");
    }

    @Override
    @Transactional
    public void deleteWorkspace(String id) {
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("workspaceId").is(id)), InvoiceData.class).wasAcknowledged(), "Failed To delete TaskDraft");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("workspaceId").is(id)), UsersPermission.class).wasAcknowledged(), "Failed To delete TaskDraft");
        Assert.isTrue(mongoTemplate.remove(new Query(Criteria.where("_id").is(id)), Workspace.class).wasAcknowledged(), "Failed To delete TaskDraft");

    }

    @Override
    public List<String> getStages(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        query.fields().include("stages");
        Workspace workspace = mongoTemplate.findOne(query, Workspace.class);
        return Objects.nonNull(workspace) && Objects.nonNull(workspace.getStages())? workspace.getStages() : Arrays.asList("Todo", "Completed");
    }

    @Override
    public void addDesignation(String id, String designation) {
        Update update = new Update();
        update.addToSet("designations", designation);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, CollectionName.workspace);
    }
    @Override
    public void removeDesignation(String id, String designation) {
        Update update = new Update();
        update.pull("designations", designation);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, CollectionName.workspace);
        mongoTemplate.updateMulti(new Query(Criteria.where("workspaceId").is(id).and("designation").is(designation)), new Update().set("designation", null), CollectionName.users_permission);
    }
    @Override
    public void editDesignation(String id, String designation, String newDesignation) {
        mongoTemplate.updateMulti(new Query(Criteria.where("workspaceId").is(id).and("designation").is(designation)),
                new Update().set("designation", newDesignation), CollectionName.users_permission);
    }

    @Override
    public void removeWorkspaceSuperuser(String workspaceId, String userId) {
        mongoTemplate.remove(new Query(Criteria.where("workspaceId").is(workspaceId).and("userId").is(userId).and("permissionFor").is(WORKSPACE)), UsersPermission.class);
        mongoTemplate.remove(new Query(Criteria.where("workspaceId").is(workspaceId).and("userId").is(userId).and("permissionFor").is(SPACE).and("role").is("OBSERVER")), UsersPermission.class);
    }

    @Override
    public void removeFromSpace(String workspace, String space, String userId) {
        mongoTemplate.remove(new Query(Criteria.where("spaceId").is(space).and("userId").is(userId).and("permissionFor").is(SPACE)), UsersPermission.class);
    }

    //==============TAGS=============//
    @Override
    public List<String> getTags(String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        query.fields().include("tags");
        Workspace workspace = mongoTemplate.findOne(query, Workspace.class);
        return Objects.nonNull(workspace) && Objects.nonNull(workspace.getTags()) ? workspace.getTags() : new ArrayList<>();
    }
    @Override
    public void saveTag(String tag, String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        Update update = new Update();
        update.addToSet("tags", tag);
        mongoTemplate.updateFirst(query, update, Workspace.class);
    }

    @Override
    public List<Document> getUserListForSpace(String space) {
        final String query = "{ aggregate: '"+ CollectionName.user + "'," +
                "pipeline:[" +
                "    { $match:{ status:'ACTIVE' } },\n" +
                "    { $project:{ id:{ $toString:'$_id' }, fullName:1, email:'$loginId', image:1, role:1, _id:0 } },\n" +
                "    { $lookup:{ \n" +
                "        from:'users_permission',\n" +
                "        let:{ 'email':'$email'  },\n" +
                "        as:'permission',\n" +
                "        pipeline:[\n" +
                "            { $match:{ \n" +
                "                $expr:{ $eq:[ '$loginId', '$$email' ] }, \n" +
                "                permissionFor:'SPACE', spaceId:'"+space+"' \n" +
                "            }},\n" +
                "            { $project:{ role:1, _id:0 } }\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $unwind:{ path:'$permission', preserveNullAndEmptyArrays:true } },\n" +
                "    { $project:{\n" +
                "        name:'$fullName', \n" +
                "        email:1, \n" +
                "        id:1, \n" +
                "        image:1, \n" +
                "        role:{ $cond: { if: { $eq: [ '$permission.role', 'OBSERVER' ] }, then: null, else: '$permission.role' } }\n" +
                "    }}," +
                "    { $sort:{ name:1 } }" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public void editSegmentConfiguration(String subspace, String source, String param, String action) {
        Update update = new Update();
        if(Objects.equals(action, "remove")){
            update.pull(source, param);
        } else {
            update.addToSet(source, param);
        }
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(subspace)), update, CollectionName.sub_spaces);
    }

    @Override
    public Object getProjectsInWorkspace(String id) {
        final String query = "{ aggregate: '"+ CollectionName.spaces + "'," +
                "pipeline:[\n" +
                "{ $match:{ workspaceId:'"+id+"' } },\n" +
                "{ $project:{ _id:0, id:{ $toString:'$_id'}, name:1, mnemonic:1 } }," +
                "{ $sort:{ name:1 } }" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return  ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public Object findWorkspaceHolidays(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        query.fields().include("configurations");
        Workspace workspace = mongoTemplate.findOne(query, Workspace.class);
        return Objects.nonNull(workspace.getConfigurations()) && Objects.nonNull(workspace.getConfigurations().getHoliday()) ? workspace.getConfigurations().getHoliday() : new ArrayList<>();
    }

    @Override
    public List<String> findAllSpaces(String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("workspaceId").is(workspaceId));
        query.fields().include("id");
        return  mongoTemplate.find(query, Space.class).stream().map(Space::getId).collect(Collectors.toList());
    }

    @Override
    public void incrementWorkspaceInvoice(String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        Update update = new Update();
        update.inc("lastInvoiceNumber");
        mongoTemplate.updateFirst(query, update, Workspace.class);

    }

    @Override
    public void addWorkspaceTaskConfig(String type, String value, String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        Update update = new Update();
        update.addToSet(Objects.equals(type, "taskType")? "types": "categories", value);
        mongoTemplate.updateFirst(query, update, Workspace.class);

    }

    @Override
    public void removeWorkspaceTaskConfig(String type, String value, String workspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        Update update = new Update();
        update.pull(Objects.equals(type, "taskType")? "types": "categories", value);
        mongoTemplate.updateFirst(query, update, Workspace.class);

    }

    @Override
    public void updateWorkspaceStages(String workspaceId, List<String> stages) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(workspaceId));
        Update update = new Update();
        update.set("stages", stages);
        mongoTemplate.updateFirst(query, update, Workspace.class);

    }

    @Override
    public void addCategoryAndAddSegmentsToFolder(String spaceId, String category, String folderId, List<String> subspaces) {
        Update update = new Update();
        update.addToSet("categories", category);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(spaceId)), update, CollectionName.spaces);

        Update update2 = new Update();
        update2.addToSet("subspaces").each(subspaces);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(folderId)), update2, CollectionName.folder);
    }

    @Override
    public Object getFileList(String id) {
        final String query = "{ aggregate: '"+ CollectionName.file_info + "'," +
                "pipeline:[\n" +
                "    { $match:{ spaceId:'"+id+"', source:'space', category:'details' }},\n" +
                "    { $facet:{\n" +
                "        files:[\n" +
                "            { $match:{ sourceId:'random' } },\n" +
                "            { $project:{\n" +
                "                _id:0,\n" +
                "                id:{ $toString:'$_id' },\n" +
                "                filename:1\n" +
                "            }}\n" +
                "        \n" +
                "        ],\n" +
                "        mandatoryFiles:[\n" +
                "            { $match:{ sourceId:{ $ne:'random' } } },\n" +
                "            \n" +
                "            { $project:{\n" +
                "                _id:0,\n" +
                "                sourceId:1,\n" +
                "                id:{ $toString:'$_id' },\n" +
                "                filename:1\n" +
                "            }},\n" +
                "            { $group:{ _id:'$sourceId', data:{ $push:'$$ROOT' } } },\n" +
                "            { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: { $arrayElemAt: ['$data', 0] }  }]  ]} }}},\n" +
                "            { $replaceRoot: { newRoot:'$data' } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$mandatoryFiles', preserveNullAndEmptyArrays:true } }" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";;
        List<Document> list = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return Objects.nonNull(list) && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public void updateType(String spaceId, String type) {
        Update update = new Update();
        update.set("clientType", type);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(spaceId)), update, CollectionName.spaces);
    }

    @Override
    public Object findListOfUserFullNames(String id) {
        String query = isValidString(id)
                ?  "{ aggregate: '"+ CollectionName.users_permission +"', \n" +
                "pipeline: [\n" +
                "   {$match:{spaceId:\""+id+"\", role:{$ne:'OBSERVER'}}},\n" +
                "   {$lookup: {\n" +
                "       from:'user',\n" +
                "       let: { 'id':'$userId' },\n" +
                "       pipeline: [\n" +
                "         { $project:{_id:0, 'id':{ $toString: '$_id' }, 'fullName':1 }},\n" +
                "         { $match: { $expr: { $eq: [ '$id','$$id' ] } } },\n" +
                "       ],\n" +
                "       as: 'users'\n" +
                "     }\n" +
                "   },\n" +
                "   { $unwind:'$users' },\n" +
                "   { $replaceRoot:{ newRoot:'$users' } },\n" +
                "   { $sort:{ fullName:1 } }  \n" +
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }"
                : "{ aggregate: '"+ CollectionName.user +"', \n" +
                "pipeline: [\n" +
                "   { $project:{_id:0, 'id':{ $toString: '$_id' }, 'fullName':1 }},\n" +
                "   { $sort:{ fullName:1 } } "+
                "],\n" +
                "allowDiskUse: true, cursor: {batchSize: 20000000000} }"
            ;
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public void updateTag(String spaceId, List<String> tags) {
        Update update = new Update();
        update.addToSet("tags").each(tags);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(spaceId)), update, CollectionName.spaces);

    }
}
