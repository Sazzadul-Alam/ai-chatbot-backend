package com.ds.tracks.ganttChart;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gantt/chart")
@RequiredArgsConstructor
public class GanttChartController {


    private final GanttChartService ganttChartService;

    @PostMapping("/initial")
    public ResponseEntity<?> getGanttChartInitialData(@RequestParam String spaceId){
        return ganttChartService.getGanttChartInitialData(spaceId);
    }

    @PostMapping("/space")
    public ResponseEntity<?> getSubSpaceBySpace(@RequestParam String spaceId){
        return ganttChartService.getSubSpaceBySpace(spaceId);
    }

    @PostMapping("/subSpace")
    public ResponseEntity<?> getSubSpacesByFolder(@RequestParam String spaceId,@RequestParam String folderId){
        return ganttChartService.getSubSpacesByFolder(spaceId,folderId);
    }

    @PostMapping("/task")
    public ResponseEntity<?> getTaskBySubSpace(@RequestParam String subSpaceId){
        return ganttChartService.getTaskBySubSpace(subSpaceId);
    }

    @PostMapping("/subTask")
    public ResponseEntity<?> getSubTasksByTasks(@RequestParam String taskId){
        return ganttChartService.getSubTasksByTasks(taskId);
    }
}
