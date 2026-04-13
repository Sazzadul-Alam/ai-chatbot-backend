package com.ds.tracks.backlog;

import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.repository.SubSpaceRepository;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.isValidString;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
public class BacklogServiceImpl implements BacklogService{

    private final BacklogRepository backlogRepository;
    private final UserService userService;
    private final SubSpaceRepository subSpaceRepository;
    private final SpaceDao spaceDao;
    @Override
    public ResponseEntity<?> create(Backlog backlog) {
        IdNameRelationDto currentUser = userService.getCurrentUserFullName();
        if(Objects.nonNull(backlog.getTags()) && !backlog.getTags().isEmpty()){
            spaceDao.updateTag(backlog.getSpaceId(), backlog.getTags());
        }
        backlog.setId(null);
        backlog.setStatus("Todo");
        backlog.setStoryPoint(backlog.getStoryPoint());
        backlog.setCreatedAt(new Date());
        backlog.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        backlog.setGeneratedId(generateBacklogId());
        backlog.setHistory(Arrays.asList(new TasksHistory("Created Backlog", currentUser.getId().toString(), currentUser.getName().toString())));

        backlogRepository.save(backlog);
        return new ResponseEntity<>("Backlog saved",HttpStatus.OK);
    }

    private String generateBacklogId() {
        return new SimpleDateFormat("ddMMyyyy").format(new Date())+"-"+UUID.randomUUID().toString().toUpperCase().substring(0, 5);
    }

    @Override
    public ResponseEntity<?> list(String spaceId, String subSpaceId, Date startDate, Date endDate, String id, List<String> tags) {
        String start = "";
        String end ="";
        if(Objects.nonNull(startDate)){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            start = sdf.format(startDate);
            end = Objects.isNull(endDate) ? start : sdf.format(endDate);
        }
        Map<String, Object> backlogs = backlogRepository.getList(spaceId, subSpaceId, start, end, tags, id);
        if (Objects.isNull(backlogs.get("Todo"))) {
            backlogs.put("Todo", Collections.EMPTY_LIST);
        }
        if (Objects.isNull(backlogs.get("Complete"))) {
            backlogs.put("Complete", Collections.EMPTY_LIST);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("type", isValidString(subSpaceId) ? "subspace" : "space");
        if(Objects.equals(response.get("type"), "space")){
            List<IdNameRelationDto> subspaces = new ArrayList<>();
            subSpaceRepository.findAllBySpaceId(spaceId).forEach(item -> subspaces.add(new IdNameRelationDto(item.getId(), item.getName())));
            response.put("subspaces", subspaces);
        }
        response.put("backlogs", backlogs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> changeStatus(String id, String status) {
        IdNameRelationDto currentUser = userService.getCurrentUserFullName();
        TasksHistory history = new TasksHistory("Changed status to "+status,
                currentUser.getId().toString(), currentUser.getName().toString());
        backlogRepository.changeStatus(id, status, history);
        return new ResponseEntity<>("Status updated",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> link(String id, String subspaceId) {
        Backlog backlog = backlogRepository.findFirstById(id);
        if(Objects.nonNull(backlog)) {
            if(backlogRepository.existsByParentIdAndSubSpaceId(backlog.getId(), subspaceId)){
                return new ResponseEntity<>("Backlog already linked", HttpStatus.ALREADY_REPORTED);
            } else {
                // Creating new backlog for subspace
                IdNameRelationDto currentUser = userService.getCurrentUserFullName();
                backlogRepository.save(Backlog.builder()
                        .name(backlog.getName()).status("Todo")
                        .description(backlog.getDescription())
                        .parentId(backlog.getId())
                        .parentGeneratedId(backlog.getGeneratedId())
                        .createdAt(new Date())
                        .createdBy(SecurityContextHolder.getContext().getAuthentication().getName())
                        .generatedId(generateBacklogId())
                        .tags(backlog.getTags())
                        .spaceId(backlog.getSpaceId())
                        .history(Arrays.asList(new TasksHistory("Linked Backlog #"+backlog.getGeneratedId(),
                                currentUser.getId().toString(), currentUser.getName().toString())))
                        .subSpaceId(subspaceId).build());
                return new ResponseEntity<>("Linked", HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Invalid Backlog", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> update(Backlog backlog) {
        IdNameRelationDto currentUser = userService.getCurrentUserFullName();
        if(Objects.nonNull(backlog.getTags()) && !backlog.getTags().isEmpty()){
            spaceDao.updateTag(backlog.getSpaceId(), backlog.getTags());
        }

        TasksHistory history = new TasksHistory("Updated",
                currentUser.getId().toString(), currentUser.getName().toString());
        backlogRepository.update(backlog.getId(),
                backlog.getName(),
                backlog.getDescription(),
                backlog.getStoryPoint(),
                backlog.getTags(),
                history);
        return new ResponseEntity<>("Updated",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> get(String id) {
        return new ResponseEntity<>(backlogRepository.findFirstById(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getPage(PagedResponseRequest pagedResponseRequest) {
        return new ResponseEntity<>(backlogRepository.getPagedResponse(pagedResponseRequest), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getByIds(List<String> ids) {
        return new ResponseEntity<>(backlogRepository.getByIds(ids), HttpStatus.OK);
    }

    @Override
    public void linkTask(List<String> backlogs, String taskId) {
        backlogRepository.linkTask(backlogs, taskId);

    }

    @Override
    public void unlinkTask(List<String> backlogs, String taskId) {
        backlogRepository.unlinkTask(backlogs, taskId);
    }
}
