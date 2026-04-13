package com.ds.tracks.testCase;

import com.ds.tracks.commons.utils.CollectionName;
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
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TestCaseDaoImpl implements TestCaseDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean changeStatus(String id, String status, TasksHistory tasksHistory) {
        try{
            UpdateResult updateResult = mongoTemplate.updateFirst(
                    new Query().addCriteria(Criteria.where("_id").is(id)),
                    new Update().set("status", status).push("history", tasksHistory),
                    CollectionName.test_case);
            return updateResult.getMatchedCount() > 0;
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    @Override
    public Map<String, Object> getBoard(String spaceId, List<String> subspaces, String startDate, String endDate, List<String> tags, String id, String refId) {
        String filter = "spaceId:"+quotedString(spaceId);
        if(!subspaces.isEmpty()){

            filter = filter.concat(", ").concat(listToStringQuery("subspaceId", subspaces));
        } else {
            filter = filter.concat(", ").concat("previousSubspaceId:{ $exists:false }");
        }
        if(isValidString(startDate) && isValidString(endDate)){
            filter = filter.concat("createdAt:").concat(dateRange(startDate, endDate)).concat(", ");
        }
        filter = filter.concat(listToStringQuery("tags", tags));
        String idFilter = "";
        if(isValidString(id)){
            filter = filter.concat("generatedId:{$regex:"+quotedString(id.trim())+", $options:'i'},");
        }
        if(isValidString(refId)){
            filter = filter.concat("manualId:{$regex:"+quotedString(refId.trim())+", $options:'i'},");
        }
        final String query = "{ aggregate: '"+ CollectionName.test_case +"', \n" +
                "pipeline: [" +
                " { $match:{ "+filter+" } }," +
                " { $project:{ _id:0, name:1, tags:1, status:1, generatedId:1 , id:{$toString:'$_id'} }}," +
                " { $group:{ _id:'$status', data:{ $push:'$$ROOT' } } }," +
                " { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$_id', v: '$data'  }]  ]} }}}," +
                " { $replaceRoot:{ newRoot:'$data' }}  " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Object> list = (List)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new HashMap<>() : (Map<String, Object>) list.get(0);
    }

    @Override
    public Map<String, Object> getBoardAsTestCase(String spaceId, List<String> subspaces, String startDate, String endDate, List<String> tags, String id, String refId) {
        String filter = "spaceId:"+quotedString(spaceId);
        if(!subspaces.isEmpty()){

            filter = filter.concat(", ").concat(listToStringQuery("subspaceId", subspaces));
        } else {
            filter = filter.concat(", ").concat("previousSubspaceId:{ $exists:false }");
        }
        if(isValidString(startDate) && isValidString(endDate)){
            filter = filter.concat("createdAt:").concat(dateRange(startDate, endDate)).concat(", ");
        }
        filter = filter.concat(listToStringQuery("tags", tags));
        String idFilter = "";
        if(isValidString(id)){
            filter = filter.concat("generatedId:{$regex:"+quotedString(id.trim())+", $options:'i'},");
        }
        if(isValidString(refId)){
            filter = filter.concat("manualId:{$regex:"+quotedString(refId.trim())+", $options:'i'},");
        }
        final String query = "{ aggregate: '"+ CollectionName.test_case +"', \n" +
                "pipeline: [" +
                " { $match:{ "+filter+" } }," +
                " { $project:{ id:{$toString:'$_id'} }}," +
                " { $group:{ _id:'', data:{ $push:'$id' } } }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<Object> list = (List)((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return list.isEmpty() ? new HashMap<>() : (Map<String, Object>) list.get(0);
    }

    @Override
    public Object getPage(String spaceId, List<String> subspaces, String startDate, String endDate, List<String> tags, List<String> status, String sortBy, String sortOrder, int page, int size) {
        String criteria = "";
        criteria = criteria.concat("spaceId:'").concat(spaceId).concat("', ");
        criteria = criteria.concat(listToStringQuery("subspaceId", subspaces));
        if(isValidString(startDate) && isValidString(endDate)){
            criteria = criteria.concat("createdAt:").concat(dateRange(startDate, endDate)).concat(", ");
        }
        criteria = criteria.concat(listToStringQuery("tags", tags));
        criteria = criteria.concat(listToStringQuery("status", status));
        String limit = "{ $limit: "+size+" },";
        String skip = "{ $skip: "+(page*size)+" },";
        String sort = "{ $sort: {'"+sortBy+"':"+( Objects.equals(sortOrder, "asc") ? -1 : 1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.test_case +"', \n" +
                "pipeline: [" +
                " { $match:{ "+criteria+" } }," +
                " { $facet:{ " +
                "       data:[" +
                "           { $project:{ _id:0, id:{$toString:'$_id'}, name:1, status:1, tags:1 }}," +
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
    public boolean updateLinks(String id, String type, List<String> newIds, TasksHistory history) {
        if(isValidString(id) && Arrays.asList("Issue", "Backlog").contains(type)){
            Query existingIdsQuery = new Query();
            existingIdsQuery.addCriteria(Criteria.where("_id").is(id));
            existingIdsQuery.fields().include(type.equals("Issue") ? "issues" : "backlogs");
            TestCase testCase = mongoTemplate.findOne(existingIdsQuery, TestCase.class);
            if(Objects.nonNull(testCase)){
                List<String> existingIds = type.equals("Issue") ? testCase.getIssues() : testCase.getBacklogs();
                if(Objects.isNull(existingIds)){
                    existingIds = new ArrayList<>();
                }
                mongoTemplate.updateFirst(
                        new Query().addCriteria(Criteria.where("_id").is(id)),
                        new Update().set(type.equals("Issue") ? "issues" : "backlogs", newIds),
                        TestCase.class);
                List<String> needToPull = new ArrayList<>();
                if(Objects.isNull(newIds) || newIds.isEmpty()){
                    needToPull = existingIds;
                } else {
                    for(String existingId : existingIds){
                        if(newIds.contains(existingId)){
                            newIds.remove(existingId);
                        } else {
                            needToPull.add(existingId);
                        }
                    }
                }
                if(!newIds.isEmpty()){
                    mongoTemplate.updateFirst(
                            new Query().addCriteria(Criteria.where("_id").in(newIds)),
                            new Update().addToSet("testCases", id),
                            type.equals("Issue") ? CollectionName.issues : CollectionName.backlog);
                }
                if(!needToPull.isEmpty()){
                    mongoTemplate.updateFirst(
                            new Query().addCriteria(Criteria.where("_id").in(needToPull)),
                            new Update().pull("testCases", id),
                            type.equals("Issue") ? CollectionName.issues : CollectionName.backlog);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getList(List<String> ids) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        query.fields().include("id", "name", "status");
        return mongoTemplate.find(query, TestCase.class);
    }

    @Override
    public void link(List<String> testCases, String id, String source) {
        mongoTemplate.updateMulti(new Query().addCriteria(Criteria.where("_id").in(testCases)), new Update().addToSet(source, id), TestCase.class);
    }

    @Override
    public void unlink(List<String> testCases, String id, String source) {
        mongoTemplate.updateMulti(new Query().addCriteria(Criteria.where("_id").in(testCases)), new Update().pull(source, id), TestCase.class);
    }

    @Override
    public List<TestCase> findBySubspaceId(String subspaceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("subspaceId").is(subspaceId));
        return mongoTemplate.find(query, TestCase.class);
    }

    @Override
    public List<Document> getVersionHistory(String id) {
        final String query = "{ aggregate: '"+ CollectionName.test_case +"', \n" +
                "pipeline: [" +
                "    { $match:{ _id:ObjectId('"+id+"') } },\n" +
                "    { $project:{ versions:1 } },\n" +
                "    { $unwind:'$versions' },\n" +
                "    { $replaceRoot:{ newRoot:'$versions' } },\n" +
                "    { $lookup:{\n" +
                "        from:'sub_spaces',\n" +
                "        let:{ id:{ $toObjectId:'$subspaceId' } },\n" +
                "        as:'subspaces',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:['$$id', '$_id'] } } },\n" +
                "            { $project:{ name:1, folderId:1, _id:0 } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$subspaces' },\n" +
                "    { $lookup:{\n" +
                "        from:'folder',\n" +
                "        let:{ id:{ $toObjectId:'$subspaces.folderId' } },\n" +
                "        as:'folder',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:['$$id', '$_id'] } } },\n" +
                "            { $project:{ name:1, _id:0 } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$folder', preserveNullAndEmptyArrays:true } },\n" +
                "    \n" +
                "    { $project:{ version:1, segment:{ $ifNull:['$subspaces.name', 'Project Root'] }, folder:'$folder.name', status:1 } }" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
}
