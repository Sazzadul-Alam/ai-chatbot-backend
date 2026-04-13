package com.ds.tracks.space.controller;
import com.ds.tracks.holiday.model.HolidayDto;
import com.ds.tracks.holiday.service.HolidayService;
import com.ds.tracks.space.SpaceService;
import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.space.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


@RestController
@RequestMapping("/space")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final HolidayService holidayService;

    @PostMapping("/get")
    public ResponseEntity<?> project(@RequestParam String id){
        return spaceService.project(id);
    }

    @PostMapping("/get/details")
    public ResponseEntity<?> getDetails(@RequestParam String id){
        return spaceService.get(id, null);
    }

    @PostMapping("/get/image")
    public ResponseEntity<?> getImage(@RequestParam String id){
        return spaceService.getImage(id);
    }

    @PostMapping("/get/list")
    public ResponseEntity<?> getList(@RequestParam(required = false) String id){
        return spaceService.projects(id);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody SpaceDto space, HttpServletRequest request){
        return this.spaceService.create(space, request);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody SpaceDto space, HttpServletRequest request){
        return spaceService.update(space, request);
    }

    @PostMapping("/update/type")
    public ResponseEntity<?> updateType(@RequestParam String id, @RequestParam String type){
        return spaceService.updateType(id, type);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteSpace( @RequestParam String id){
        return spaceService.deleteSpace(id);
    }

    // TODO
    @PostMapping("/initializeCategory")
    public ResponseEntity<?> initializeCategory(@RequestBody SpaceCategoryDto spaceCategoryDto){
        return spaceService.initializeCategory(spaceCategoryDto);
    }

    // Workspace Related Information
    @GetMapping("/tags/get/{workspaceId}")
    public ResponseEntity<?> getTags( @PathVariable String workspaceId){
        return this.spaceService.getTags(workspaceId);
    }
    @PostMapping("/tags/save")
    public ResponseEntity<?> saveTag(@RequestParam("id") String workspaceId, @RequestParam String tag){
        return this.spaceService.saveTag(workspaceId, tag);
    }
    @PostMapping("/tags/remove")
    public ResponseEntity<?> removeTag(@RequestParam("id") String workspaceId, @RequestParam String tag){
        return this.spaceService.removeTag(workspaceId, tag);
    }

    @PostMapping("/configure-weekend")
    public ResponseEntity<?> configureWeekend(@RequestBody WeekendConfigDto configDto){
        return spaceService.configureWeekends(configDto);
    }
    @PostMapping("/configure-work-hour")
    public ResponseEntity<?> configureWorkHour(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String spaceId,
            @RequestParam(required = false) String source,
            @RequestParam Double duration){
        return spaceService.configureWorkHour(workspaceId, spaceId, source, duration);
    }

    @GetMapping("/user-details")
    public ResponseEntity<?> userDetails(){
        return spaceService.userDetails();
    }

    //    Workspace Related Information - End
    @PostMapping("/get-holiday-list")
    public ResponseEntity<?> getHolidayList(@RequestParam("id") String spaceId){
        return holidayService.getHolidayList(spaceId);
    }
    @PostMapping("/save-holiday")
    public ResponseEntity<String> saveHoliday(@RequestBody HolidayDto holiday){
        return holidayService.save(holiday);
    }
    @PostMapping("/upload-holiday")
    public ResponseEntity<?> uploadHoliday(@RequestParam MultipartFile file, @RequestParam(required = false) String source,
                                           @RequestParam(required = false) String spaceId, @RequestParam(required = false) String workspaceId
    ){
        return holidayService.upload(file, source, spaceId, workspaceId);
    }
    @PostMapping("/delete-holiday")
    public ResponseEntity<String> deleteHoliday(@RequestBody HolidayDto holiday){
        return holidayService.delete(holiday);
    }

}
