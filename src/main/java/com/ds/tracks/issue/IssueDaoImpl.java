package com.ds.tracks.issue;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ds.tracks.commons.utils.Utils.*;

@Repository
@RequiredArgsConstructor
public class IssueDaoImpl implements IssueDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public Map<String, Object> initial(String spaceId, String subspaceId, String startDate, String endDate, String severity, List<String> tag, String id) {
        String filter = "spaceId:"+quotedString(spaceId);
        if(isValidString(subspaceId)){
            filter = filter.concat(", ").concat("subspaceId:").concat(quotedString(subspaceId));
        }
        if(isValidString(startDate) && isValidString(endDate)){
            filter = filter.concat(", ").concat("createdAt:").concat(dateRange(startDate, endDate));
        }
        if(isValidString(severity)){
            filter = filter.concat(", ").concat("severity:").concat(quotedString(severity));
        }
        filter = filter.concat(", ").concat(listToStringQuery("tags", tag));
        String idFilter = "";
        if(isValidString(id)){
            idFilter = "{ $match:{ id:{$regex:'"+id.trim()+"', $options:'i'} } },";
        }
        final String query = "{ aggregate: '"+ CollectionName.issues +"', \n" +
                "pipeline: [" +
                " { $match:{ "+filter+" } }," +
                " { $project:{ _id:0, name:1, tags:1, tag:1, severity:1, status:1, trackedBy:1, trackedAt:1, id:{$toString:'$_id'} }}," +
                idFilter+
                " { $group:{ _id:'$status', data:{ $push:'$$ROOT' } } }," +
                " { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}}," +
                " { $replaceRoot:{ newRoot:'$data' }}  " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Object> list = (List)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new HashMap<>() : (Map<String, Object>) list.get(0);
    }

    @Override
    public List<Map<String, Object>> findCommentsById(String id, String currentUserId) {
        List<Map<String, Object>> list= null;
        try{
            final String query = "{ aggregate: '"+ CollectionName.issues +"', \n" +
                    "pipeline: [" +
                    " { $match:{ _id:ObjectId('"+id+"') } }," +
                    " { $project:{ comments:1, _id:0 }}," +
                    " { $unwind:{ path:'$comments' }}," +
                    " { $replaceRoot:{ newRoot:'$comments' }}," +
                    " { $project:{ fullName:1, self:{ $eq:['$userId', '"+currentUserId+"' ] }, date:1, comment:1  } }" +
                    "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
            list = (List<Map<String, Object>>)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        } catch (Exception ignored) { }
        return list;
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
//        if(isValidString(responseRequest.getSubSpaceId())){
//            criteria = criteria.concat("subspaceId:'").concat(responseRequest.getSubSpaceId()).concat("', ");
//        }

        if(Objects.nonNull(responseRequest.getStartDate()) && Objects.nonNull(responseRequest.getEndDate()) ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            criteria = criteria.concat("createdAt:").concat(dateRange(sdf.format(responseRequest.getStartDate()), sdf.format(responseRequest.getEndDate()))).concat(", ");
        }
        criteria = criteria.concat(listToStringQuery("severity", responseRequest.getSeverities()));
        criteria = criteria.concat(listToStringQuery("tags", responseRequest.getTags()));
        criteria = criteria.concat(listToStringQuery("status", responseRequest.getStatus()));
        String limit = "{ $limit: "+responseRequest.getSize()+" },";
        String skip = "{ $skip: "+responseRequest.getSize()* responseRequest.getPage()+" },";
        String sort = "{ $sort: {'"+responseRequest.getSortBy()+"':"+( Objects.equals(responseRequest.getSortOrder(), "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.issues +"', \n" +
                "pipeline: [" +
                " { $match:{ "+criteria+" } }," +
                " { $facet:{ " +
                "       data:[" +
                "           { $project:{ _id:0, id:{$toString:'$_id'}, name:1, severity:1, status:1, createdBy:1, createdAt:1 }}," +
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
    public Object getIssues(List<String> ids) {
        Query query =new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        query.fields().include("name", "severity", "status");
        return  mongoTemplate.find(query, Issue.class);
    }

    @Override
    public UpdateResult linkTask(List<String> issues, String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(issues));
        Update update = new Update();
        update.addToSet("tasks", taskId);
        return mongoTemplate.updateMulti(query, update, CollectionName.issues);
    }

    @Override
    public UpdateResult unlinkTask(List<String> issues, String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(issues));
        Update update = new Update();
        update.pull("tasks", taskId);
        return mongoTemplate.updateMulti(query, update, CollectionName.issues);
    }
}
