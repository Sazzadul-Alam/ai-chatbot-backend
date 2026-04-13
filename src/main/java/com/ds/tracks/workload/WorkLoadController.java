package com.ds.tracks.workload;
import com.ds.tracks.user.model.dto.UserPermissionDto;
import com.ds.tracks.user.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Date;


@RestController
@RequestMapping("/workload")
@RequiredArgsConstructor
public class WorkLoadController {
    private final WorkLoadService workLoadService;

    private final UserPermissionService userPermissionService;

//    @PostMapping("/update/capacity")
//    public ResponseEntity<?> activeUserList(@RequestBody UserPermissionDto userPermissionDto){
//        return userPermissionService.updateCapacity(userPermissionDto);
//    }

    @PostMapping("/data")
    public ResponseEntity<?> getWorkload(@RequestParam(required = false) String workspaceId,
                                         @RequestParam(required = false) String spaceId,
                                         @RequestParam(required = false) String subspaceId,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) throws ParseException {
        return this.workLoadService.getWorkload(workspaceId, spaceId,subspaceId,startDate,endDate);
    }
    @PostMapping("/reassign")
    public ResponseEntity<?> reassign(@RequestParam String id, @RequestParam String user) {
        return this.workLoadService.reassign(id, user);
    }
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestParam String id, @RequestParam Date date) {
        return this.workLoadService.transfer(id, date);
    }
    @PostMapping("/tasks-for-date")
    public ResponseEntity<?> findByDate(@RequestParam String assignedTo,
                                        @RequestParam(required = false) String workspaceId,
                                        @RequestParam(required = false) String spaceId,
                                        @RequestParam(required = false) String subspaceId,
                                        @RequestParam Date date) throws ParseException {
        return workLoadService.findByDate(date, assignedTo, workspaceId, spaceId, subspaceId);
    }
}
