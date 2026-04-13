package com.ds.tracks.template;


import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TemplateDaoImpl implements TemplateDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Map<String, Object>> findTemplatesByCategory(String category) {
        if(Objects.isNull(category)){
            return null;
        }
        final String query = "{ aggregate: '"+ CollectionName.template +"'," +
                "pipeline:[" +
                "    { $match:{category:'"+category+"'} },\n" +
                "    { $lookup:{\n" +
                "        from:'"+CollectionName.template_tasks+"',\n" +
                "        let:{ id:{ $toString:'$_id' } },\n" +
                "        as:'tasks',\n" +
                "        pipeline:[\n" +
                "            { $match:{ $expr:{ $eq:[ '$templateId', '$$id' ] } } },\n" +
                "            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1 } },\n" +
                "        ]\n" +
                "    } },\n" +
                "    { $project:{ id:{ $toString:'$_id' }, name:1, tasks:1, _id:0 } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<Map<String, Object>> findTemplatesByTasks(List<String> selectedTasks) {
        if(Objects.isNull(selectedTasks) || selectedTasks.isEmpty()){
            return new ArrayList<>();
        }
        String tasks = String.join("','", selectedTasks);
        final String query = "{ aggregate: '"+ CollectionName.template +"'," +
                "pipeline:[\n" +
                "    {  $project:{ id:{ $toString:'$_id' }, templateId:1, _id:0 }  },\n" +
                "    { $match:{ id:{ $in:[ '"+tasks+"' ] } } },   \n" +
                "    { $group:{ _id:'$templateId' } },\n" +
                "    { $project:{ _id:0, id:'$_id' } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public void updateTaskSerial(List<CustomTemplateTasks> tasks) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CustomTemplateTasks.class);
        for (CustomTemplateTasks task : tasks) {
            Query query = new Query(Criteria.where("_id").is(task.getId()));
            Update update = new Update().set("position", task.getPosition());
            bulkOps.updateOne(query, update);
        }
        bulkOps.execute();
    }
}
