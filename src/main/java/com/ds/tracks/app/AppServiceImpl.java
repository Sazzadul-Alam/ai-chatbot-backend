package com.ds.tracks.app;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.ds.tracks.commons.utils.Utils.daysBetween;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService{

    private final AppDao repository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final WorkspaceRepository workspaceRepository;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public ResponseEntity<?> analytics(AppRequest request) {
        if(Objects.isNull(request.getStartDate())){
            request.setStartDate(new Date());
        }
        if(Objects.isNull(request.getEndDate())){
            request.setEndDate(request.getStartDate());
        }
        long numOfDays = days(request.getStartDate(), request.getEndDate());
        String workdays = Math.ceil(numOfDays) < 1 ? "1" : Double.toString(Math.ceil(numOfDays));
        return new ResponseEntity<>(
                repository.analytics(
                        request.getWorkspace(),
                        request.getProject(),
                        format.format(request.getStartDate()),
                        format.format(request.getEndDate()),
                        userService.getCurrentUserId(),
                        workdays
                ), HttpStatus.OK);
    }
    static long days(Date startDate, Date endDate){
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);

        int workDays = 0;

        //Return 0 if start and end are the same
        if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
            return 0;
        }

        if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
            startCal.setTime(endDate);
            endCal.setTime(startDate);
        }

        do {
            //excluding start date
            startCal.add(Calendar.DAY_OF_MONTH, 1);
            if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                ++workDays;
            }
        } while (startCal.getTimeInMillis() < endCal.getTimeInMillis()); //excluding end date

        return workDays;
    }

    @Override
    public ResponseEntity<?> tasks(AppRequest request) {
        return new ResponseEntity<>(repository.tasks(formatAndCleanDate(request), userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> issues(AppRequest request) {
        return new ResponseEntity<>(repository.issues(formatAndCleanDate(request), userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> backlogs(AppRequest request) {
        return new ResponseEntity<>(repository.backlogs(formatAndCleanDate(request), userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> findClientList() {
        return null;
    }

    @Override
    public ResponseEntity<?> tasksList(AppRequest request) {
        return new ResponseEntity<>(repository.tasksList(formatAndCleanDate(request), userService.getCurrentUserId()),HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> taskStatusPieChart(String spaceId) {
        return new ResponseEntity<>(repository.taskStatusPieChart(spaceId),HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> workloadChart(AppRequest request) {
        if(Objects.isNull(request.getStartDate())){
            request.setStartDate(new Date());
        }
        if(Objects.isNull(request.getEndDate())){
            request.setEndDate(request.getStartDate());
        }
        long numOfDays = days(request.getStartDate(), request.getEndDate());
        String workdays = Math.ceil(numOfDays) < 1 ? "1" : Double.toString(Math.ceil(numOfDays));
        return new ResponseEntity<>(
                repository.workloadChart(
                        request.getWorkspace(),
                        request.getProject(),
                        format.format(request.getStartDate()),
                        format.format(request.getEndDate()),
                        userService.getCurrentUserId(),
                        workdays
                ), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> configurations() {
        Workspace workspace = workspaceRepository.findFirst();
        return new ResponseEntity<>(workspace, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> invoiceList() {
        auditLogService.save("Viewed Invoice List");
        return new ResponseEntity<>(repository.invoiceList(), HttpStatus.OK);
    }

    private AppRequest formatAndCleanDate(AppRequest request){
        if(Objects.isNull(request.getStartDate())){
            request.setStartDate(new Date());
        }
        if(Objects.isNull(request.getEndDate())){
            request.setEndDate(request.getStartDate());
        }
        request.setStartDateString(format.format(request.getStartDate()));
        request.setEndDateString(format.format(request.getEndDate()));
        return request;
    }
}
