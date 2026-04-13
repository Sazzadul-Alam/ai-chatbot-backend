package com.ds.tracks.reportData.repository;

import com.ds.tracks.reportData.model.ReportData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ReportDataRepository extends MongoRepository<ReportData, String> {

    List<ReportData> findAllByWorkspaceId(String workspaceId);

    ReportData findByTaskIdAndSubTaskId(String taskId, String subTaskId);

    ReportData findByTaskIdAndSubTaskIdIsNull(String taskId);
}
