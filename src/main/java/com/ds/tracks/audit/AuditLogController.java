package com.ds.tracks.audit;

import com.ds.tracks.commons.models.PagedResponseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestBody PagedResponseRequest pagedResponseRequest) {
        return new ResponseEntity<>(auditLogService.list(pagedResponseRequest), HttpStatus.OK);
    }

    @PostMapping("/download-report")
    public void downloadReport(@RequestBody PagedResponseRequest pagedResponseRequest, HttpServletResponse response) {
        auditLogService.downloadReport(pagedResponseRequest, response);
    }
}
