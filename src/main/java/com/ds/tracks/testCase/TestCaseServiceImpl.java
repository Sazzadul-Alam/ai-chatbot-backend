package com.ds.tracks.testCase;


import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.space.SpaceService;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.ds.tracks.tasks.model.dto.TasksVersion;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService{

    private final TestCaseRepository testCaseRepository;
    private final SpaceDao spaceDao;
    private final SpaceService spaceService;
    private final UserService userService;

    @Override
    public ResponseEntity<?> findById(String id) {
        Optional<TestCase> testCase = testCaseRepository.findById(id);
        if(testCase.isPresent()){
            return new ResponseEntity<>(testCase.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Test Case not found", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> getPage(PagedResponseRequest responseRequest) {
        if(isValidString(responseRequest.getSpaceId())){
            try{
                if(Objects.isNull(responseRequest.getSize())){
                    responseRequest.setSize(30);
                }
                if(Objects.isNull(responseRequest.getPage())){
                    responseRequest.setPage(0);
                }
                if(Objects.isNull(responseRequest.getSortBy())){
                    responseRequest.setSortBy("_id");
                }
                String startDate = "";
                String endDate ="";
                if(Objects.nonNull(responseRequest.getStartDate())){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    startDate = sdf.format(responseRequest.getStartDate());
                    endDate = Objects.isNull(responseRequest.getEndDate()) ? startDate : sdf.format(responseRequest.getEndDate());
                }
                List<String> subspaces = new ArrayList<>();
                if(isValidString(responseRequest.getSubSpaceId())){
                    String folderId = spaceDao.getFolderId(responseRequest.getSubSpaceId());
                    if(isValidString(folderId)){
                        subspaces.addAll(spaceDao.getSubspacesByFolderId(folderId));
                    } else {
                        subspaces.add(responseRequest.getSubSpaceId());
                    }
                }
                return new ResponseEntity<>(testCaseRepository.getPage(
                        responseRequest.getSpaceId(), subspaces, startDate, endDate,
                        responseRequest.getTags(), responseRequest.getStatus(),
                        Objects.isNull(responseRequest.getSortBy())? "_id" : responseRequest.getSortBy(),
                        Objects.isNull(responseRequest.getSortOrder())? "desc" : responseRequest.getSortOrder(),
                        Objects.isNull(responseRequest.getPage()) ? 0: responseRequest.getPage(),
                        Objects.isNull(responseRequest.getSize()) ? 30: responseRequest.getSize()
                ), HttpStatus.OK);
            }  catch (Exception e){
                log.error(e.getMessage(), e.getCause());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> getBoard(PagedResponseRequest responseRequest) {
        if(isValidString(responseRequest.getSpaceId())){
            try{
                String start = "";
                String end ="";
                if(Objects.nonNull(responseRequest.getStartDate())){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    start = sdf.format(responseRequest.getStartDate());
                    end = Objects.isNull(responseRequest.getEndDate()) ? start : sdf.format(responseRequest.getEndDate());
                }
                List<String> subspaces = new ArrayList<>();
                if(isValidString(responseRequest.getSubSpaceId())){
                    String folderId = spaceDao.getFolderId(responseRequest.getSubSpaceId());
                    if(isValidString(folderId)){
                        subspaces.addAll(spaceDao.getSubspacesByFolderId(folderId));
                    } else {
                        subspaces.add(responseRequest.getSubSpaceId());
                    }
                }
                Map<String, Object> response = testCaseRepository.getBoard(responseRequest.getSpaceId(), subspaces, start, end, responseRequest.getTags(), responseRequest.getId(), responseRequest.getRefId());
                if(!response.containsKey("Not Executed")){
                    response.put("Not Executed", Collections.EMPTY_LIST);
                }
                if(!response.containsKey("Passed")){
                    response.put("Passed", Collections.EMPTY_LIST);
                }
                if(!response.containsKey("Failed")){
                    response.put("Failed", Collections.EMPTY_LIST);
                }
                if(!response.containsKey("Ignored")){
                    response.put("Ignored", Collections.EMPTY_LIST);
                }
                if(!response.containsKey("Future")){
                    response.put("Future", Collections.EMPTY_LIST);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            }  catch (Exception e){
                log.error(e.getMessage(), e.getCause());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    }

    @Override
    public ResponseEntity<?> save(TestCase testCase) {
        IdNameRelationDto user = userService.getUserIdAndName(SecurityContextHolder.getContext().getAuthentication().getName());
        if(isValidString(testCase.getId())){
            Optional<TestCase> dbTestCase = testCaseRepository.findById(testCase.getId());
            if(dbTestCase.isPresent()){
                dbTestCase.get().getHistory().add(new TasksHistory("Updated Details", user.getId().toString(), user.getName().toString()));
                dbTestCase.get().setName(testCase.getName());
                dbTestCase.get().setManualId(testCase.getManualId());
                dbTestCase.get().setActualResult(testCase.getActualResult());
                dbTestCase.get().setExpectedResult(testCase.getExpectedResult());
                dbTestCase.get().setTestData(testCase.getTestData());
                dbTestCase.get().setPriority(testCase.getPriority());
                dbTestCase.get().setTestSteps(testCase.getTestSteps());
                dbTestCase.get().setPreCondition(testCase.getPreCondition());
                dbTestCase.get().setTags(testCase.getTags());
                testCaseRepository.save(dbTestCase.get());
            } else {
                return new ResponseEntity<>("Test Case not found", HttpStatus.BAD_REQUEST);
            }
        } else {
            if(isValidString(testCase.getSpaceId()) && isValidString(testCase.getWorkspaceId())){
                testCase.setGeneratedId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
                testCase.setHistory(Arrays.asList(new TasksHistory("Created", user.getId().toString(), user.getName().toString())));
                testCase.setCreatedAt(new Date());
                testCase.setCreatedBy(user.getId().toString());
                testCase.setVersions(Arrays.asList(new TasksVersion(testCase.getSpaceId(), testCase.getSubspaceId(), user.getId().toString(), null, 0)));
                testCaseRepository.save(testCase);
            } else {
                return new ResponseEntity<>("Test case cannot be created without workspace and project", HttpStatus.BAD_REQUEST);
            }
        }
        if(Objects.nonNull(testCase.getTags()) && !testCase.getTags().isEmpty()){
            spaceService.saveTags(testCase.getSpaceId(), testCase.getTags());
        }
        return new ResponseEntity<>("Test Case Saved", HttpStatus.OK);
    }


    @Override
    public ResponseEntity<?> changeStatus(String id, String status) {
        IdNameRelationDto user = userService.getUserIdAndName(SecurityContextHolder.getContext().getAuthentication().getName());
        if(isValidString(id) && isValidString(status) && Objects.nonNull(user)){
            TasksHistory history = new TasksHistory("Changed Status To ".concat(status), user.getId().toString(), user.getName().toString());
            boolean isSuccess = testCaseRepository.changeStatus(id, status, history);
            return isSuccess ?  ResponseEntity.ok(history) : ResponseEntity.badRequest().body("Test Case not found");
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> updateLinks(String id, String type, List<String> data) {
        IdNameRelationDto user = userService.getUserIdAndName(SecurityContextHolder.getContext().getAuthentication().getName());
        TasksHistory history = new TasksHistory("Linked ".concat(type), user.getId().toString(), user.getName().toString());
        boolean isSuccess = testCaseRepository.updateLinks(id, type, data, history);
        return isSuccess ?  ResponseEntity.ok(history) : ResponseEntity.badRequest().body("Test Case not found");
    }

    @Override
    public ResponseEntity<?> getList(List<String> ids) {
        return new ResponseEntity<>(testCaseRepository.getList(ids), HttpStatus.OK);
    }

    @Override
    public void link(List<String> testCases, String id, String source) {
        testCaseRepository.link(testCases, id, source);
    }
    @Override
    public void unlink(List<String> testCases, String id, String source) {
        testCaseRepository.unlink(testCases, id, source);
    }

//    @Override
//    public ResponseEntity<?> copy(String from, String to, String id) {
//        IdNameRelationDto user = userService.getCurrentUserFullName();
//        if(isValidString(to) && Objects.nonNull(user)){
//            List<TestCase> testCases = null;
//
//            if(isValidString(id)){
//                TestCase testCase = testCaseRepository.findById(id).orElse(null);
//                if(Objects.isNull(testCase)){
//                    return new ResponseEntity<>("Test Case Not found", HttpStatus.BAD_REQUEST);
//                }
//                testCases = Arrays.asList(testCase);
//            } else if(isValidString(from)){
//                testCases = testCaseRepository.findBySubspaceId(from);
//            }
//            List<TestCase> copiedTestCases = new ArrayList<>();
//            for(TestCase testCase : testCases){
//                copiedTestCases.add(copyFromOriginal(testCase, to, user.getId().toString(), user.getName().toString()));
//            }
//            testCaseRepository.saveAll(copiedTestCases);
//            return new ResponseEntity<>(testCases.size()+ " Test Cases Linked", HttpStatus.OK);
//        }
//        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
//    }

    @Override
    public ResponseEntity<?> copy(PagedResponseRequest responseRequest) {
        IdNameRelationDto user = userService.getCurrentUserFullName();
        if(isValidString(responseRequest.getTo()) && Objects.nonNull(user)){
            List<TestCase> testCases = null;
            List<String> testCasesMap;
            try{
                String start = "";
                String end ="";
                if(Objects.nonNull(responseRequest.getStartDate())){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    start = sdf.format(responseRequest.getStartDate());
                    end = Objects.isNull(responseRequest.getEndDate()) ? start : sdf.format(responseRequest.getEndDate());
                }
                List<String> subspaces = new ArrayList<>();
                if(isValidString(responseRequest.getSubSpaceId())){
                    String folderId = spaceDao.getFolderId(responseRequest.getSubSpaceId());
                    if(isValidString(folderId)){
                        subspaces.addAll(spaceDao.getSubspacesByFolderId(folderId));
                    } else {
                        subspaces.add(responseRequest.getSubSpaceId());
                    }
                }
                if(isValidString(responseRequest.getId())){
                    TestCase testCase = testCaseRepository.findById(responseRequest.getId()).orElse(null);
                    if(Objects.isNull(testCase)){
                        return new ResponseEntity<>("Test Case Not found", HttpStatus.BAD_REQUEST);
                    }
                    testCases = Arrays.asList(testCase);
                } else if(isValidString(responseRequest.getFrom())){
                    if (responseRequest.getId() != "" && responseRequest.getId() != null) {
                        testCases = testCaseRepository.findBySubspaceId(responseRequest.getFrom());
                    } else {
                        testCasesMap = (List<String>) testCaseRepository.getBoardAsTestCase(responseRequest.getSpaceId(), subspaces, start, end, responseRequest.getTags(), responseRequest.getId(), responseRequest.getRefId()).get("data");
                        if (testCasesMap != null) {
                            testCases = (List<TestCase>) testCaseRepository.findAllById(testCasesMap);
                            if (testCases.size() < 1) {
                                return new ResponseEntity<>("Test Case Not found", HttpStatus.BAD_REQUEST);
                            }
                        }
                    }
                }
                List<TestCase> copiedTestCases = new ArrayList<>();
                for(TestCase testCase : testCases){
                    System.out.println("testCase = " + testCase);
                    copiedTestCases.add(copyFromOriginal(testCase, responseRequest.getTo(), user.getId().toString(), user.getName().toString()));
                }
                testCaseRepository.saveAll(copiedTestCases);
                return new ResponseEntity<>(testCases.size()+ " Test Cases Linked", HttpStatus.OK);
            }  catch (Exception e){
                log.error(e.getMessage(), e.getCause());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> versionHistory(String id) {
        if(isValidString(id)){
            List<Document> list = testCaseRepository.getVersionHistory(id);
            boolean isRoot = true;
            for(Map<String, Object> item : list){
                if(Objects.equals(item.get("version"), 0)){
                    isRoot = false;
                }
            }
            if(isRoot){
                Collections.reverse(list);
                list.add(new Document().append("version", 0).append("segment","Project Root"));
                Collections.reverse(list);
            }
            return new ResponseEntity<>(list, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    TestCase copyFromOriginal(TestCase testCase, String subspaceId, String userId, String userName){
        TestCase newTestCase = new TestCase();
        newTestCase.setName(testCase.getName());
        newTestCase.setSpaceId(testCase.getSpaceId());
        newTestCase.setSubspaceId(subspaceId);
        newTestCase.setPriority(testCase.getPriority());
        newTestCase.setPreviousSubspaceId(Objects.nonNull(testCase.getSubspaceId()) ? testCase.getSubspaceId() : "ROOT");
        newTestCase.setActualResult(testCase.getActualResult());
        newTestCase.setExpectedResult(testCase.getExpectedResult());
        newTestCase.setTestData(testCase.getTestData());
        newTestCase.setTestSteps(testCase.getTestSteps());
        newTestCase.setManualId(testCase.getManualId());
        newTestCase.setPreCondition(testCase.getPreCondition());
        newTestCase.setTags(testCase.getTags());
        newTestCase.setVersions(testCase.getVersions());
        newTestCase.getVersions().add(new TasksVersion(newTestCase.getSpaceId(), subspaceId, userId, testCase.getStatus(), testCase.getVersions().size()));
        newTestCase.setGeneratedId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        newTestCase.setHistory(new ArrayList<>());
        newTestCase.setStatus("Not Executed");
        newTestCase.getHistory().add(new TasksHistory(
                "Copied From Test Case #"+testCase.getGeneratedId(),
                userId, userName));
        return newTestCase;
    }
}
