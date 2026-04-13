package com.ds.tracks.workload;

import com.ds.tracks.tasks.model.dto.TasksDto;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface WorkloadDao {
    List<Map<String, Object>> findByDateAndAssignedToForSpaceOrSubSpace(String taskDate, String assignedTo,String workspaceId, String spaceId, String subSpaceId);

    Map<String, List<Map<String, Object>>> findWorkloadForSpaceOrSubspaceByDateBetween(String spaceId,String subSpaceId,String startDate, String endDate);

    Map<String, List<Document>> findWorkloadForWorkspace(List<String> spaces, String startDate, String endDate);
}
