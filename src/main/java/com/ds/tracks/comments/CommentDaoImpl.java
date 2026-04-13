package com.ds.tracks.comments;

import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CommentDaoImpl implements CommentDao{
    private final MongoTemplate mongoTemplate;
    @Override
    public List<Map<String, Object>> fetch(String userId, String sourceId, String source) {
        final String query = "{ aggregate: '"+ CollectionName.comments +"', \n" +
                "pipeline: [" +
                "   { $match:{ sourceId:'"+sourceId+"', source:'"+source+"' } }," +
                "   { $lookup:{ " +
                "      from:'user', " +
                "      let:{ id:{ $toObjectId:'$userId' } }, " +
                "      pipeline:[" +
                "          { $project:{ fullName:1 } }," +
                "           { $match:{ $expr:{ $eq:[ '$_id', '$$id' ] } }}" +
                "       ], " +
                "       as:'user'  " +
                "   }}," +
                "   { $unwind:{ path:'$user' } }," +
                "   { $project:{ " +
                "       comment:1, " +
                "       name:'$user.fullName', " +
                "       self:{ $eq:[ '$userId', '"+userId+"' ]} , " +
                "       date:1, " +
                "       _id:0 " +
                "   } }"+
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Map<String, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
}
