package com.ds.tracks.dashboard;
import com.ds.tracks.dashboard.model.DashboardDto;
import com.ds.tracks.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @PostMapping("/project-summary")
    public ResponseEntity<?> projectsSummary(
                              @RequestParam Date startDate,
                              @RequestParam Date endDate,
                              @RequestParam(required = false) String spaceId,
                              @RequestParam(required = false) String workspaceId
    ){
        return this.dashboardService.projectsSummary( workspaceId, spaceId, startDate, endDate);
    }

    @PostMapping("/workload-summary")
    public ResponseEntity<?> workload(
            @RequestParam Date startDate,
            @RequestParam Date endDate,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String spaceId){
        return this.dashboardService.workload(workspaceId, spaceId, startDate, endDate);
    }
    @PostMapping("/efforts-summary")
    public ResponseEntity<?> efforts(
            @RequestParam Date startDate,
            @RequestParam Date endDate,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String spaceId){
        return this.dashboardService.efforts(workspaceId, spaceId, startDate, endDate);
    }

    @PostMapping("/tasks-summary")
    public ResponseEntity<?> tasks(
            @RequestParam Date startDate,
            @RequestParam Date endDate,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String spaceId){
        return this.dashboardService.tasks(workspaceId, spaceId, startDate, endDate);
    }

    @PostMapping(value = "/save-note")
    public ResponseEntity<?> saveNote(@RequestParam String note){
        return dashboardService.saveNote(note);
    }
    @GetMapping(value = "/get-note")
    public ResponseEntity<?> getNote(){
        return dashboardService.getNote();
    }
}
