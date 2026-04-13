package com.ds.tracks.testCase;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCaseRepository extends MongoRepository<TestCase, String>,TestCaseDao {
}
