package com.ds.tracks.issue;

import com.ds.tracks.commons.models.PagedResponseRequest;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

public interface IssueService {
    ResponseEntity<?> get(String id);
    ResponseEntity<?> getAll(String workspaceId, String spaceId, String subspaceId, Date startDate, Date endDate, String severity, List<String> tag, String id);
    ResponseEntity<?> save(Issue issue);
    ResponseEntity<?> changeStatus(String id, String status);

    ResponseEntity<?> getComments(String id);

    ResponseEntity<?> getList(PagedResponseRequest responseRequest);

    ResponseEntity<?> getIssues(List<String> ids);

    void linkTask(List<String> issues, String id);

    void unlinkTask(List<String> list, String id);
}
