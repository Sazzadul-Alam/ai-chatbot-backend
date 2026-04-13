package com.ds.tracks.tasks.dao;


import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.ds.tracks.tasks.model.dto.ModificationDto;
import com.ds.tracks.tasks.model.dto.TaskPositionDto;
import com.ds.tracks.tasks.model.dto.TaskStatus;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TasksDao {
    List<Map<String, Object>> getAllIssueGroupedByStatus(String spaceId, String subSpaceId);
    Map<String, Object> getGroupedTasks(String spaceId, String subSpaceId, String userId, String startDate, String endDate, String tracker, String priority, List<String> tags);

    UpdateResult update(String id, String field, Object value, TasksHistory log);

    UpdateResult update(String source, String id, List<IdNameRelationDto> updates, TasksHistory log);
    UpdateResult updateSubtask(String id, String field, Object value, TasksHistory log);
    UpdateResult updateSubtaskByTaskId(String id, String field, Object value, TasksHistory log);

    Map<String, Object> findDraftById(String taskId);
    Map<String, Object> findTaskById(String taskId);
    Document findTaskLocationById(String taskId);

    Map<String, Object>  findSubTaskById(String id);

    List<TaskStatus> getAllStatusBySpaceOrSubSpace(String spaceId, String subSpaceId);

    UpdateResult reassignForWorkload(String id, String userId);
    UpdateResult changeDateOfSchedule(String id, Date date, String taskDate);
    Map<String, List<Object>> findTasksForCalenderView(String startDate, String endDate, String spaceId, String subspaceId, String userId);

    List<Map<String, Object>> getAllTaskByDate(String spaceId, String subspaceId, String taskDate, String priority, String userId);
    List<Map<String, String>> getAllAssignedUsers(String taskId);

    List<Map<String, Object>> getAllAssignedTasks(String userId, String startDate, String endDate);
    List<Map<String, Object>> getAllSpaces(String userId, String workspaceId);

    UpdateResult updateTaskOrSubtaskForEffortLog(String subTaskId, Double completion, Double duration, boolean validString);

    Object getPagedResponse(PagedResponseRequest responseRequest);

    Object getByIds(List<String> ids);

    UpdateResult link(List<String> tasks, String id);

    List<String> getLinks(String type, String id);

    void unlink(List<String> list, String id);

    List<Document> report(ReportDto requestParam);

    Object reportPaged(ReportDto requestParam);

    Object getGroupedDrafts(String spaceId, String subSpaceId, String currentUserId);

    void deleteByTaskId(String taskId);

    Object getOverallBoard(String startDate, String endDate, String userId, List<String> spaceIds, PagedResponseRequest responseRequest);
    Object getAllUsers(String workspaceId);

    void changeStageOfAllTasksInWorkspace(String workspaceId, String oldStage, String newStage);

    void changeStageOfAllTaskDraftsInWorkspace(String workspaceId, String oldStage, String newStage);

    void reassign(String id, List<ModificationDto> list, Boolean fromToday);

    Object getAllTasksPendingApproval(String currentUserId);

    Object getMonitorList(PagedResponseRequest pagedResponseRequest);

    Integer findLastPosition(String subSpaceId);

    Object tasksListForReposition(PagedResponseRequest pagedResponseRequest);

    void updatePositions(List<TaskPositionDto> positions);

    void updateSchedules(String taskId, String s, TasksHistory history);
}
