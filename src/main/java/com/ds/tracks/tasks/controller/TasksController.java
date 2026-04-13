package com.ds.tracks.tasks.controller;


import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.model.dto.ReassignDto;
import com.ds.tracks.tasks.model.dto.TaskPositionDto;
import com.ds.tracks.tasks.model.dto.TasksDto;
import com.ds.tracks.tasks.service.TasksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TasksController {
    private final TasksService taskService;

    @PostMapping("/create")
    public ResponseEntity<String> createTask(@RequestBody TasksDto taskDto, HttpServletRequest request){
        return this.taskService.createTask(taskDto, request);
    }

    @PostMapping("/save-draft")
    public ResponseEntity<String> saveDraft(@RequestBody TaskDraft draft, HttpServletRequest request){
        return this.taskService.saveDraft(draft, request);
    }

    @PostMapping("/delete-draft")
    public ResponseEntity<String> deleteDraft(@RequestParam String id, HttpServletRequest request){
        return this.taskService.deleteDraft(id, request);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateTask(@RequestParam String id, @RequestParam String type,@RequestParam String value, HttpServletRequest request){
        return this.taskService.updateTask(id, type, value, request);
    }

    @PostMapping("/update/all/fields")
    public ResponseEntity<String> updateTaskAllField(@RequestBody TasksDto taskDto){
        return this.taskService.updateTaskAllField(taskDto);
    }

    @PostMapping("/paged")
    public ResponseEntity<?> getPaged( @RequestBody PagedResponseRequest responseRequest) {
        return taskService.getPaged(responseRequest);
    }
    @PostMapping("/overall")
    public ResponseEntity<?> overall(@RequestBody PagedResponseRequest responseRequest) {
        return taskService.overall(responseRequest);
    }
    @PostMapping("/allUsers")
    public ResponseEntity<?> allUsersInWorkspace(@RequestBody PagedResponseRequest responseRequest) {
        return taskService.allUsersInWorkspace(responseRequest);
    }
    @PostMapping("/get-by-id")
    public ResponseEntity<?> getByIds(@RequestBody List<String> ids) {
        return taskService.getByIds(ids);
    }


    @PostMapping("/get")
    public ResponseEntity<?> getTask(@RequestParam String id){
        return this.taskService.getTask(id);
    }

    @PostMapping("/get-draft")
    public ResponseEntity<?> getDraft(@RequestParam String id){
        return this.taskService.getDraft(id);
    }

    @PostMapping("/calender")
    public ResponseEntity<?> generateCalender(@RequestParam Date startDate,
                                              @RequestParam Date endDate,
                                              @RequestParam String workspaceId,
                                              @RequestParam(required = false) String spaceId,
                                              @RequestParam(required = false) String subspaceId,
                                              @RequestParam(required = false) Boolean showAll){
        return this.taskService.generateCalender(startDate, endDate,workspaceId, spaceId, subspaceId, showAll);
    }

    @PostMapping("/list")
    public ResponseEntity<?> getList(@RequestParam Date date,
                                     @RequestParam String spaceId,
                                     @RequestParam(required = false) String subspaceId,
                                     @RequestParam(required = false) Boolean showAll,
                                     @RequestParam(required = false) String priority){
        return this.taskService.getList(spaceId, subspaceId, date, priority, showAll);
    }


    @PostMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestParam String id, @RequestParam String status, HttpServletRequest request){
        return this.taskService.changeStatus(id, status, request);
    }

    @PostMapping("/{viewType}/view")
    public ResponseEntity<?> viewTasksGrouped(@PathVariable String viewType,
                                               @RequestParam String spaceId,
                                               @RequestParam(required = false) String subSpaceId,
                                               @RequestParam(required = false) Date startDate,
                                               @RequestParam(required = false) String workspaceId,
                                               @RequestParam(required = false) Date endDate,
                                               @RequestParam(required = false) String tracker,
                                               @RequestParam(required = false) String priority,
                                               @RequestParam(required = false) List<String> tags,
                                               @RequestParam(required = false) Boolean showAll){
        return this.taskService.getAllIssueGroupedByStatus(workspaceId, spaceId, subSpaceId, showAll, startDate, endDate, tracker, priority, tags, viewType);
    }

    @PostMapping("/drafts-grouped")
    public ResponseEntity<?> getDraftsGrouped(@RequestParam String spaceId, @RequestParam(required = false) String subSpaceId){
        return this.taskService.getDraftsGrouped(spaceId, subSpaceId);
    }

    @PostMapping("/lock")
    public ResponseEntity<?> lock(@RequestParam String id,
                                  @RequestParam String type,
                                  @RequestParam String spaceId,
                                  @RequestParam(required = false) String subspaceId){
        return this.taskService.lock(id,type, spaceId, subspaceId);
    }

    @PostMapping("/update/links")
    public ResponseEntity<?> updateLinks(@RequestParam String type, @RequestParam String id, @RequestParam List<String> list){
        return taskService.updateLinks(type, id, list);
    }
    @PostMapping("/update/tags/{id}")
    public ResponseEntity<?> updateTags(@PathVariable String id, @RequestBody List<String> tags){
        return taskService.updateTags(id, tags);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String id, HttpServletRequest request){
        return taskService.deleteByTaskId(id, request);
    }


    @PostMapping("/link/{taskId}")
    public ResponseEntity<?> link(@PathVariable String taskId, @RequestBody List<String> tasks){
        return taskService.link(taskId, tasks);
    }

    @PostMapping("/{taskId}/reassign")
    public ResponseEntity<?> reassign(@PathVariable String taskId,  @RequestBody List<String> users, HttpServletRequest request){
        return taskService.reassign(taskId, users, request);
    }


    @PostMapping("/monitor-list")
    public ResponseEntity<?> monitorList(@RequestBody PagedResponseRequest pagedResponseRequest){
        return taskService.monitorList(pagedResponseRequest);
    }

    @PostMapping("/reposition-list")
    public ResponseEntity<?> tasksListForReposition(@RequestBody PagedResponseRequest pagedResponseRequest){
        return taskService.tasksListForReposition(pagedResponseRequest);
    }

    @PostMapping("/{spaceId}/{subspaceId}/update-positions")
    public ResponseEntity<?> updatePositions(@PathVariable String spaceId, @PathVariable String subspaceId, @RequestBody List<TaskPositionDto> positions, HttpServletRequest request){
        return taskService.updatePositions(positions, spaceId, subspaceId, request);
    }



}
