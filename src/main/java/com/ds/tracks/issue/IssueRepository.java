package com.ds.tracks.issue;

import com.ds.tracks.commons.dao.CommonDao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends MongoRepository<Issue, String>, IssueDao, CommonDao {
    Issue findFirstById(String id);

    @Query(value="{ 'id': ?0 }", fields="{ 'testCases' : 1, 'id':0 }")
    Issue findTestCasesById(String id);
}
