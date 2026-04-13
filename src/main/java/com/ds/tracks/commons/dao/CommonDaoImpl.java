package com.ds.tracks.commons.dao;

import com.ds.tracks.commons.models.KeyValuePair;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class CommonDaoImpl implements CommonDao{
    private final MongoTemplate mongoTemplate;

    /**
     *
     * @param id Id of the document
     * @param sets updates to a specific field in document
     * @param pushes adds to array field of document
     * @param collection
     * @return Update Result
     */
    @Override
    public UpdateResult update(String id, List<KeyValuePair> sets, List<KeyValuePair> pushes, String collection) {
        Update update = new Update();
        if(Objects.nonNull(sets)) {
            for (KeyValuePair toUpdate : sets) {
                update.set(toUpdate.getKey(), toUpdate.getValue());
            }
        }
        if(Objects.nonNull(pushes)){
            for(KeyValuePair toPush : pushes){
                update.push(toPush.getKey(), toPush.getValue());
            }
        }
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, collection);
    }


}
