package com.ds.tracks.ganttChart;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GanttChartServiceImpl implements GanttChartService {


    private final GanttChartDao ganttChartDao;
    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<?> getGanttChartInitialData(String spaceId) {
        auditLogService.save("Viewed Gantt Chart", CollectionName.spaces, spaceId, spaceId, null);
        return new ResponseEntity<>(ganttChartDao.getGanttChartInitialData(spaceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getSubSpaceBySpace(String spaceId) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) ganttChartDao.getSubSpaceBySpace(spaceId);

        List<Map<String, Object>> efforts = (List<Map<String, Object>>) ganttChartDao.getSubSpaceEffortDates(spaceId);

        List<Map<String, Object>> results = new ArrayList<>();


        for (Map<String, Object> dt : data) {

            results.add(dt);

            Long startDate = null;
            Long endDate = null;
            String objId = null;

            if (dt.get("dType").equals("folder")) {

                for (Map<String, Object> ef : efforts) {
                    Long effortStart = ef.get("start") != null ? (Long) ef.get("start") : Long.MAX_VALUE;
                    Long effortEnd = ef.get("end") != null ? (Long) ef.get("end") : Long.MIN_VALUE;

                    if (ef.get("folderId") != null && ef.get("folderId").equals(dt.get("id"))) {
                        startDate = Math.min((startDate != null ? startDate : effortStart), effortStart);
                        endDate = Math.max((endDate != null ? endDate : effortEnd), effortEnd);
                    }
                }

                objId = dt.get("id") + String.valueOf(new Timestamp(System.currentTimeMillis()).getTime());
            }

            if (dt.get("dType").equals("sub_space")) {
                for (Map<String, Object> ef : efforts) {

                    Long effortStart = ef.get("start") != null ? (Long) ef.get("start") : Long.MAX_VALUE;
                    Long effortEnd = ef.get("end") != null ? (Long) ef.get("end") : Long.MIN_VALUE;

                    if (ef.get("subSpaceId").equals(dt.get("id"))) {
                        startDate = Math.min((startDate != null ? startDate : effortStart), effortStart);
                        endDate = Math.max((endDate != null ? endDate : effortEnd), effortEnd);
                    }
                }
                objId = dt.get("id") + String.valueOf(new Timestamp(System.currentTimeMillis()).getTime());
            }

            Map<String, Object> temp = new HashMap<>();
            temp.put("id", objId);
            temp.put("title", (dt.get("title")+" Time Line"));
            temp.put("start", startDate);
            temp.put("end", endDate);
            results.add(temp);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getSubSpacesByFolder(String spaceId, String folderId) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) ganttChartDao.getSubSpacesByFolder(folderId);

        List<Map<String, Object>> efforts = (List<Map<String, Object>>) ganttChartDao.getSubSpaceEffortDates(spaceId);

        List<Map<String, Object>> results = new ArrayList<>();


        for (Map<String, Object> dt : data) {

            results.add(dt);

            Long startDate = null;
            Long endDate = null;
            String objId = null;

            if (dt.get("dType").equals("sub_space")) {
                for (Map<String, Object> ef : efforts) {

                    Long effortStart = ef.get("start") != null ? (Long) ef.get("start") : Long.MAX_VALUE;
                    Long effortEnd = ef.get("end") != null ? (Long) ef.get("end") : Long.MIN_VALUE;

                    if (ef.get("subSpaceId").equals(dt.get("id"))) {
                        startDate = Math.min((startDate != null ? startDate : effortStart), effortStart);
                        endDate = Math.max((endDate != null ? endDate : effortEnd), effortEnd);
                    }
                }
                objId = dt.get("id") + String.valueOf(new Timestamp(System.currentTimeMillis()).getTime());
            }

            Map<String, Object> temp = new HashMap<>();
            temp.put("id", objId);
            temp.put("title", (dt.get("title")+" Time Line"));
            temp.put("start", startDate);
            temp.put("end", endDate);
            results.add(temp);
        }


        return new ResponseEntity<>(results, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<?> getTaskBySubSpace(String subSpaceId) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) ganttChartDao.getTaskBySubSpace(subSpaceId);

        List<Map<String, Object>> efforts = (List<Map<String, Object>>) ganttChartDao.getTaskEffortDates(subSpaceId);

        List<Map<String, Object>> results = new ArrayList<>();


        for (Map<String, Object> dt : data) {

            results.add(dt);

            Long startDate = null;
            Long endDate = null;
            String objId;


            for (Map<String, Object> ef : efforts) {

                Long effortStart = ef.get("start") != null ? (Long) ef.get("start") : Long.MAX_VALUE;
                Long effortEnd = ef.get("end") != null ? (Long) ef.get("end") : Long.MIN_VALUE;

                if (ef.get("taksId").equals(dt.get("id"))) {
                    startDate = Math.min((startDate != null ? startDate : effortStart), effortStart);
                    endDate = Math.max((endDate != null ? endDate : effortEnd), effortEnd);
                }
            }
            objId = dt.get("id") + String.valueOf(new Timestamp(System.currentTimeMillis()).getTime());

            Map<String, Object> temp = new HashMap<>();
            temp.put("id", objId);
            temp.put("title", (dt.get("title")+" Time Line"));
            temp.put("start", startDate);
            temp.put("end", endDate);
            results.add(temp);
        }


        return new ResponseEntity<>(results, HttpStatus.OK);

    }


    @Override
    public ResponseEntity<?> getSubTasksByTasks(String taskId) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) ganttChartDao.getSubTasksByTasks(taskId);

        List<Map<String, Object>> efforts = (List<Map<String, Object>>) ganttChartDao.getSubTaskEffortDates(taskId);

        List<Map<String, Object>> results = new ArrayList<>();


        for (Map<String, Object> dt : data) {

            results.add(dt);

            Long startDate = null;
            Long endDate = null;
            String objId;


            for (Map<String, Object> ef : efforts) {

                Long effortStart = ef.get("start") != null ? (Long) ef.get("start") : Long.MAX_VALUE;
                Long effortEnd = ef.get("end") != null ? (Long) ef.get("end") : Long.MIN_VALUE;

                if (ef.get("subTaskId").equals(dt.get("id"))) {
                    startDate = Math.min((startDate != null ? startDate : effortStart), effortStart);
                    endDate = Math.max((endDate != null ? endDate : effortEnd), effortEnd);
                }
            }
            objId = dt.get("id") + String.valueOf(new Timestamp(System.currentTimeMillis()).getTime());

            Map<String, Object> temp = new HashMap<>();
            temp.put("id", objId);
            temp.put("title", (dt.get("title")+" Time Line"));
            temp.put("start", startDate);
            temp.put("end", endDate);
            results.add(temp);
        }


        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
