package com.ds.tracks.backlog;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.*;
import static com.ds.tracks.commons.utils.Utils.listToStringQuery;

@Repository
@RequiredArgsConstructor
public class BacklogDaoImpl implements BacklogDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public UpdateResult changeStatus(String id, String status, TasksHistory history) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(id)),
                new Update().set("status", status)
                        .push("history", history),
                Backlog.class
        );
    }

    @Override
    public UpdateResult link(String id, String subspaceId) {
        return null;
    }

    @Override
    public UpdateResult update(String id, String title, String description, Double storyPoint, List<String> tags, TasksHistory history) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where("id").is(id)),
                new Update()
                    .set("name", title)
                    .set("description", description)
                    .set("storyPoint", storyPoint)
                    .set("tags", tags)
                    .push("history", history)
                    .set("updatedAt", new Date())
                    .set("updatedBy", SecurityContextHolder.getContext().getAuthentication().getName()),
                Backlog.class
            );
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
        String criteria = "";
        if(isValidString(responseRequest.getSpaceId())){
            criteria = criteria.concat("spaceId:'").concat(responseRequest.getSpaceId()).concat("', ");
        }
        if(isValidString(responseRequest.getSubSpaceId())){
            criteria = criteria.concat("subSpaceId:'").concat(responseRequest.getSubSpaceId()).concat("', ");
        }
        if(Objects.nonNull(responseRequest.getStartDate()) && Objects.nonNull(responseRequest.getEndDate()) ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            criteria = criteria.concat("createdAt:").concat(dateRange(sdf.format(responseRequest.getStartDate()), sdf.format(responseRequest.getEndDate()))).concat(", ");
        }
        criteria = criteria.concat(listToStringQuery("status", responseRequest.getStatus()));
        criteria = criteria.concat(listToStringQuery("tags", responseRequest.getTags()));
        String limit = "{ $limit: "+responseRequest.getSize()+" },";
        String skip = "{ $skip: "+responseRequest.getSize()* responseRequest.getPage()+" },";
        String sort = "{ $sort: {'"+responseRequest.getSortBy()+"':"+( Objects.equals(responseRequest.getSortOrder(), "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.backlog +"', \n" +
                "pipeline: [" +
                " { $match:{ "+criteria+" } }," +
                " { $facet:{ " +
                "       data:[" +
                "           { $project:{ _id:0, id:{$toString:'$_id'}, name:1, status:1, storyPoint:1, createdAt:1 }}," +
                skip+limit+sort+
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
        query.fields().include("name", "storyPoint", "createdAt");
        return  mongoTemplate.find(query, Backlog.class);
    }

    @Override
    public UpdateResult linkTask(List<String> backlogs, String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(backlogs));
        Update update = new Update();
        update.addToSet("tasks", taskId);
        return mongoTemplate.updateMulti(query, update, CollectionName.backlog);
    }

    @Override
    public UpdateResult unlinkTask(List<String> backlogs, String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(backlogs));
        Update update = new Update();
        update.pull("tasks", taskId);
        return mongoTemplate.updateMulti(query, update, CollectionName.backlog);
    }

    @Override
    public Map<String, Object> getList(String spaceId, String subSpaceId, String startDate, String endDate, List<String> tags, String id) {
        String filter = "spaceId:"+quotedString(spaceId);
        if(isValidString(subSpaceId)){
            filter = filter.concat(", ").concat("subSpaceId:").concat(quotedString(subSpaceId));
        } else {
            filter = filter.concat(", ").concat("subSpaceId:{ $exists:false }");

        }
        if(isValidString(startDate) && isValidString(endDate)){
            filter = filter.concat(", ").concat("createdAt:").concat(dateRange(startDate, endDate));
        }
        if(isValidString(id)){
            filter = filter.concat(", ").concat("generatedId:{$regex:").concat(quotedString(id.trim())).concat(", $options:'i'}");
        }
        filter = filter.concat(", ").concat(listToStringQuery("tags", tags));

        final String query = "{ aggregate: '"+ CollectionName.backlog +"', \n" +
                "pipeline: [" +
                " { $match:{ "+filter+" } }," +
                " { $sort:{ _id:-1 }}," +
                " { $project:{ _id:0, name:1, tags:1, storyPoint:1, status:1, id:{$toString:'$_id'} }}," +
                " { $group:{ _id:'$status', data:{ $push:'$$ROOT' } } }," +
                " { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}}," +
                " { $replaceRoot:{ newRoot:'$data' }}  " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Object> list = (List)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new HashMap<>() : (Map<String, Object>) list.get(0);
    }
}
