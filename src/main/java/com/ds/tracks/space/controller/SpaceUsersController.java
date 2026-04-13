package com.ds.tracks.space.controller;

import com.ds.tracks.space.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/space/user")
@RequiredArgsConstructor
public class SpaceUsersController {

    private final SpaceService spaceService;

    @PostMapping("/invite")
    public ResponseEntity<?> invite(
            @RequestParam String workSpaceId,
            @RequestParam String spaceId,
            @RequestParam String email,
            HttpServletRequest request
    ){
        return this.spaceService.invite(workSpaceId, spaceId, email, request);
    }
    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(
            @RequestParam String id,
            @RequestParam String space,
            @RequestParam String workspace
    ){
        return this.spaceService.revoke(id, space, workspace);
    }
    @PostMapping("/list")
    public ResponseEntity<?> getUserList(
            @RequestParam("workspace") String workspaceId,
            @RequestParam("space") String spaceId
    ){
        return this.spaceService.getSpaceWiseUsers(spaceId);
    }

    @GetMapping("/names-list")
    public ResponseEntity<?> namesList(@RequestParam(required = false) String id){
        return this.spaceService.findListOfUserFullNames(id);
    }
    @PostMapping("/invite-bulk")
    public ResponseEntity<?> inviteBulk(@RequestParam String space, @RequestBody Map<String, List<String>> body){
        return this.spaceService.inviteBulk(space, body.get("userIds"));
    }

    @PostMapping("/change-role")
    public ResponseEntity<?> changeRole(@RequestParam String role,
                                        @RequestParam String spaceId,
                                        @RequestParam String id){
        return this.spaceService.changeRole(role,spaceId,  id);
    }
    @PostMapping("/permissions")
    public ResponseEntity<?> getUserPermissionList(@RequestParam String space){
        return spaceService.getUserList(space);
    }
}
