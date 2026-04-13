package com.ds.tracks.workload;

import com.ds.tracks.tasks.model.dto.TasksDto;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface WorkLoadService {

    ResponseEntity<?> getWorkload(String workspaceId,String spaceId,String subSpaceId, String startDate, String endDate) throws ParseException;

    ResponseEntity<?> reassign(String id, String user);

    ResponseEntity<?> findByDate(Date date, String assignedTo, String workspaceId, String spaceId, String subspaceId);

    ResponseEntity<?> transfer(String id, Date date);
}
