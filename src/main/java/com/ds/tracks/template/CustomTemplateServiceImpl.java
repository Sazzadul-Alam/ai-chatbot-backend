package com.ds.tracks.template;


import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.repository.SubSpaceRepository;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.repository.TaskDraftRepository;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomTemplateServiceImpl implements CustomTemplateService {

    private final CustomTemplateRepository templateRepository;
    private final SubSpaceRepository subSpaceRepository;
    private final CustomTemplateTasksRepository templateTaskRepository;
    private final UserService userService;
    private final TaskDraftRepository draftRepository;
    private final SpaceDao spaceDao;
    private final TemplateDao templateDao;
    private final TasksDao tasksDao;



    @Override
    public ResponseEntity<?> getList(PagedResponseRequest request) {
        if(hasPermission()){
            if(Objects.nonNull(request.getCategories()) && !request.getCategories().isEmpty()){
                return new ResponseEntity<>(templateRepository.findAllByCategoryIn(request.getCategories()), HttpStatus.OK);

            } else {
                return new ResponseEntity<>(templateRepository.findAllBasicInfo(), HttpStatus.OK);

            }
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> getTemplate(String id) {
        return new ResponseEntity<>(templateTaskRepository.findAllNameByTemplateId(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> saveTemplate(CustomTemplate template) {
        if(hasPermission()){
            if(!isValidString(template.getId())){
                template.setId(null);
            }
            String id = templateRepository.save(template).getId();
            return new ResponseEntity<>(id,HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> deleteTemplate(String id) {
        if(hasPermission()){
            templateRepository.deleteById(id);
            return new ResponseEntity<>("Template Deleted",HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> getTemplateTask(String id) {
        if(hasPermission()){
            return new ResponseEntity<>(templateTaskRepository.findById(id), HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> saveTemplateTask(CustomTemplateTasks task) {
        if(hasPermission()){
            if(!isValidString(task.getId())){
                task.setId(null);
            }
            String id = templateTaskRepository.save(task).getId();
            return new ResponseEntity<>(id, HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> deleteTemplateTask(String id) {
        if(hasPermission()){
            templateTaskRepository.deleteById(id);
            return new ResponseEntity<>("Template Deleted", HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }

    @Override
    public ResponseEntity<?> clone(List<String> ids, String spaceId, String subspaceId) {
        Integer lastPosition = tasksDao.findLastPosition(subspaceId);
        SubSpace subSpace = subSpaceRepository.findSpaceIdAndWorkspaceId(subspaceId);
        if(Objects.nonNull(subSpace) && userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.TASK_MANAGEMENT), subSpace.getSpaceId())){
            if(Objects.nonNull(ids) && !ids.isEmpty()){
                String userId = userService.getCurrentUserId();
                List<String> stages = spaceDao.getStages(subSpace.getWorkspaceId());
                String status = stages.isEmpty() ? "Todo" : stages.get(0);
                Date now = new Date();

                List<CustomTemplateTasks> templates = templateTaskRepository.findAllByTemplateIdInOrderByPosition(ids);
                List<TaskDraft> drafts = new ArrayList<>();
                int i = 0;
                for(CustomTemplateTasks template : templates){
                    drafts.add(TaskDraft.builder()
                        .spaceId(spaceId)
                        .subSpaceId(subspaceId)
                        .name(template.getName())
                        .description(template.getDescription())
                        .category(template.getCategory())
                        .type(template.getType())
                        .tags(template.getTags())
                        .duration(template.getDuration())
                        .priority(template.getPriority())
                        .storyPoint(template.getStoryPoint())
                        .createdBy(userId)
                        .createdAt(now)
                        .status(status)
                        .position(lastPosition + (Objects.isNull(template.getPosition())? i:template.getPosition()))
                        .build());
                    i++;
                }
                if(!drafts.isEmpty()){
                    draftRepository.saveAll(drafts);
                }
            }
            return new ResponseEntity<>("Tasks Cloned", HttpStatus.OK);
        } else {
            return noPermissionResponse();
        }
    }



    @Override
    public ResponseEntity<?> updateTaskSerial( List<CustomTemplateTasks> tasks) {
        if(Objects.nonNull(tasks) && !tasks.isEmpty()){
            templateDao.updateTaskSerial(tasks);
        }
        return null;
    }

    @Override
    public ResponseEntity<?> findAllTemplatesByCategory(String category) {
        return new ResponseEntity<>(templateDao.findTemplatesByCategory(category), HttpStatus.OK);
    }

    private boolean hasPermission() {
        return true;
    }
    private ResponseEntity<?> noPermissionResponse() {
        return new ResponseEntity<>("You don't have permission to perform this action", HttpStatus.UNAUTHORIZED);
    }
//    @PostConstruct
//    public void migrateTaskTemplates(){
//        log.info("=========== Running Task Template Migration ===========");
//        List<CustomTemplate> templates = templateRepository.findAllId();
//        for(CustomTemplate template : templates){
//            log.info("Migrating {}", template.getName());
//            List<CustomTemplateTasks> tasks = templateTaskRepository.findAllByTemplateId(template.getId());
//            if(Objects.nonNull(tasks)){
//                log.info("Total Tasks {}", tasks.size());
//                for(int i=0; i < tasks.size(); i++){
//                    tasks.get(i).setPosition(i);
//                }
//                templateTaskRepository.saveAll(tasks);
//            }
//            log.info("{} Migration Complete", template.getName());
//        }
//    }
}
