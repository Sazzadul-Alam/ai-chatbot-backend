package com.ds.tracks.notification;

import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class NotificationDaoImpl implements NotificationDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public Map<String, Object> getNotifications(String userId, String workspaceId) {
        final String query = "{ aggregate: '" + CollectionName.notifications + "', \n" +
                "pipeline: [\n" +
                "    { $match:{ userId:'"+userId+"' }},\n" +
                "    { $sort:{ _id:-1 } },\n" +
                "    { $project:{ source:1, sourceId:1, message:1, date:1, read:1, id:{ $toString:'$_id' }, _id:0 } },\n" +
                "    { $facet:{\n" +
                "        'read':[\n" +
                "            { $match:{ read:true } },\n" +
                "            { $limit:10 }\n" +
                "        ],\n" +
                "        'unread':[\n" +
                "            { $match:{ read:false } },\n" +
                "            { $limit:50 }\n" +
                "        ]\n" +
                "    } }\n" +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";

        List<Map<String, Object>> notifications =  (List<Map<String, Object>>) (((Map<Object, Object>) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch"));
        return (Objects.nonNull(notifications) && !notifications.isEmpty()) ? notifications.get(0) : Collections.EMPTY_MAP;
    }

    @Override
    public void markRead(String id) {
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), new Update().set("read", true), CollectionName.notifications);
    }
}
