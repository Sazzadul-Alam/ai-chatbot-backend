package com.ds.tracks.audit;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.space.repository.SubSpaceRepository;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.model.Tasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.utils.Utils.dateRange;
import static com.ds.tracks.commons.utils.Utils.noDataResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final MongoTemplate mongoTemplate;
    private final AuditLogRepository repository;

    public Object list(PagedResponseRequest responseRequest){
        if(Objects.isNull(responseRequest.getSize())){
            responseRequest.setSize(30);
        }
        if(Objects.isNull(responseRequest.getPage())){
            responseRequest.setPage(0);
        }
        if(Objects.isNull(responseRequest.getSortBy())){
            responseRequest.setSortBy("_id");
        }
        String limit = "{ $limit: "+responseRequest.getSize()+" },";
        String skip = "{ $skip: "+responseRequest.getSize()* responseRequest.getPage()+" },";
        String sort = "{ $sort: {'"+responseRequest.getSortBy()+"':"+( Objects.equals(responseRequest.getSortOrder(), "asc") ? 1 : -1 )+"} },";
        final String query = "{ aggregate: '"+ CollectionName.audit_log +"', \n" +
                "pipeline: [" +  baseQuery() +
                " { $facet:{ " +
                "       data:[ " + skip+limit+ " ]," +
                "       metaData: [ { $count: 'total' } ]" +
                " } }," +
                "{ $unwind:{ path:'$metaData' } }," +
                "{ $project:{ data:1, totalData:'$metaData.total' } } " +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        save("Viewed Audit Log");
        return  res.isEmpty() ? noDataResponse() : res.get(0);
    }

    public void downloadReport(PagedResponseRequest requestParam, HttpServletResponse response) {
        if (Objects.isNull(requestParam.getStartDate())) {
            requestParam.setStartDate(new Date());
        }
        if (Objects.isNull(requestParam.getEndDate())) {
            requestParam.setEndDate(requestParam.getStartDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = sdf.format(requestParam.getStartDate());
        String endDate = sdf.format(requestParam.getEndDate());
        String dateQuery = "date:{'$gte':ISODate('"+startDate+"T00:00:00.000+00:00'), '$lte':ISODate('"+endDate+"T23:59:59.999+00:00')}";
        BasicQuery query = new BasicQuery("{ "+dateQuery+" }");
        long totalRows = mongoTemplate.count(query, AuditLog.class);
        if(totalRows > 0){
            Workbook workbook = null;
            ServletOutputStream outputStream = null;
            try{
                workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet(startDate+" TO "+endDate);
                // Header
                Row row = sheet.createRow(0);
                Cell cell = null;
                cell = row.createCell(0); cell.setCellValue("Date");
                cell = row.createCell(1); cell.setCellValue("Login ID");
                cell = row.createCell(2); cell.setCellValue("IP Address");
                cell = row.createCell(3); cell.setCellValue("Action");
                cell = row.createCell(4); cell.setCellValue("Task");
                cell = row.createCell(5); cell.setCellValue("Client");
                cell = row.createCell(6); cell.setCellValue("Segment");

                int rowIdx = 0;
                while (totalRows > rowIdx){
                    List<Document> dataList = this.prepareListData(rowIdx, dateQuery);
                    for (Document data : dataList) {
                        rowIdx ++;
                        row = sheet.createRow(rowIdx);
                        cell = row.createCell(0); cell.setCellValue(data.get("date").toString());
                        cell = row.createCell(1); cell.setCellValue(data.get("loginId").toString());
                        cell = row.createCell(2); cell.setCellValue(data.get("ipAddress").toString());
                        cell = row.createCell(3); cell.setCellValue(data.get("action").toString());
                        cell = row.createCell(4); cell.setCellValue(Objects.nonNull(data.get("task")) ? data.get("task").toString() : "");
                        cell = row.createCell(5); cell.setCellValue(Objects.nonNull(data.get("space")) ? data.get("space").toString() : "");
                        cell = row.createCell(6); cell.setCellValue(Objects.nonNull(data.get("subspace")) ? data.get("subspace").toString() : "");
                    }

                }
                outputStream = response.getOutputStream();
                workbook.write(outputStream);
            } catch (Exception e){
                log.error(e.getMessage(), e.getCause());

            } finally {
                if(Objects.nonNull(workbook)){
                    try {
                        workbook.close();
                        outputStream.close();
                    } catch (Exception e) {
                        log.error("Unable to close workbook", e.getCause());
                    }
                }
            }
        }
        save("Downloaded Audit Log Report");
    }


    private List<Document> prepareListData(long skip, String dateQuery){
        final String query = "{ aggregate: '"+ CollectionName.audit_log +"', \n" +
                "pipeline: [" +
                "    { $match:{ "+ dateQuery +" }},\n"+
                "    { $sort:{ _id:-1 } },\n" +
                "    { $skip: "+skip+" },\n" +
                "    { $limit:100000 },\n" +
                baseQuery() +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }


    private String baseQuery(){
        return  "    { $lookup:{\n" +
                "        from:'tasks',\n" +
                "        let:{ id:{ $cond:[ { $eq:[ '$source', 'tasks' ] }, '$sourceId', null ] } },\n" +
                "        as:'task',\n" +
                "        pipeline:[ \n" +
                "            { $project:{  name:1, _id:0, id:{ $toString:'$_id' }, spaceId:1, subSpaceId:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $lookup:{\n" +
                "        from:'tasks_draft',\n" +
                "        let:{ id:{ $cond:[ { $eq:[ '$source', 'tasks_draft' ] }, '$sourceId', null ] } },\n" +
                "        as:'tasks_draft',\n" +
                "        pipeline:[ \n" +
                "            { $project:{  name:1, _id:0, id:{ $toString:'$_id' }, spaceId:1, subSpaceId:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$task', preserveNullAndEmptyArrays:true } },\n" +
                "    { $unwind:{ path:'$tasks_draft', preserveNullAndEmptyArrays:true } },\n" +
                "    { $addFields:{\n" +
                "        task:{ $ifNull:['$task.name', '$tasks_draft.name'] },\n" +
                "        spaceId: { $ifNull:[ '$spaceId', { $ifNull:['$task.spaceId', '$tasks_draft.name'] } ] },\n" +
                "        subspaceId:{ $ifNull:[ '$subspaceId', { $ifNull:['$task.subSpaceId', '$tasks_draft.name'] } ] }\n" +
                "    } },\n" +
                "    { $lookup:{\n" +
                "        from:'spaces',\n" +
                "        let:{ id:'$spaceId' },\n" +
                "        as:'space',\n" +
                "        pipeline:[ \n" +
                "            { $project:{  name:1, _id:0, id:{ $toString:'$_id' } } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$space', preserveNullAndEmptyArrays:true } },\n" +
                "    { $lookup:{\n" +
                "        from:'sub_spaces',\n" +
                "        let:{ id:'$subspaceId' },\n" +
                "        as:'subspace',\n" +
                "        pipeline:[ \n" +
                "            { $project:{  name:1, _id:0, id:{ $toString:'$_id' } } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$$id', '$id' ] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$subspace', preserveNullAndEmptyArrays:true } },\n" +
                "    { $addFields:{\n" +
                "        space:'$space.name',\n" +
                "        subspace:'$subspace.name',\n" +
                "        date:{ $dateToString: { format: '%Y-%m-%d %H:%M', date: { $toDate:'$_id' }}}\n" +
                "    } },\n" +
                "    { $lookup:{\n" +
                "        from:'user',\n" +
                "        let:{ loginId:'$loginId' },   \n" +
                "        as:'user',\n" +
                "        pipeline:[ \n" +
                "            { $project:{ loginId:1, fullName:1 } },\n" +
                "            { $match:{ $expr:{ $eq:['$loginId', '$$loginId'] } } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$user' },\n" +
                "    { $sort:{ _id:-1 } },\n" +
                "    { $addFields:{ \n" +
                "        user:'$user.fullName',\n" +
                "        task:{\n" +
                "          $cond: {\n" +
                "            if: { $or:[{$eq: ['$source', 'tasks']}, {$eq: ['$source', 'tasks_draft']}] },\n" +
                "            then: { $ifNull: ['$task', '$sourceName'] },\n" +
                "            else: ''\n" +
                "          }\n" +
                "        },\n" +
                "        space:{\n" +
                "          $cond: {\n" +
                "            if: { $or:{$eq: ['$source', 'spaces']} },\n" +
                "            then: { $ifNull: ['$space', '$sourceName'] },\n" +
                "            else: { $ifNull: ['$space', '$spaceName'] }\n" +
                "          }\n" +
                "        },\n" +
                "        subspace:{\n" +
                "          $cond: {\n" +
                "            if: { $or:{$eq: ['$source', 'sub_spaces']} },\n" +
                "            then: { $ifNull: ['$subspace', '$sourceName'] },\n" +
                "            else: { $ifNull: ['$subspace', '$subspaceName'] }\n" +
                "          }\n" +
                "        }\n" +
                "    } },\n" +
                "    { $project:{ _id:0, _class:0 } }, \n";
    }

    public void save(String action) {
        AuditLog log = new AuditLog(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), action);
        repository.save(log);
    }
    public void save(String action, String source, String sourceId) {
        AuditLog log = new AuditLog(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), action);
        log.setSource(source);
        log.setSourceId(sourceId);
        log.setSourceName(getSourceName(source, sourceId));
        repository.save(log);
    }
    public void save(String action, String source, String sourceId,  String spaceId, String subspaceId) {
        AuditLog log = new AuditLog(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), action);
        log.setSpaceId(spaceId);
        log.setSubspaceId(subspaceId);
        log.setSource(source);
        log.setSourceId(sourceId);
        log.setSourceName(getSourceName(source, sourceId));
        repository.save(log);
    }

    private String getSourceName(String source, String sourceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(sourceId));
        query.fields().include("name");
        Document document = mongoTemplate.findOne(query,  Document.class, source);
        return Objects.nonNull(document) && document.containsKey("name") && Objects.nonNull(document.get("name")) ? document.get("name").toString() : null;
    }

    public void migrateDraftToPublishedTask(String draftId, String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("source").is(CollectionName.tasks_draft).and("sourceId").is(draftId));
        Update update = new Update();
        update.set("source",CollectionName.task).set("sourceId", id);
        mongoTemplate.updateMulti(query, update, AuditLog.class);
    }

    public void logDeletedSource(String action, String source, String sourceId, String spaceId, String subspaceId, String sourceName) {
        AuditLog log = new AuditLog(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), action);
        log.setSpaceId(spaceId);
        log.setSubspaceId(subspaceId);
        log.setSource(source);
        log.setSourceId(sourceId);
        log.setSourceName(sourceName);
        repository.save(log);
        if(Objects.equals(source, CollectionName.sub_spaces)){
            Query find = new Query();
            find.addCriteria(Criteria.where("subSpaceId").is(sourceId));
            find.fields().include("id");
            List<String> tids = mongoTemplate.find(find, Tasks.class).stream().map(Tasks::getId).collect(Collectors.toList());
            List<String> tdids = mongoTemplate.find(find, TaskDraft.class).stream().map(TaskDraft::getId).collect(Collectors.toList());
            Query query = new Query();
            query.addCriteria(
                    new Criteria().orOperator(
                        Criteria.where("source").is(CollectionName.task).and("sourceId").in(tids),
                        Criteria.where("source").is(CollectionName.tasks_draft).and("sourceId").in(tdids)
                    )
            );
            Update update = new Update();
            update.set("isSourceDeleted", true).set("spaceId", spaceId).set("subspaceId", subspaceId).set("subspaceName", sourceName);
            mongoTemplate.updateMulti(query, update, AuditLog.class);
        } else {
            Query query = new Query();
            query.addCriteria(Criteria.where("source").is(source).and("sourceId").is(sourceId));
            Update update = new Update();
            update.set("isSourceDeleted", true).set("spaceId", spaceId).set("subspaceId", subspaceId);
            mongoTemplate.updateMulti(query, update, AuditLog.class);
        }
    }


    public void logMultipleDeletedSources(String action, String source, String sourceId, String spaceId, List<String> subspaceIds, String sourceName) {
        AuditLog log = new AuditLog(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest(), action);
        log.setSpaceId(spaceId);
        log.setSource(source);
        log.setSourceId(sourceId);
        log.setSourceName(sourceName);
        log.setIsSourceDeleted(true);
        repository.save(log);

        for(String subspaceId : subspaceIds){
            final String query = "{ aggregate: '"+ CollectionName.sub_spaces +"', pipeline: [\n" +
                    "    { $match:{ _id:ObjectId('"+subspaceId+"') } }\n" +
                    "    { $lookup:{ \n" +
                    "        from:'spaces',\n" +
                    "        let:{ id:'$spaceId' },\n" +
                    "        as:'space',\n" +
                    "        pipeline:[\n" +
                    "            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1 } },\n" +
                    "            { $match:{ $expr:{ $eq:['$id','$$id'] } } }\n" +
                    "        ]\n" +
                    "    } },\n" +
                    "    { $unwind:'$space' },\n" +
                    "    { $lookup:{ \n" +
                    "        from:'tasks',\n" +
                    "        let:{ id:{ $toString:'$_id' } },\n" +
                    "        as:'tasks',\n" +
                    "        pipeline:[\n" +
                    "            { $match:{ $expr:{ $eq:['$subSpaceId','$$id'] } } },\n" +
                    "            { $group:{ _id:'', data:{ $push:{ $toString:'$_id' } } } }, \n" +
                    "        ]\n" +
                    "    } },\n" +
                    "    { $lookup:{ \n" +
                    "        from:'tasks_draft',\n" +
                    "        let:{ id:{ $toString:'$_id' } },\n" +
                    "        as:'tasks_draft',\n" +
                    "        pipeline:[\n" +
                    "            { $match:{ $expr:{ $eq:['$subSpaceId','$$id'] } } },\n" +
                    "            { $group:{ _id:'', data:{ $push:{ $toString:'$_id' } } } }, \n" +
                    "        ]\n" +
                    "    } },\n" +
                    "    { $project:{ \n" +
                    "        _id:0, \n" +
                    "        subspaceName:'$name', \n" +
                    "        spaceName:'$space.name', \n" +
                    "        taskIds:'$tasks.data', \n" +
                    "        draftTaskIds:'$tasks_draft.data'  \n" +
                    "    } },\n" +
                    "    { $unwind:{ path:'$taskIds', preserveNullAndEmptyArrays:true } }," +
                    "    { $unwind:{ path:'$draftTaskIds', preserveNullAndEmptyArrays:true } }," +
                    "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
            List<Document> res = (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
            if(Objects.nonNull(res) && !res.isEmpty()){
                Query updateQry = new Query();
                updateQry.addCriteria(
                        new Criteria().orOperator(
                                Criteria.where("source").is(CollectionName.task).and("sourceId").in(res.get(0).get("taskIds")),
                                Criteria.where("source").is(CollectionName.tasks_draft).and("sourceId").in(res.get(0).get("draftTaskIds")),
                                Criteria.where("source").is(CollectionName.sub_spaces).and("sourceId").is(subspaceId),
                                Criteria.where("source").is(CollectionName.folder).and("sourceId").is(sourceId)
                        )
                );
                Update update = new Update();
                update.set("isSourceDeleted", true).set("spaceId", spaceId).set("subspaceId", subspaceId).set("subspaceName", res.get(0).get("subspaceName")).set("spaceName", res.get(0).get("spaceName"));
                mongoTemplate.updateMulti(updateQry, update, AuditLog.class);
            }
            if(Objects.equals(source, CollectionName.spaces)){
                Query updateQry = new Query();
                updateQry.addCriteria(Criteria.where("spaceId").is(spaceId));
                Update update = new Update();
                update.set("isSourceDeleted", true).set("spaceName", sourceName);
                mongoTemplate.updateMulti(updateQry, update, AuditLog.class);
            }
        }
    }
}
