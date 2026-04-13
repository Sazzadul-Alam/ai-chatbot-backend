package com.ds.tracks.tasks.service;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.backlog.BacklogService;
import com.ds.tracks.commons.models.KeyValuePair;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.issue.IssueService;
import com.ds.tracks.notification.NotificationService;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.tasks.model.dto.TaskPositionDto;
import com.ds.tracks.tasks.repository.*;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.model.SpaceConfigurations;
import com.ds.tracks.template.CustomTemplateRepository;
import com.ds.tracks.space.repository.SpaceRepository;
import com.ds.tracks.space.SpaceService;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.tasks.model.*;
import com.ds.tracks.tasks.model.dto.TasksDto;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.ds.tracks.testCase.TestCaseService;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.models.strings.TasksStatus.CHANGE_STATUS;
import static com.ds.tracks.commons.models.strings.TasksStatus.TASK_CREATED;
import static com.ds.tracks.commons.utils.Utils.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class TasksServiceImpl implements TasksService {
    private final TasksRepository tasksRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TasksDao tasksDao;
    private final SpaceDao spaceDao;
    private final TaskScheduleRepository taskScheduleRepository;
    private final UserService userService;
    private final SpaceService spaceService;
    private final IssueService issueService;
    private final BacklogService backlogService;
    private final NotificationService notificationService;
    private final SpaceRepository spaceRepository;
    private final TaskDraftRepository draftRepository;
    private final TestCaseService testCaseService;
    private final CustomTemplateRepository templateRepository;
    private final AuditLogService auditLogService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


    @Override
    @Transactional
    public ResponseEntity<String> createTask(TasksDto tasksDto, HttpServletRequest request) {
        Integer position = null;

        if(isValidString(tasksDto.getDraftId())){
            TaskDraft draft = draftRepository.findPosition(tasksDto.getDraftId());
            position = draft.getPosition();
            draftRepository.deleteById(tasksDto.getDraftId());
        }
        if(Objects.isNull(position)){
            position = findLastPosition(tasksDto.getSubSpaceId());
        }
        if(Objects.equals(tasksDto.getApp(), true)){
            if(isValidString(tasksDto.getDescription())){
                String description = tasksDto.getDescription();
                description = description.replaceAll("\n", "<br>");
                description = "<div>"+description+"</div>";
                tasksDto.setDescription(description);
            }
        }
        Space space = spaceRepository.findFirstConfigurationsAndMnemonicById(tasksDto.getSpaceId());
        if(Objects.isNull(space)){
            return new ResponseEntity<>("Invalid Space Id",HttpStatus.BAD_REQUEST);
        }
        Workspace workspace = workspaceRepository.findFirstConfiguration(space.getWorkspaceId());
        if(Objects.isNull(tasksDto.getDeadline())){
            tasksDto.setDeadline(tasksDto.getStartDate());
        }
        IdNameRelationDto currentUser = userService.getCurrentUserFullName();
        tasksDto.getStartDate().setHours(6);
        tasksDto.getDeadline().setHours(6);
        Tasks task = Tasks.builder()
                .name(tasksDto.getName())
                .spaceId(tasksDto.getSpaceId())
                .subSpaceId(tasksDto.getSubSpaceId())
                .assignedUsers(tasksDto.getAssignedUsers())
                .type(tasksDto.getType())
                .storyPoint(tasksDto.getStoryPoint())
                .category(tasksDto.getCategory())
                .backlogs(tasksDto.getBacklogs())
                .tasks(tasksDto.getTasks())
                .description(tasksDto.getDescription())
                .priority(Objects.nonNull(tasksDto.getPriority()) ? tasksDto.getPriority() : 4)
                .severity(tasksDto.getSeverity())
                .deadline(tasksDto.getDeadline())
                .duration(tasksDto.getDuration())
                .issuedTo(tasksDto.getIssuedTo())
                .issuedBy(currentUser.getId().toString())
                .issueDate(new Date())
                .startDate(tasksDto.getStartDate())
                .deadline(tasksDto.getDeadline())
                .completion(0D)
                .position(position)
                .tags(tasksDto.getTags())
                .issues(tasksDto.getIssues())
                .testCases(tasksDto.getTestCases())
                .actualDuration(0D)
                .overtime(tasksDto.getOvertime())
                .generatedId(generateTaskId(space.getMnemonic(), tasksDto.getType()))
                .history(Arrays.asList(new TasksHistory(TASK_CREATED, currentUser.getId().toString(), currentUser.getName().toString())))
                .status(tasksDto.getStatus()).build();
        tasksRepository.save(task);
        linkToTasks(tasksDto.getTasks(), task.getId());
        linkToIssues(tasksDto.getIssues(), task.getId());
        linkTestCases(tasksDto.getTestCases(), task.getId());
        linkToBacklogs(tasksDto.getBacklogs(), task.getId());
        createSchedules(task.getId(),
                task.getSpaceId(),
                task.getSubSpaceId(),
                tasksDto.getStartDate(),
                tasksDto.getDeadline(),
                task.getDuration(),
                task.getAssignedUsers(),
                workspace.getConfigurations(),
                Objects.equals(tasksDto.getOvertime(), Boolean.TRUE)
        );
        if(isValidString(tasksDto.getDraftId())){
            auditLogService.save( "Published a task",  CollectionName.task, task.getId(), task.getSpaceId(), task.getSubSpaceId());
            auditLogService.migrateDraftToPublishedTask(tasksDto.getDraftId(), task.getId());
        } else {
            auditLogService.save( "Created a Task",  CollectionName.task, task.getId(), task.getSpaceId(), task.getSubSpaceId());
        }
        notificationService.sendNotifications(task.getAssignedUsers(), "Task", task.getId(), tasksDto.getWorkspaceId(), task.getSpaceId(), task.getName());
        return new ResponseEntity<>("Task Created", HttpStatus.OK);
    }

    private Integer findLastPosition(String subSpaceId) {
        return tasksDao.findLastPosition(subSpaceId);
    }

    private void linkTestCases(List<String> testCases, String id) {
        testCaseService.link(testCases, id, "tasks");
    }

    @Override
    public ResponseEntity<String> saveDraft(TaskDraft draft, HttpServletRequest request) {
        String action = "Updated Draft Task";
        if(!isValidString(draft.getId())){
            draft.setId(null);
            action = "Saved Draft Task";
        }else{
            TaskDraft saved = draftRepository.findFirstById(draft.getId());
            draft.setPosition(saved.getPosition());
        }
        draft.setCreatedAt(new Date());
        draft.setCreatedBy(userService.getCurrentUserId());
        if(Objects.isNull(draft.getPosition())){
            draft.setPosition(findLastPosition(draft.getSubSpaceId()));
        }
        if(Objects.nonNull(draft.getStartDate())){
            draft.getStartDate().setHours(6);
        }
        if(Objects.nonNull(draft.getDeadline())){
            draft.getDeadline().setHours(6);
        }
        draft = draftRepository.save(draft);
        auditLogService.save( action,  CollectionName.tasks_draft, draft.getId(), draft.getSpaceId(), draft.getSubSpaceId());
        return new ResponseEntity<>(draft.getId(), HttpStatus.OK);
    }


    private void linkToIssues(List<String> issues, String id) {
        issueService.linkTask(issues, id);
    }

    private void linkToBacklogs(List<String> backlogs, String id) {
        backlogService.linkTask(backlogs, id);
    }

    private void linkToTasks(List<String> tasks, String id) {
        tasksDao.link(tasks, id);
    }


    @Override
    public ResponseEntity<?> getTask(String id) {
        Map<String,Object> response = tasksDao.findTaskById(id);
        if(Objects.nonNull(response)){
            response.put("dependencies", dependencyRepository.findAllLinkedTaskIds(id));
            String currentUser = userService.getCurrentUserId();
            if(Objects.equals(response.get("issuedBy"), currentUser)){
                response.put("canEdit", true);
                response.put("canDelete", true);
            } else {
                response.put("canEdit", userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT), response.get("spaceId").toString()));
                response.put("canDelete", userService.checkIfCurrentUserIsSpecialUser());
            }
            List<Map<String, String>> assigned = tasksDao.getAllAssignedUsers(id);
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            for(Map<String, String> map : assigned){
                if(Objects.equals(map.get("email"), email)){
                    response.put("canLogEffort", true);
                    break;
                }
            }
            response.put("assigned", assigned);
            response.put("location", tasksDao.findTaskLocationById(id));
        }
        auditLogService.save("Viewed Task", CollectionName.task, id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getList(String spaceId, String subspaceId, Date date, String priority, Boolean showAll) {
        String taskDate = dateFormat.format(date);
        auditLogService.save("Viewed Task List", CollectionName.sub_spaces, subspaceId, spaceId, subspaceId);
        return new ResponseEntity<>(tasksDao.getAllTaskByDate(spaceId, subspaceId, taskDate, priority, Objects.equals(showAll, true) ? null : userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> changeStatus(String id, String status, HttpServletRequest request) {
        List<Document> dependencies = new ArrayList<>();
        if(Objects.equals("Completed", status)){
            dependencies =dependencyRepository.canCloseTask(id);

        }
        if(dependencies.isEmpty()){
            IdNameRelationDto user =userService.getCurrentUserFullName();
            UpdateResult res = tasksDao.update(id, "status", status, new TasksHistory(CHANGE_STATUS(status), user.getId().toString(), user.getName().toString()));
            if (res.wasAcknowledged()) {
                auditLogService.save( CHANGE_STATUS(status),  CollectionName.task, id);
                return ResponseEntity.ok(new Document("status", "Updated"));
            }
        } else {
            return ResponseEntity.ok(new Document("status", "Hold").append("dependencies", dependencies));
        }
        return ResponseEntity.badRequest().body("Task not fount");
    }


    @Override
    public ResponseEntity<?> getAllIssueGroupedByStatus(String workspaceId, String spaceId, String subSpaceId, Boolean showAll, Date start, Date end, String tracker, String priority, List<String> tags, String viewType) {
        if(Objects.isNull(start)){
            start = new Date();
        }
        if(Objects.isNull(end)){
            end = start;
        }
        showAll = Objects.equals(showAll, Boolean.TRUE);
        boolean isAdmin = userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT), spaceId);
        if(showAll && !isAdmin){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = sdf.format(start);
        String endDate = sdf.format(end);

        String userId = userService.getCurrentUserId();
        List<String> stages = spaceDao.getStages(workspaceId);
        Map<String, Object> response = tasksDao.getGroupedTasks(spaceId,subSpaceId, Objects.equals(showAll, true) ? null : userId, startDate, endDate, tracker, priority, tags);
        Map<String, List> groupedList = Objects.isNull(response.get("data"))
                ? Collections.emptyMap() : (Map<String, List>) response.get("data");
        List<Document> sortedGroupedList = new ArrayList<>();
        stages.forEach(st->
                sortedGroupedList.add(
                        new Document("title", st).append("tasks", groupedList.getOrDefault(st, Collections.emptyList()))));
        response.put("data",sortedGroupedList);
        response.put("user",userId);
        if(isAdmin){
            response.put("su", spaceId);
        }
        if(isValidString(subSpaceId)){

            auditLogService.save("Viewed Task "+viewType, CollectionName.sub_spaces, subSpaceId, spaceId, subSpaceId);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private int indexOfTask(List<Map<String, Object>> list, String status,Integer size) {
        for (int i = 0; i < size; i++)
            if (list.get(i).get("title").equals(status))
                return i;
        return -1;
    }

    @Override
    public ResponseEntity<?> generateCalender(Date startDate, Date endDate, String workspaceId, String spaceId, String subspaceId, Boolean showAll) {
       try {
           boolean isAdmin = userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT), spaceId);
           showAll = Objects.equals(showAll, Boolean.TRUE);
           if(showAll && !isAdmin){
               return forbidden();
           }
           SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
           String userId = null;
           if(!showAll){
               userId = userService.getCurrentUserId();
           }
           Map<String, List<Object>> data = tasksDao.findTasksForCalenderView(
                   df.format(startDate), df.format(endDate), spaceId, subspaceId, userId);
           Map<String, Object> result = new HashMap<>();
           for(String key : data.keySet()){
               Map<Object, Integer> counts = new HashMap<>();
               for(Object item : data.get(key)){
                   if(!counts.containsKey(item)){
                       counts.put(item, 1);
                   } else {
                       counts.put(item, counts.get(item)+1);
                   }
               }
               result.put(key, counts);
           }
           Map<String, Object> response = spaceService.getHolidaysAndWeekend(workspaceId);
           response.put("data", result);
           if(isAdmin){
               response.put("su", spaceId);
           }
           auditLogService.save("Viewed Task Calender", CollectionName.sub_spaces, subspaceId, spaceId, subspaceId);
           return new ResponseEntity<>(response, HttpStatus.OK);
       } catch (Exception ex){
           log.error(ex.getMessage(), ex.getCause());
       }
       return badRequest();
    }

    @Override
    public ResponseEntity<?> lock(String id, String type, String spaceId, String subspaceId) {
        IdNameRelationDto user = userService.getCurrentUserFullName();
        if(Objects.nonNull(user) && isValidString(id)) {
            String finalStage = spaceService.getFinalStage(spaceId, subspaceId);
            List<IdNameRelationDto> updates = new ArrayList<>();
            updates.add(new IdNameRelationDto("status", finalStage));
            updates.add(new IdNameRelationDto("locked", true));
            TasksHistory history = new TasksHistory("Locked Task", user.getId().toString(), user.getName().toString());
            tasksDao.update( type,id, updates, history );
            Map<String, Object> response =new HashMap<>();
            response.put("history", history);
            response.put("status", finalStage);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> getAllTasks(String workspaceId, Date startDate, Date endDate) {
        String userId =userService.getCurrentUserId();
        return new ResponseEntity<>(tasksDao.getAllAssignedTasks(userId, dateFormat.format(startDate), dateFormat.format(endDate)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getPaged(PagedResponseRequest responseRequest) {
        return new ResponseEntity<>(tasksDao.getPagedResponse(responseRequest), HttpStatus.OK);
    }
    @Override
    public ResponseEntity<?> getTemplate(String id) {
        return new ResponseEntity<>(templateRepository.findById(id), HttpStatus.OK);
    }
    @Override
    public ResponseEntity<?> getTemplates() {
        return new ResponseEntity<>(templateRepository.findAll(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getByIds(List<String> ids) {
        return new ResponseEntity<>(tasksDao.getByIds(ids), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> updateLinks(String type, String id, List<String> list) {
        if(isValidString(id) && Arrays.asList("tasks", "backlogs", "issues", "testCases").contains(type)){
            List<String> existingIds = tasksDao.getLinks(type, id);
            if(Objects.nonNull(existingIds)){
                tasksRepository.update(id, Arrays.asList(new KeyValuePair(type, list)), null, CollectionName.task);
                List<String> needToPull = new ArrayList<>();
                if(Objects.isNull(list) || list.isEmpty()){
                    needToPull = existingIds;
                } else {
                    for(String existingId : existingIds){
                        if(!list.contains(existingId)){
                            needToPull.add(existingId);
                        } else {
                            list.remove(existingId);
                        }
                    }
                }
                if(Objects.equals(type, "tasks")){
                    if(!list.isEmpty()){
                        tasksDao.link(list, id);
                    }
                    if(!needToPull.isEmpty()){
                        tasksDao.unlink(needToPull, id);
                    }
                } else if (Objects.equals(type, "backlogs")){
                    if(!list.isEmpty()){
                        backlogService.linkTask(list, id);
                    }
                    if(!needToPull.isEmpty()){
                        backlogService.unlinkTask(needToPull, id);
                    }
                } else if (Objects.equals(type, "issues")){
                    if(!list.isEmpty()){
                        issueService.linkTask(list, id);
                    }
                    if(!needToPull.isEmpty()){
                        issueService.unlinkTask(needToPull, id);
                    }
                } else if (Objects.equals(type, "testCases")){
                    if(!list.isEmpty()){
                        linkTestCases(list, id);
                    }
                    if(!needToPull.isEmpty()){
                        testCaseService.unlink(needToPull, id, "tasks");
                    }
                }
                return new ResponseEntity<>("Updated", HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<String> updateTask(String id, String type, String value, HttpServletRequest request) {
        if(isValidString(id) && isValidString(type) && isValidString(value) && Arrays.asList("name", "description", "type", "category", "storyPoint", "priority").contains(type)){
            IdNameRelationDto user = userService.getCurrentUserFullName();
            UpdateResult updateResult = tasksRepository.update(id, Arrays.asList(new KeyValuePair(type, value)),
                    Arrays.asList(new KeyValuePair("history",
                    new TasksHistory("Updated task "+type, user.getId().toString(), user.getName().toString()))), CollectionName.task);
            if(updateResult.getMatchedCount() > 0){

                auditLogService.save( "Updated task "+type,  CollectionName.task, id);
                return ResponseEntity.ok("Updated");
            }
        }
        return ResponseEntity.badRequest().body("Invalid Params");
    }


    @Override
    public ResponseEntity<?> getDraft(String id) {
        Map<String,Object> response = tasksDao.findDraftById(id);
        if(Objects.nonNull(response)){
            String currentUser = userService.getCurrentUserId();
            if(Objects.equals(response.get("issuedBy"), currentUser)){
                response.put("canEdit", true);
                response.put("canDelete", true);
            } else {
                response.put("canEdit", userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT), response.get("spaceId").toString()));
                response.put("canDelete", userService.checkIfCurrentUserIsSpecialUser());
            }
            List<Map<String, String>> assigned = tasksDao.getAllAssignedUsers(id);
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            for(Map<String, String> map : assigned){
                if(Objects.equals(map.get("email"), email)){
                    response.put("canLogEffort", true);
                    break;
                }
            }
            response.put("assigned", assigned);
        }
        auditLogService.save("Viewed Task Draft", CollectionName.tasks_draft, id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getDraftsGrouped(String spaceId, String subSpaceId) {
        return new ResponseEntity<>(tasksDao.getGroupedDrafts(spaceId, subSpaceId, userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteDraft(String id, HttpServletRequest request) {
        TaskDraft draft = draftRepository.findById(id).orElse(null);
        if(Objects.nonNull(draft)){
            auditLogService.logDeletedSource("Deleted Draft Task",  CollectionName.tasks_draft, id, draft.getSpaceId(), draft.getSubSpaceId(), draft.getName());
            draftRepository.deleteById(id);
            return new ResponseEntity<>("Draft Deleted",HttpStatus.OK);
        }
        return new ResponseEntity<>("Draft not found",HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<String> updateTaskAllField(TasksDto tasksDto) {
        try {
            IdNameRelationDto currentUser = userService.getCurrentUserFullName();
            List<KeyValuePair> sets = new ArrayList<>();
            sets.add(createRelation("name", tasksDto.getName()));
            sets.add(createRelation("assignedUsers", tasksDto.getAssignedUsers()));
            sets.add(createRelation("description", tasksDto.getDescription()));
            sets.add(createRelation("priority", Objects.nonNull(tasksDto.getPriority()) ? tasksDto.getPriority() : 4));
            sets.add(createRelation("type", tasksDto.getType()));
            sets.add(createRelation("category", tasksDto.getCategory()));
            sets.add(createRelation("status", tasksDto.getStatus()));
            if (Objects.nonNull(tasksDto.getTags()) && tasksDto.getTags().size() > 0) {
                sets.add(createRelation("tags", tasksDto.getTags()));
            }
            if(Objects.nonNull(tasksDto.getTasks()) && !tasksDto.getTasks().isEmpty()){
                sets.add(createRelation("tasks", tasksDto.getTasks()));
                testCaseService.link(tasksDto.getTasks(), tasksDto.getId(), "tasks");
            }
            if(Objects.nonNull(tasksDto.getIssues()) && !tasksDto.getIssues().isEmpty()){
                sets.add(createRelation("issues", tasksDto.getIssues()));
                testCaseService.link(tasksDto.getIssues(), tasksDto.getId(), "tasks");
            }
            if(Objects.nonNull(tasksDto.getBacklogs()) && !tasksDto.getBacklogs().isEmpty()){
                sets.add(createRelation("backlogs", tasksDto.getBacklogs()));
                testCaseService.link(tasksDto.getBacklogs(), tasksDto.getId(), "tasks");
            }
            if(Objects.nonNull(tasksDto.getTestCases()) && !tasksDto.getTestCases().isEmpty()){
                sets.add(createRelation("testCases", tasksDto.getTestCases()));
                testCaseService.link(tasksDto.getTestCases(), tasksDto.getId(), "tasks");
            }
            List<KeyValuePair> pushes = new ArrayList<>();
            pushes.add(createRelation("history",  new TasksHistory("Updated Task Details", currentUser.getId().toString(), currentUser.getName().toString())));
            UpdateResult updateResult = tasksRepository.update(tasksDto.getId(), sets, pushes, CollectionName.task);
            if(updateResult.getMatchedCount() == 0){
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else {
                notificationService.sendNotifications(tasksDto.getAssignedUsers(), "Task", tasksDto.getId(), tasksDto.getWorkspaceId(), tasksDto.getSpaceId(), tasksDto.getName());
                return new ResponseEntity<>("Task Updated",HttpStatus.OK);
            }
        } catch (Exception e){
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<?> deleteByTaskId(String taskId, HttpServletRequest request) {
        if(isValidString(taskId)){
            Tasks task = tasksRepository.findFirstById(taskId);
            if(Objects.nonNull(task) && userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT),task.getSpaceId())){
                try{
                    auditLogService.logDeletedSource("Deleted Task", CollectionName.task, taskId, task.getSpaceId(), task.getSubSpaceId(), task.getName());
                    tasksDao.deleteByTaskId(taskId);
                    return new ResponseEntity<>("Task Deleted", HttpStatus.OK);
                } catch (Exception e){
                    log.error(e.getMessage(), e);
                    return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return new ResponseEntity<>("You don't have permission to perform this action.", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> updateTags(String id, List<String> tags) {
        tasksDao.update(id,"tags", tags, null);
        return new ResponseEntity<>("ack", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> overall(PagedResponseRequest responseRequest) {
        if(Objects.isNull(responseRequest.getStartDate())){
            responseRequest.setStartDate(new Date());
        }
        if(Objects.isNull(responseRequest.getEndDate())){
            responseRequest.setEndDate(responseRequest.getStartDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = sdf.format(responseRequest.getStartDate());
        String endDate = sdf.format(responseRequest.getEndDate());
        String userId = userService.getCurrentUserId();
        Map<String, Object> response = new HashMap<>();
        List<Space> spaces = Objects.nonNull(responseRequest.getSpaces()) && !responseRequest.getSpaces().isEmpty()
            ? spaceRepository.findAllNameAndColorByWorkspaceIdAndIdIn(responseRequest.getWorkspaceId(), responseRequest.getSpaces())
            : spaceRepository.findAllNameAndColorByWorkspaceId(responseRequest.getWorkspaceId());
        response.put("tasks", tasksDao.getOverallBoard(startDate, endDate, userId, spaces.stream().map(Space::getId).collect(Collectors.toList()), responseRequest));
        response.put("spaces", spaces);
        auditLogService.save("Viewed overall tasks board");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> allUsersInWorkspace(PagedResponseRequest responseRequest) {

        return new ResponseEntity<>(tasksDao.getAllUsers(responseRequest.getWorkspaceId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> link(String taskId, List<String> tasks) {
        List<TaskDependency> dependencies = new ArrayList<>();
        for(String dependentTask : tasks){
            dependencies.add(TaskDependency.builder().taskId(taskId).dependency(dependentTask).build());
        }
        dependencyRepository.deleteAllByTaskId(taskId);
        dependencyRepository.saveAll(dependencies);

        return new ResponseEntity<>("Dependencies Updated",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> reassign(String taskId, List<String> users, HttpServletRequest request) {
        if(Objects.nonNull(users) && !users.isEmpty()){
//            taskScheduleRepository.deleteAllByTaskId(taskId);
            String userId = users.get(0);
            IdNameRelationDto user = userService.getCurrentUserFullName();
            String action = "Task was reassigned to "+userService.getUsernameById(userId);
            TasksHistory history = new TasksHistory(action, user.getId().toString(), user.getName().toString());
            tasksDao.updateSchedules(taskId, userId, history);
            auditLogService.save( action,  CollectionName.task, taskId);
            return new ResponseEntity<>("Reassigned", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Please select atleast one user",HttpStatus.BAD_REQUEST);
        }
    }

    public void reopenTask(String taskId) {
        Tasks task = tasksRepository.findFirstById(taskId);
        IdNameRelationDto currentUser = userService.getCurrentUserFullName();
        TaskDraft draft = TaskDraft.builder()
                .spaceId(task.getSpaceId())
                .subSpaceId(task.getSubSpaceId())
                .name(task.getName())
                .type(task.getType())
                .category(task.getCategory())
                .description(task.getDescription())
                .priority(task.getPriority())
                .severity(task.getSeverity())
                .duration(task.getDuration())
                .storyPoint(task.getStoryPoint())
                .startDate(task.getStartDate())
                .deadline(task.getDeadline())
                .deadline(task.getDeadline())
                .createdBy(currentUser.getId().toString())
                .createdAt(new Date())
                .history(Arrays.asList(new TasksHistory("Task reopened from "+task.getGeneratedId(), currentUser.getId().toString(), currentUser.getName().toString())))
                .backlogs(task.getBacklogs())
                .tags(task.getTags())
                .issues(task.getIssues())
                .tasks(task.getTasks())
                .assignedUsers(task.getAssignedUsers())
                .status("Todo")
                .overtime(task.getOvertime())
                .build();
        draftRepository.save(draft);

    }

    @Override
    public ResponseEntity<?> monitorList(PagedResponseRequest pagedResponseRequest) {
        auditLogService.save("Viewed Task Monitor List");
        return new ResponseEntity<>(tasksDao.getMonitorList(pagedResponseRequest), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> tasksListForReposition(PagedResponseRequest pagedResponseRequest) {
        return new ResponseEntity<>(tasksDao.tasksListForReposition(pagedResponseRequest), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> updatePositions(List<TaskPositionDto> positions, String spaceId, String subspaceId, HttpServletRequest request) {
        tasksDao.updatePositions(positions);
        auditLogService.save("Updated task positions",  CollectionName.spaces, spaceId, spaceId, subspaceId);
        return ResponseEntity.ok(null);
    }


    private void createSchedules(String taskId, String spaceId, String subspaceId, Date startDate, Date endDate, Double duration, List<String> assignedList, SpaceConfigurations configurations, boolean isOvertime){
        List<Integer> weekends = new ArrayList<>();
        List<Date> holidays = new ArrayList<>();
        if(Objects.nonNull(configurations) && !Objects.equals(isOvertime, true)){
            weekends = getWeekends(configurations.getWeekend());
            holidays = getHolidays(configurations.getHoliday());
        }
        startDate.setMinutes(0);
        startDate.setSeconds(0);

        endDate.setMinutes(0);
        endDate.setSeconds(0);
        List<TaskSchedule> schedules = new ArrayList<>();
        List<Date> days = getDaysBetweenDates(startDate, endDate);
        for(Date date : days){
            if(!weekends.contains(getDayNumberOld(date)) && !holidays.contains(date)){
                for(String assignedTo: assignedList){
                    schedules.add(TaskSchedule.builder()
                            .taskId(taskId)
                            .spaceId(spaceId)
                            .subspaceId(subspaceId)
                            .scheduleDate((Date) date.clone())
                            .dateString(dateFormat.format(date))
                            .assignedTo(assignedTo)
                            .build());
                }
            }
        }
        if(schedules.isEmpty()){
            for(String assignedTo: assignedList){
                schedules.add(TaskSchedule.builder()
                        .taskId(taskId)
                        .spaceId(spaceId)
                        .subspaceId(subspaceId)
                        .scheduleDate(startDate)
                        .dateString(dateFormat.format(startDate))
                        .assignedTo(assignedTo)
                        .build());
            }
        }
        Double durationSegment = duration/schedules.size();
        schedules.forEach(s->s.setDuration(durationSegment));

        taskScheduleRepository.saveAll(schedules);
    }
    public static List<Date> getDaysBetweenDates(Date startdate, Date enddate)
    {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startdate);

        while (calendar.getTime().getTime() <= enddate.getTime()) {
            Date result = calendar.getTime();
            dates.add(result);
            calendar.add(Calendar.DATE, 1);
        }
        return dates;
    }

    private List<Integer> getWeekends(List<String> weekdays) {
        List<Integer> weekends = new ArrayList<>();
        if(Objects.nonNull(weekdays)){
            for(String weekday : weekdays){
                switch (weekday) {
                    case "Sunday": weekends.add(1); break;
                    case "Monday":weekends.add(2); break;
                    case "Tuesday":weekends.add(3); break;
                    case "Wednesday":weekends.add(4); break;
                    case "Thursday":weekends.add(5); break;
                    case "Friday":weekends.add(6); break;
                    case "Saturday":weekends.add(7); break;
                    default: break;
                }
            }
        }
        return weekends;
    }
    private List<Date> getHolidays(List<Holiday> holidays) {
        List<Date> holidayDates = new ArrayList<>();
        if(Objects.nonNull(holidays)){
            for(Holiday holiday : holidays){
                holiday.getDate().setHours(6);
                holidayDates.add(holiday.getDate());
            }
        }
        return holidayDates;
    }
    public static int getDayNumberOld(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }
}
