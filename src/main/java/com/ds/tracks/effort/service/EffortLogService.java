package com.ds.tracks.effort.service;

import com.ds.tracks.effort.model.EffortLog;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface EffortLogService {
    ResponseEntity<?> entryEffortLog(EffortLog effortLog, HttpServletRequest request);
    ResponseEntity<?> logs(String id, String type);
}
