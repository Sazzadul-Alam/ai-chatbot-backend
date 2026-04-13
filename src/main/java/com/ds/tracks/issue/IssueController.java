package com.ds.tracks.issue;

import com.ds.tracks.commons.models.PagedResponseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/issue")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping("/all")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Date startDate,
            @RequestParam(required = false) Date endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) List<String> tags,
            @RequestParam String workspaceId,
            @RequestParam String spaceId,
            @RequestParam(required = false) String subspaceId){
        return issueService.getAll(workspaceId, spaceId, subspaceId, startDate, endDate, severity, tags, id);
    }
    @PostMapping("/get-list")
    public ResponseEntity<?> getList(@RequestBody PagedResponseRequest responseRequest) {
        return issueService.getList(responseRequest);
    }
    @PostMapping("/get-issues-by-id")
    public ResponseEntity<?> getIssues(@RequestBody List<String> ids) {
        return issueService.getIssues(ids);
    }
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Issue issue){
        return issueService.save(issue);
    }
    @PostMapping("/get")
    public ResponseEntity<?> get(@RequestParam String id){
        return issueService.get(id);
    }
    @PostMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestParam String id, @RequestParam String status){
        return issueService.changeStatus(id, status);
    }
    @PostMapping("/fetch-comments")
    public ResponseEntity<?> getComments(@RequestParam String id){
        return issueService.getComments(id);
    }
}
