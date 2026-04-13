package com.ds.tracks.effort.service;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.effort.repository.EffortLogRepository;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.tasks.model.Tasks;
import com.ds.tracks.tasks.repository.TasksRepository;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

import static com.ds.tracks.commons.utils.Utils.isValidString;
import static com.ds.tracks.commons.utils.Utils.sourceToCollectionName;


@Slf4j
@Service
@RequiredArgsConstructor
public class EffortLogServiceImpl implements EffortLogService {
    private final EffortLogRepository effortLogRepository;
    private final TasksDao tasksDao;
    private final TasksRepository tasksRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;


    @Override
    public ResponseEntity<?> entryEffortLog(EffortLog effortLog, HttpServletRequest request) {
        try {
            effortLog.setCreatedAt(new Date());
            effortLog.setCreatedBy(userService.getCurrentUserId());
            Tasks tasks = tasksRepository.findFirstById(effortLog.getTaskId());
            effortLog.setSpaceId(tasks.getSpaceId());
            effortLog.setSubspaceId(tasks.getSubSpaceId());
            effortLogRepository.save(effortLog);
            tasksDao.updateTaskOrSubtaskForEffortLog(isValidString(effortLog.getSubTaskId())
                    ? effortLog.getSubTaskId() : effortLog.getTaskId(),effortLog.getCompletion(),
                    effortLog.getDuration(), isValidString(effortLog.getSubTaskId()));
//            reportDataService.addEffort(effortLog);
            auditLogService.save( "Logged Effort", CollectionName.task, effortLog.getTaskId(), tasks.getSpaceId(), tasks.getSubSpaceId());
            return new ResponseEntity<>("Effort Logged",HttpStatus.OK);
        } catch (Exception e){
            log.error(e.getMessage(),e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> logs(String id, String type) {
        auditLogService.save("Viewed Effort Logs", sourceToCollectionName(type), id);
        return new ResponseEntity<>(effortLogRepository.findEffortLogsById(id, Objects.equals(type, "subtask")), HttpStatus.OK);
    }


}
