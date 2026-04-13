package com.ds.tracks.backlog;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.mongodb.client.result.UpdateResult;

import java.util.List;
import java.util.Map;

public interface BacklogDao {
    UpdateResult changeStatus(String id, String status, TasksHistory history);
    UpdateResult link(String id, String subspaceId);

    UpdateResult update(String id, String title, String description, Double storyPoint, List<String> tags, TasksHistory history);

    Object getPagedResponse(PagedResponseRequest pagedResponseRequest);

    Object getByIds(List<String> ids);

    UpdateResult linkTask(List<String> backlogs, String taskId);

    UpdateResult unlinkTask(List<String> backlogs, String taskId);

    Map<String, Object> getList(String spaceId, String subSpaceId, String start, String end, List<String> tags, String id);
}
