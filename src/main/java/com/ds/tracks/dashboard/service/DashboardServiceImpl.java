package com.ds.tracks.dashboard.service;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.dashboard.dao.DashboardDao;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.model.SpaceConfigurations;
import com.ds.tracks.space.repository.SpaceRepository;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.model.Notes;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.user.repository.NotesRepository;
import com.ds.tracks.user.repository.UsersPermissionRepository;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workload.DailyWorkload;
import com.ds.tracks.workload.LoadType;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.models.enums.PermissionLayer.SPACE;
import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardDao dashboardDao;
    private final SpaceRepository spaceRepository;
    private final UsersPermissionRepository usersPermissionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;
    private final NotesRepository notesRepository;
    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<?> projectsSummary(String workspaceId, String spaceId, Date start, Date end) {
        if(Objects.isNull(start)){ start = new Date(); }
        if(Objects.isNull(end)){ end = start; }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = df.format(start);
        String endDate = df.format(end);
        double workDays = 0D, workHour = 0D;
        List<String> weekends = new ArrayList<>();
        List<String> spaceIds = new ArrayList<>();
        String currentUser = userService.getCurrentUserId();
        if(!isValidString(spaceId)){
            if(userService.hasAccess(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT))){
                spaceIds = spaceRepository.findAllId().stream().map(Space::getId).collect(Collectors.toList());
            } else {
                spaceIds = usersPermissionRepository.findAllSpaceIdByWorkspaceIdAndPermissionForAndUserId(workspaceId, currentUser);
            }
        }
        Map<String, Object> data = new HashMap<>();
        if(isValidString(spaceId)){
            SpaceConfigurations configurations = spaceRepository.findConfigurationsById(spaceId).orElse(new Space()).getConfigurations();
            if(Objects.nonNull(configurations)){
                if(Objects.nonNull(configurations.getWeekend())){
                    weekends = configurations.getWeekend().stream().map(String::toUpperCase).collect(Collectors.toList());
                }
                if(Objects.nonNull(configurations.getWorkHour())){
                    workHour = configurations.getWorkHour();
                }
            }
            for (LocalDate date = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                 !date.isAfter(end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                 date = date.plusDays(1)) {
                if (!weekends.contains(date.getDayOfWeek().toString())) {
                    workDays++;
                }
            }
            data = dashboardDao.generate( spaceId, startDate, endDate, String.valueOf(workHour), String.valueOf(workDays));
            auditLogService.save("Viewed Insights", CollectionName.spaces, spaceId, spaceId, null);
        } else {
            data = dashboardDao.generateOverall( spaceIds, startDate, endDate, currentUser, String.valueOf(workDays));
            auditLogService.save("Viewed Insights");
        }
        data.put("tasks", dashboardDao.tasksSummary( getSpaceIds(workspaceId, spaceId, currentUser), currentUser));
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> workload(String workspaceId, String spaceId, Date start, Date end) {
        if(Objects.isNull(start)){ start = new Date(); }
        if(Objects.isNull(end)){ end = start; }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = df.format(start);
        String endDate = df.format(end);
        String userId = userService.getCurrentUserId();
        Workspace workspace= workspaceRepository.findFirstConfiguration(workspaceId);
        Double capacity = 0D;
        if(Objects.nonNull(workspace.getConfigurations())){
            capacity = Objects.isNull(workspace.getConfigurations().getWorkHour()) ? 0 : workspace.getConfigurations().getWorkHour();
        }
        double workDays = ChronoUnit.DAYS.between(start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) + 1;
        List<String> spaceIds = getSpaceIdsForWorkload(workspaceId, userId);
        if(isValidString(spaceId) ){
            spaceIds = spaceIds.contains(spaceId) ? Arrays.asList(spaceId) : new ArrayList<>();
        }
        List<Document> spaces = new ArrayList<>();
        if(!spaceIds.isEmpty()){
            spaces = dashboardDao.workload(spaceIds, startDate, endDate, userId, String.valueOf(workDays));
        }
        Double totalWorkload = 0D;
        Double totalCapacity = 0D;
//        if(spaces.isEmpty() && !spaceIds.isEmpty()){
//            spaces = dashboardDao.workloadWithJustCapacity(spaceIds, startDate, endDate, userService.getCurrentUserId(), String.valueOf(workDays));
//        }

        capacity = capacity * workDays;
        for(Document space : spaces){
            Double workload = Objects.isNull(space.get("workload")) ? 0D : Double.parseDouble(space.get("workload").toString());
//            Double capacity = Objects.isNull(space.get("capacity")) ? 0D : Double.parseDouble(space.get("capacity").toString()) ;
//            capacity = capacity * workDays;
            LoadType loadType = workload == 0
                    ? LoadType.empty
                    : (workload < capacity)
                    ? LoadType.under
                    : (workload > capacity)
                    ? LoadType.over
                    : (workload.equals(capacity))
                    ? LoadType.equal
                    : LoadType.empty;
            if(Objects.equals(loadType, LoadType.empty)){
                space.put("barHeight", 0);
            } else if(Objects.equals(loadType, LoadType.over) || Objects.equals(loadType, LoadType.equal)){
                space.put("barHeight", 100);
            } else {
                space.put("barHeight", Double.parseDouble(new DecimalFormat("##.##").format((workload/capacity)*100)));
            }
            space.put("workload", workload);
            space.put("capacity", capacity);
            space.put("loadType", loadType);
            totalWorkload += workload;
//            totalCapacity += capacity;
        }
        Map<String, Object> res =new HashMap<>();
        res.put("workloads", spaces);
        res.put("totalWorkload", totalWorkload);
        res.put("totalCapacity", capacity);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
    @Override
    public ResponseEntity<?> tasks(String workspaceId, String spaceId, Date start, Date end) {
        if(Objects.isNull(spaceId)){
            auditLogService.save("Viewed Dashboard");
        } else {
            auditLogService.save("Viewed Dashboard", CollectionName.spaces, spaceId, spaceId, null);
        }
        if(Objects.isNull(start)){ start = new Date(); }
        if(Objects.isNull(end)){ end = start; }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = df.format(start);
        String endDate = df.format(end);

        return new ResponseEntity<>(dashboardDao.tasks( getSpaceIds(workspaceId, spaceId, userService.getCurrentUserId()), startDate, endDate, userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> efforts(String workspaceId, String spaceId, Date start, Date end) {
        if(Objects.isNull(start)){ start = new Date(); }
        if(Objects.isNull(end)){ end = start; }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = df.format(start);
        String endDate = df.format(end);
        String currentUserId = userService.getCurrentUserId();
        return new ResponseEntity<>(dashboardDao.effort( getSpaceIds(workspaceId, spaceId, currentUserId), startDate, endDate, currentUserId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getNote() {
        Notes notes = notesRepository.findFirstByUserId(SecurityContextHolder.getContext().getAuthentication().getName());
        return new ResponseEntity<>(Objects.nonNull(notes) ? notes.getNote() : "", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> saveNote(String note) {
        notesRepository.save(new Notes(SecurityContextHolder.getContext().getAuthentication().getName(), note));
        return new ResponseEntity<>("Saved", HttpStatus.OK);
    }


    List<String> getSpaceIds(String workspaceId, String spaceId, String userId){
        if(isValidString(spaceId)) {
            return Arrays.asList(spaceId);
        }
        return usersPermissionRepository.findAllSpaceIdByWorkspaceIdAndPermissionForAndUserId(workspaceId, userId);
    }
    List<String> getSpaceIdsForWorkload(String workspaceId, String userId){
        return usersPermissionRepository.findAllSpaceIdByWorkspaceIdAndUserIdAndPermissionForAndRoleNe(workspaceId, userId, "SPACE", "OBSERVER");
    }
}
