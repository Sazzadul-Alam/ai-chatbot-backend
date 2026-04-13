package com.ds.tracks.issue;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.mongodb.client.result.UpdateResult;

import java.util.List;
import java.util.Map;

public interface IssueDao {
    Map<String, Object> initial(String spaceId, String subspaceId, String startDate, String endDate, String severity, List<String> tag, String id);

    List<Map<String, Object>> findCommentsById(String id, String currentUserId);

    Object getPagedResponse(PagedResponseRequest responseRequest);

    Object getIssues(List<String> ids);

    UpdateResult linkTask(List<String> issues, String taskId);

    UpdateResult unlinkTask(List<String> issues, String taskId);
}
