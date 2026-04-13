package com.ds.tracks.backlog;

import com.ds.tracks.commons.models.PagedResponseRequest;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

public interface BacklogService {
    ResponseEntity<?> create(Backlog backlog);

    ResponseEntity<?> list(String spaceId, String subSpaceId, Date startDate, Date endDate, String id, List<String> tags);

    ResponseEntity<?> changeStatus(String id, String status);

    ResponseEntity<?> link(String id, String link);

    ResponseEntity<?> update(Backlog backlog);

    ResponseEntity<?> get(String id);

    ResponseEntity<?> getPage(PagedResponseRequest pagedResponseRequest);

    ResponseEntity<?> getByIds(List<String> ids);

    void linkTask(List<String> backlogs, String id);

    void unlinkTask(List<String> list, String id);
}
