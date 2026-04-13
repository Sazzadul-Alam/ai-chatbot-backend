package com.ds.tracks.tasks.repository;

import com.ds.tracks.commons.dao.CommonDao;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.tasks.model.Tasks;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TasksRepository extends MongoRepository<Tasks, String>, CommonDao {
    Tasks findFirstById(String id);

    @Query(value = "{ 'spaceId': ?0, 'startDate' : { $gt: ?1, $lt: ?2 } }", fields = "{ 'description':0 }")
    List<Tasks> findIssuesBySpaceIdAndStartDateBetween(String spaceId, Date start, Date end);

    @Query(value="{}", fields="{ 'name' : 1, 'priority' : 1, 'issuedTo' : 1, 'status' : 1}")
    List<Tasks> getAllIssueGroupedByStatus();

    @Query(value="{ 'id': ?0 }", fields="{ 'generatedId' : 1, 'id':0 }")
    Tasks getGeneratedIdById(String parentTaskId);

    @Query(value="{ 'id': ?0 }", fields="{ 'spaceId' : 1, 'subSpaceId':1,  'id':0 }")
    Tasks findSpaceIdByTaskId(String taskId);
}
