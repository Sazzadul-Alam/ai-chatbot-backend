package com.ds.tracks.dashboard.service;

import com.ds.tracks.dashboard.model.DashboardDto;
import org.springframework.http.ResponseEntity;

import java.util.Date;

public interface DashboardService {
    ResponseEntity<?> projectsSummary( String workspaceId, String spaceId,  Date startDate, Date endDate);
    ResponseEntity<?> workload(String workspaceId, String spaceId, Date startDate, Date endDate);
    ResponseEntity<?> tasks(String workspaceId, String spaceId, Date start, Date end);

    ResponseEntity<?> efforts(String workspaceId, String spaceId, Date startDate, Date endDate);

    ResponseEntity<?> saveNote(String note);

    ResponseEntity<?> getNote();
}
