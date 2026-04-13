package com.ds.tracks.ganttChart;

import java.util.List;

public interface GanttChartDao {

    List<?> getGanttChartInitialData(String spaceId);

    List<?> getSubSpacesByFolder(String folderId);

    List<?> getTaskBySubSpace(String subSpaceId);

    List<?> getSubTasksByTasks(String taskId);

    List<?> getSubSpaceBySpace(String spaceId);

    List<?> getSubSpaceEffortDates(String spaceId);

    List<?> getTaskEffortDates(String spaceId);
    List<?> getSubTaskEffortDates(String taskId);
}
