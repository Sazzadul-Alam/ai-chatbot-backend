package com.ds.tracks.backlog;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/backlog")
@RequiredArgsConstructor
public class BacklogController {

    private final BacklogService backlogService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Backlog backlog){
        return this.backlogService.create(backlog);
    }
    @PostMapping("/get")
    public ResponseEntity<?> get(@RequestParam String id){
        return this.backlogService.get(id);
    }
    @PostMapping("/get-by-id")
    public ResponseEntity<?> getByIds(@RequestBody List<String> ids){
        return this.backlogService.getByIds(ids);
    }
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody Backlog backlog){
        return this.backlogService.update(backlog);
    }

    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestParam String spaceId,
                                  @RequestParam(required = false) String subSpaceId,
                                  @RequestParam(required = false) Date startDate,
                                  @RequestParam(required = false) Date endDate,
                                  @RequestParam(required = false) String severity,
                                  @RequestParam(required = false) String id,
                                  @RequestParam(required = false) List<String> tags
    ){
        return this.backlogService.list(spaceId, subSpaceId, startDate, endDate, id, tags);
    }
    @PostMapping("/page")
    public ResponseEntity<?> getPage(@RequestBody PagedResponseRequest pagedResponseRequest){
        return this.backlogService.getPage(pagedResponseRequest);
    }
    @PostMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestParam String id, @RequestParam String status){
        return this.backlogService.changeStatus(id, status);
    }

    @PostMapping("/link")
    public ResponseEntity<?> link(@RequestParam String id, @RequestParam String link){
        return this.backlogService.link(id, link);
    }
}
