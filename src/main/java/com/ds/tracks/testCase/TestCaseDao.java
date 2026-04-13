package com.ds.tracks.testCase;

import com.ds.tracks.tasks.model.dto.TasksHistory;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface TestCaseDao {
    boolean changeStatus(String id, String status, TasksHistory tasksHistory);

    Map<String, Object> getBoard(String spaceId, List<String> subspaceIds,
                                 String startDate, String endDate,
                                 List<String> tags, String param, String refId);
    Object getPage(String spaceId, List<String> subspaceIds,
                    String startDate, String endDate,
                    List<String> tags, List<String> status,
                    String sortBy, String sortOrder,
                    int page, int size);

    boolean updateLinks(String id, String type, List<String> data, TasksHistory history);

    Object getList(List<String> ids);

    void link(List<String> testCases, String id, String source);

    void unlink(List<String> testCases, String id, String source);

    List<TestCase> findBySubspaceId(String subspaceId);

    List<Document> getVersionHistory(String id);

    Map<String, Object> getBoardAsTestCase(String spaceId, List<String> subspaces, String start, String end, List<String> tags, String id, String refId);
}
