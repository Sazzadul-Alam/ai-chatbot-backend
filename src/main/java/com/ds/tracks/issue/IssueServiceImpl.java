package com.ds.tracks.issue;

import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.KeyValuePair;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.space.SpaceService;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.ds.tracks.testCase.TestCaseService;
import com.ds.tracks.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.createRelation;
import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {
    private final IssueRepository issueRepository;
    private final TestCaseService testCaseService;
    private final SpaceService spaceService;
    private final UserService userService;

    @Override
    public ResponseEntity<?> getAll(String workspaceId, String spaceId, String subspaceId, Date startDate, Date endDate, String severity, List<String> tag, String id) {
        try{
            String start = "";
            String end ="";
            if(Objects.nonNull(startDate)){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                start = sdf.format(startDate);
                end = Objects.isNull(endDate) ? start : sdf.format(endDate);
            }
            Map<String, Object> response = issueRepository.initial(spaceId, subspaceId, start, end, severity, tag, id);
            if(!response.containsKey("Open")){
                response.put("Open", Collections.EMPTY_LIST);
            }
            if(!response.containsKey("Re-Open")){
                response.put("Re-Open", Collections.EMPTY_LIST);
            }
            if(!response.containsKey("In-Progress")){
                response.put("In-Progress", Collections.EMPTY_LIST);
            }
            if(!response.containsKey("Resolved")){
                response.put("Resolved", Collections.EMPTY_LIST);
            }
            if(!response.containsKey("Closed")){
                response.put("Closed", Collections.EMPTY_LIST);
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        }  catch (Exception e){
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> save(Issue issue) {
        try {
            IdNameRelationDto currentUser = userService.getCurrentUserFullName();
            if(isValidString(issue.getId())){
                unlinkTestCases(issue.getId(), issue.getTestCases());
                List<KeyValuePair> sets = new ArrayList<>();
                sets.add(createRelation("name",issue.getName()));
                sets.add(createRelation("tag",issue.getTag()));
                sets.add(createRelation("tags",issue.getTags()));
                sets.add(createRelation("severity",issue.getSeverity()));
                sets.add(createRelation("preConditions",issue.getPreConditions()));
                sets.add(createRelation("testData",issue.getTestData()));
                sets.add(createRelation("testSteps",issue.getTestSteps()));
                sets.add(createRelation("expectedResult",issue.getExpectedResult()));
                sets.add(createRelation("actualResult",issue.getActualResult()));
                if(Objects.nonNull(issue.getTestCases()) && !issue.getTestCases().isEmpty()){
                    sets.add(createRelation("testCases",issue.getTestCases()));
                    testCaseService.link(issue.getTestCases(), issue.getId(), "issues");
                }
                List<KeyValuePair> pushes = new ArrayList<>();
                pushes.add(createRelation("history",  new TasksHistory("Updated Issue Details", currentUser.getId().toString(), currentUser.getName().toString())));
                UpdateResult updateResult = issueRepository.update(issue.getId(), sets, pushes, CollectionName.issues);
                if(updateResult.getMatchedCount() == 0){
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                issue.setId(null);
                issue.setHistory(Arrays.asList(new TasksHistory("Raised Issue", currentUser.getId().toString(), currentUser.getName().toString())));
                issue.setCreatedAt(new Date());
                issue.setComments(new ArrayList<>());
                issue.setCreatedBy(currentUser.getId().toString());
                issueRepository.save(issue);
            }
            if(Objects.nonNull(issue.getTags()) && !issue.getTags().isEmpty()){
                spaceService.saveTags(issue.getSpaceId(), issue.getTags());
            }
            return new ResponseEntity<>("Issue Recorded",HttpStatus.OK);
        } catch (Exception e){
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void unlinkTestCases(String id, List<String> testCases) {
        Issue issue = issueRepository.findTestCasesById(id);
        if(Objects.nonNull(issue) && Objects.nonNull(issue.getTestCases()) && !issue.getTestCases().isEmpty()){
            issue.getTestCases().removeAll(testCases);
            if(!issue.getTestCases().isEmpty()){
                testCaseService.unlink(issue.getTestCases(), id, "issues");
            }
        }
    }

    @Override
    public ResponseEntity<?> changeStatus(String id, String status) {
        try{
            IdNameRelationDto currentUser = userService.getCurrentUserFullName();
            if(isValidString(id) && isValidString(status)){
                List<KeyValuePair> updates = new ArrayList<>();
                if(Arrays.asList("In-Progress", "Resolved").contains(status)){
                    updates.add(createRelation("trackedBy", currentUser.getId()));
                    updates.add(createRelation("trackedAt", new Date()));

                }
                updates.add(createRelation("status", status));
                TasksHistory history = new TasksHistory("Changed status to "+status,
                        currentUser.getId().toString(), currentUser.getName().toString());
                UpdateResult updateResult = issueRepository.update(id, updates, Arrays.asList(new KeyValuePair("history", history)), CollectionName.issues);
                if(updateResult.getMatchedCount() > 0){
                    return new ResponseEntity<>(history, HttpStatus.OK);
                }
            }
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<?> getComments(String id) {
        List<Map<String, Object>> comments = issueRepository.findCommentsById(id, userService.getCurrentUserId());
        return new ResponseEntity<>(Objects.isNull(comments) ? Collections.EMPTY_LIST : comments, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getList(PagedResponseRequest responseRequest) {
        return new ResponseEntity<>(issueRepository.getPagedResponse(responseRequest), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getIssues(List<String> ids) {
        return new ResponseEntity<>(issueRepository.getIssues(ids), HttpStatus.OK);
    }

    @Override
    public void linkTask(List<String> issues, String taskId) {
        issueRepository.linkTask(issues, taskId);
    }

    @Override
    public void unlinkTask(List<String> issues, String taskId) {
        issueRepository.unlinkTask(issues, taskId);

    }

    @Override
    public ResponseEntity<?> get(String id) {
        Issue issue = issueRepository.findById(id).orElse(null);
        if(Objects.isNull(issue)){

            return ResponseEntity.badRequest().body("Issue not found");
        }
        if(Objects.equals(userService.getCurrentUserId(), issue.getCreatedBy())){
            ObjectMapper oMapper = new ObjectMapper();
            Map<String, Object> map = oMapper.convertValue(issue, Map.class);
            map.put("canEdit", true);
            return ResponseEntity.ok(map);
        }
        return ResponseEntity.ok(issue);
    }


}
