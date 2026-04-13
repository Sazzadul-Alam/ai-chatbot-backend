package com.ds.tracks.testCase;


import com.ds.tracks.commons.models.PagedResponseRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@RestController
@RequestMapping("/testcase")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping("/get/{id}")
    public ResponseEntity<?> findById(@PathVariable String id){
        return testCaseService.findById(id);
    }

    @PostMapping("/page")
    public ResponseEntity<?> getPage(@RequestBody PagedResponseRequest responseRequest) {
        return testCaseService.getPage(responseRequest);
    }

    @PostMapping("/board")
    public ResponseEntity<?> getBoard(@RequestBody PagedResponseRequest responseRequest) {
        return testCaseService.getBoard(responseRequest);
    }

    @PostMapping("/list")
    public ResponseEntity<?> getList(@RequestBody List<String> ids) {
        return testCaseService.getList(ids);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody TestCase testCase){
        return testCaseService.save(testCase);
    }

    @PostMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestParam String id, @RequestParam String status){
        return testCaseService.changeStatus(id, status);
    }

    @PostMapping("/update-links")
    public ResponseEntity<?> updateLinks(@RequestParam String id, @RequestParam String type, @RequestParam List<String> data){
        return testCaseService.updateLinks(id, type, data);
    }

    @PostMapping("/copy")
    public ResponseEntity<?> copy(@RequestBody PagedResponseRequest responseRequest){
        return testCaseService.copy(responseRequest);
    }

    @PostMapping("/version-history")
    public ResponseEntity<?> versionHistory(@RequestParam String id){
        return testCaseService.versionHistory(id);
    }
}
