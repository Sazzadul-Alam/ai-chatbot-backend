package com.ds.tracks.effort.controller;
import com.ds.tracks.effort.service.EffortLogService;
import com.ds.tracks.effort.model.EffortLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/effort-log")
@RequiredArgsConstructor
public class EffortLogController {
    private final EffortLogService effortLogService;

    @PostMapping("/entry")
    public ResponseEntity<?> entry(@RequestBody EffortLog effortLog, HttpServletRequest request){
        return effortLogService.entryEffortLog(effortLog, request);
    }
    @PostMapping("/logs")
    public ResponseEntity<?> logs(@RequestParam String id, @RequestParam String type){
        return effortLogService.logs(id, type);
    }
}
