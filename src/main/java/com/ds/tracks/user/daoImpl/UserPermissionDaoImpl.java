package com.ds.tracks.user.daoImpl;

import com.ds.tracks.commons.models.enums.PermissionLayer;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.user.dao.UserPermissionDao;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.user.model.dto.UserPermissionDto;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Repository
@RequiredArgsConstructor
public class UserPermissionDaoImpl implements UserPermissionDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public long updateCapacity(UserPermissionDto userPermissionDto) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(userPermissionDto.getUserId()).and("permissionFor").is("SPACE").and("spaceId").is(userPermissionDto.getSpaceId())),
                new Update().set("capacity", userPermissionDto.getCapacity()),
                UsersPermission.class).getModifiedCount();
    }

    @Override
    public UpdateResult changeRole(String id, String spaceId, String role) {
        Update update = new Update();
        update.set("role", role);
        return mongoTemplate.updateFirst(new Query(Criteria.where("userId").is(id).and("spaceId").is(spaceId)), update, UsersPermission.class);
    }
    @Override
    public UpdateResult changeDesignation(String id, String spaceId, String designation) {
        Update update = new Update();
        update.set("designation", designation);
        return mongoTemplate.updateFirst(new Query(Criteria.where("userId").is(id).and("spaceId").is(spaceId)), update, UsersPermission.class);
    }

    @Override
    public List<Map<String, Object>> getUsersForSpace(String spaceId) {
        final String query = "{ aggregate: '"+CollectionName.users_permission+"'," +
                "pipeline:[\n" +
                "{ $match:{spaceId:\""+spaceId+"\", permissionFor:\"SPACE\", role:{$ne:'OBSERVER'}} },\n" +
                "{ $project:{userId:1, _id:0, capacity:1} },\n" +
                "{ $lookup:{\n" +
                "    from:'"+CollectionName.user+"',\n" +
                "    let:{'id':{$toObjectId:'$userId'}, 'capacity':'$capacity'},\n" +
                "    pipeline:[\n" +
                "        {$match:{$expr:{$eq:['$_id','$$id']}}},\n" +
                "        {$project:{\n" +
                "            _id:0,\n" +
                "            userId:{$toString:'$_id'},\n" +
                "            userName:'$fullName',\n" +
                "            image:'$image',\n" +
                "            capacity:'$$capacity'\n" +
                "        }}\n" +
                "    ],\n" +
                "    as:'user'\n" +
                "}},\n" +
                "{$unwind:{path:'$user'}},\n" +
                "{ $replaceRoot: { newRoot: \"$user\" }},\n" +
                "],allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");

    }

    @Override
    public List<String> findAllSpaceIdByWorkspaceIdAndPermissionForAndUserId(String workspaceId,  String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("workspaceId").is(workspaceId).and("permissionFor").is("SPACE").and("userId").is(userId));
        query.fields().include("spaceId");
        return mongoTemplate.find(query, UsersPermission.class).stream().map(UsersPermission::getSpaceId).collect(Collectors.toList());
    }

    @Override
    public Object getUsersInWorkspace(String id) {
        final String query = "{ aggregate: '"+CollectionName.user+"'," +
                "pipeline:[\n" +
                "    { $match:{ loginId:{$ne:'support@datasoft-bd.com'} } },\n" +
                "    { $project:{ id:{$toString:'$_id'}, _id:0, name:'$fullName', email:'$loginId' } },\n" +
                "    { $sort:{ name:1 } }\n" +
                "],allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<String> findAllSpaceIdByWorkspaceIdAndUserIdAndPermissionForAndRoleNe(String workspaceId, String userId, String permissionFor, String role) {
        Query query = new Query();
        query.fields().include("spaceId");
        query.addCriteria(Criteria.where("workspaceId").is(workspaceId).and("userId").is(userId).and("permissionFor").is(permissionFor).and("role").ne(role));
        return mongoTemplate.find(query, UsersPermission.class).stream().map(UsersPermission::getSpaceId).collect(Collectors.toList());

    }

}
