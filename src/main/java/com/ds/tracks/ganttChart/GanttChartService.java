package com.ds.tracks.ganttChart;

import org.springframework.http.ResponseEntity;

public interface GanttChartService {

    ResponseEntity<?> getGanttChartInitialData(String spaceId);

    ResponseEntity<?> getSubSpacesByFolder(String spaceId,String folderId);

    ResponseEntity<?> getTaskBySubSpace(String subSpaceId);

    ResponseEntity<?> getSubTasksByTasks(String taskId);

    ResponseEntity<?> getSubSpaceBySpace(String spaceId);

}
