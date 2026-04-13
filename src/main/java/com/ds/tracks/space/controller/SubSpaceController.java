package com.ds.tracks.space.controller;

import com.ds.tracks.space.SpaceService;
import com.ds.tracks.space.model.SubSpace;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/space/subspace")
@RequiredArgsConstructor
public class SubSpaceController {

    private final SpaceService spaceService;

    @PostMapping("/get")
    public ResponseEntity<?> get(@RequestParam String id){
        return spaceService.get(null, id);
    }
    @PostMapping("/save")
    public ResponseEntity<?> createSubSpace(@RequestBody SubSpace subSpace, HttpServletRequest request){
        return this.spaceService.saveSubspace(subSpace,request);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> deleteSegment(@RequestParam String id){
        return spaceService.deleteSegment(id);
    }
    @PostMapping("/list")
    public ResponseEntity<?> segments(@RequestParam("id") String spaceId){
        return spaceService.segments(spaceId);
    }
    @PostMapping("/list/ungrouped")
    public ResponseEntity<?> subSpaceWithoutFolder(@RequestParam("id") String spaceId, @RequestParam(required = false) String folderId){
        return spaceService.subSpaceWithoutFolder(spaceId, folderId);
    }
}
