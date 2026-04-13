package com.ds.tracks.app;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping("/analytics")
    public ResponseEntity<?> analytics(@RequestBody AppRequest request){
        return appService.analytics(request);
    }
    @GetMapping("/pie")
    public ResponseEntity<?> taskStatusPieChart(@RequestParam(required = false) String spaceId ){
        return appService.taskStatusPieChart(spaceId);
    }
    @PostMapping("/column")
    public ResponseEntity<?> workloadChart(@RequestBody AppRequest request){
        return appService.workloadChart(request);
    }
    @PostMapping("/tasks")
    public ResponseEntity<?> tasks(@RequestBody AppRequest request){
        return appService.tasks(request);
    }
    @PostMapping("/issues")
    public ResponseEntity<?> issues(@RequestBody AppRequest request){
        return appService.issues(request);
    }
    @PostMapping("/backlogs")
    public ResponseEntity<?> backlogs(@RequestBody AppRequest request){
        return appService.backlogs(request);
    }
    @PostMapping("/task-list")
    public ResponseEntity<?> tasksList(@RequestBody AppRequest appRequest){
        return appService.tasksList(appRequest);
    }

    @GetMapping("/task-configurations")
    public ResponseEntity<?> configurations(){
        return appService.configurations();
    }
    @GetMapping("/invoice-list")
    public ResponseEntity<?> invoiceList(){
        return appService.invoiceList();
    }
}
