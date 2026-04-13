package com.ds.tracks.tasks.service;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.model.dto.TaskPositionDto;
import com.ds.tracks.tasks.model.dto.TasksDto;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

public interface TasksService {

    ResponseEntity<String> createTask(TasksDto issue, HttpServletRequest request);

    ResponseEntity<String> saveDraft(TaskDraft draft, HttpServletRequest request);

    ResponseEntity<?> getTask(String id);

    ResponseEntity<?> getList(String spaceId, String subspaceId, Date date, String priority, Boolean showAll);

    ResponseEntity<?> changeStatus(String id, String status, HttpServletRequest request);

    ResponseEntity<?> getAllIssueGroupedByStatus(String workspaceId, String spaceId, String subSpaceId, Boolean showAll, Date startDate, Date endDate, String tracker, String priority, List<String> tags, String viewType);


    ResponseEntity<?> generateCalender(Date startDate, Date endDate,String workspaceId, String spaceId, String subspaceId, Boolean showAll);

    ResponseEntity<?> lock(String id, String type, String spaceId, String subspaceId);

    ResponseEntity<?> getAllTasks(String workspaceId, Date startDate, Date endDate);

    ResponseEntity<?> getPaged(PagedResponseRequest responseRequest);
    ResponseEntity<?> getTemplates();
    ResponseEntity<?> getTemplate(String id);

    ResponseEntity<?> getByIds(List<String> ids);

    ResponseEntity<?> updateLinks(String type, String id, List<String> list);

    ResponseEntity<String> updateTask(String id, String type, String value, HttpServletRequest request);


    ResponseEntity<?> getDraft(String id);

    ResponseEntity<?> getDraftsGrouped(String spaceId, String subSpaceId);

    ResponseEntity<String> deleteDraft(String id, HttpServletRequest request);

    ResponseEntity<String> updateTaskAllField(TasksDto taskDto);

    ResponseEntity<?> deleteByTaskId(String taskId, HttpServletRequest request);

    ResponseEntity<?> updateTags(String id, List<String> tags);

    ResponseEntity<?> overall(PagedResponseRequest responseRequest);

    ResponseEntity<?> allUsersInWorkspace(PagedResponseRequest responseRequest);

    ResponseEntity<?> link(String taskId, List<String> tasks);

    ResponseEntity<?> reassign(String taskId, List<String> users, HttpServletRequest request);

    void reopenTask(String sourceId);

    ResponseEntity<?> monitorList(PagedResponseRequest pagedResponseRequest);

    ResponseEntity<?> tasksListForReposition(PagedResponseRequest pagedResponseRequest);

    ResponseEntity<?> updatePositions(List<TaskPositionDto> positions, String spaceId, String subspaceId, HttpServletRequest request);
}
