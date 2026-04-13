package com.ds.tracks.testCase;

import com.ds.tracks.commons.models.PagedResponseRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface TestCaseService {
    ResponseEntity<?> findById(String id);

    ResponseEntity<?> getPage(PagedResponseRequest responseRequest);

    ResponseEntity<?> getBoard(PagedResponseRequest responseRequest);

    ResponseEntity<?> save(TestCase testCase);

    ResponseEntity<?> changeStatus(String id, String status);

    ResponseEntity<?> updateLinks(String id, String type, List<String> data);

    ResponseEntity<?> getList(List<String> ids);

    void link(List<String> list, String id, String source);

    void unlink(List<String> needToPull, String id, String source);

    ResponseEntity<?> copy(PagedResponseRequest responseRequest);

    ResponseEntity<?> versionHistory(String id);
}
