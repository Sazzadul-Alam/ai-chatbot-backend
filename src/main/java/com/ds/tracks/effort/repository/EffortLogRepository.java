package com.ds.tracks.effort.repository;

import com.ds.tracks.effort.dao.EffortLogDao;
import com.ds.tracks.effort.model.EffortLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EffortLogRepository extends MongoRepository<EffortLog, String>, EffortLogDao {

}