package com.ds.tracks.workload;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.user.dao.UserDao;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.user.repository.UserRepository;
import com.ds.tracks.user.repository.UsersPermissionRepository;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.models.enums.ManagementRoles.ADMIN;
import static com.ds.tracks.commons.utils.Utils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkLoadServiceImpl implements WorkLoadService {
    private final WorkloadDao workloadDao;
    private final WorkspaceRepository workspaceRepository;
    private final SpaceDao spaceDao;
    private final UsersPermissionRepository usersPermissionRepository;
//    private final UserRepository userRepository;
    private final UserDao userDao;
    private final UserService userService;
    private final TasksDao tasksDao;
    private final AuditLogService auditLogService;
    final List<String> weekDays = Arrays.asList("Sunday", "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday");

    @Override
    public ResponseEntity<?> getWorkload(String workspaceId,String spaceId,String subspaceId, String startDate, String endDate) {
        List<String> spaces = spaceDao.findAllSpaces(workspaceId);
//        Map<String, List<Map<String, Object>>> data = workloadDao.findWorkloadForSpaceOrSubspaceByDateBetween(spaceId,subspaceId,startDate,endDate);
        Map<String, List<Document>> data = workloadDao.findWorkloadForWorkspace(spaces,startDate,endDate);
        List<Document> userData = userDao.findAllUserIdAndNameAndImage();

//        Map<String, Object> configuration = spaceDao.configurationForWorkload(startDate, endDate, spaceId);
        Workspace workspace = workspaceRepository.findFirstConfiguration(workspaceId);
        double capacity = 0;//Objects.nonNull(configuration.get("workHour")) ? Double.parseDouble(configuration.get("workHour").toString()) : 0D;
        double weekendCount = 0;// Objects.nonNull(configuration.get("totalWeekend")) ? Double.parseDouble(configuration.get("totalWeekend").toString()) : 0D;
        if(Objects.nonNull(workspace.getConfigurations())){
            capacity = Objects.isNull(workspace.getConfigurations().getWorkHour()) ? 0 :workspace.getConfigurations().getWorkHour();
            weekendCount = Objects.isNull(workspace.getConfigurations().getWeekend()) ? 0 : workspace.getConfigurations().getWeekend().size();
        }
        double workingDays = 7 - weekendCount;
        for(Document weeklyWorkload : userData){
            Map<String, Double> res = new HashMap<>();
//            Double capacity  = Objects.isNull(weeklyWorkload.get("capacity")) ? companyWorkHour : Double.parseDouble(weeklyWorkload.get("capacity").toString());
            if(data.containsKey(weeklyWorkload.get("id").toString())){
                for(Map<String, Object> row : data.get(weeklyWorkload.get("id").toString())){
                    switch (Objects.nonNull(row.get("day")) ? row.get("day").toString() : ""){
                        case "1":containOrAppend(res, "Sunday", row); break;
                        case "2":containOrAppend(res, "Monday", row); break;
                        case "3":containOrAppend(res, "Tuesday", row); break;
                        case "4":containOrAppend(res, "Wednesday", row); break;
                        case "5":containOrAppend(res, "Thursday", row); break;
                        case "6":containOrAppend(res, "Friday", row); break;
                        case "7":containOrAppend(res, "Saturday", row); break;
                        default: break;
                    }
                }
            }
            double totalBookedHour = 0D;
            for(String day : weekDays){
                double hour = Objects.isNull(res.get(day)) ? 0D : res.get(day);
                weeklyWorkload.put(day, buildDailyWorkload(hour, capacity));
                totalBookedHour += hour;
            }
            weeklyWorkload.put("capacityHour", capacity * workingDays);
            weeklyWorkload.put("totalBookedHour", totalBookedHour);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("data", userData);
        res.put("permission", userService.hasAccess(Arrays.asList(AccessPoints.TASK_MANAGEMENT)));
        auditLogService.save("Viewed Workload");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<?> reassign(String id, String user) {
        if(isValidString(id) && isValidString(user)){
            UpdateResult updateResult = tasksDao.reassignForWorkload(id, user);
            return new ResponseEntity<>(updateResult.getModifiedCount()> 0 ? "Updated": "No Records Updated", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);

    }

    @Override
    public ResponseEntity<?> findByDate(Date date, String assignedTo, String workspaceId, String spaceId, String subspaceId) {
        try{
            String taskDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
            return new ResponseEntity<>(workloadDao.findByDateAndAssignedToForSpaceOrSubSpace(taskDate, assignedTo, workspaceId, spaceId, subspaceId), HttpStatus.OK);

        } catch (Exception ignored){}
        return new ResponseEntity<>("Invalid Params",HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> transfer(String id, Date date) {
        try{
            String taskDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
            if(isValidString(id)){
                date.setHours(6);
                tasksDao.changeDateOfSchedule(id, date, taskDate);
                auditLogService.save("Transferred Workload");
                return new ResponseEntity<>("Updated", HttpStatus.OK);
            }
        } catch (Exception ignored){}
        return new ResponseEntity<>("Invalid Params",HttpStatus.BAD_REQUEST);
    }

    private DailyWorkload buildDailyWorkload(Double engagedHour, Double capacity) {
        DailyWorkload dailyWorkload = new DailyWorkload();
        dailyWorkload.setEngagedHour(engagedHour);
        if(Objects.isNull(dailyWorkload.getEngagedHour()) || Objects.equals(dailyWorkload.getEngagedHour(), 0D)){
            dailyWorkload.setLoadType(LoadType.empty);
            dailyWorkload.setBarHeight(0D);
            dailyWorkload.setEngagedHour(0D);
        } else if(dailyWorkload.getEngagedHour() > capacity) {
            dailyWorkload.setLoadType(LoadType.over);
            dailyWorkload.setBarHeight(100D);
            dailyWorkload.setExtraHour(Double.parseDouble(new DecimalFormat("##.##").format(dailyWorkload.getEngagedHour() - capacity)));
        } else if(dailyWorkload.getEngagedHour() < capacity){
            dailyWorkload.setLoadType(LoadType.under);
            dailyWorkload.setBarHeight(Double.parseDouble(new DecimalFormat("##.##").format((dailyWorkload.getEngagedHour()/capacity)*100)));
        } else {
            dailyWorkload.setBarHeight(100D);
            dailyWorkload.setLoadType(LoadType.equal);
        }
        return dailyWorkload;
    }

    private void containOrAppend(Map<String, Double> res, String key, Map<String, Object> data) {
        if(!res.containsKey(key)){
            res.put(key, 0D);
        }
        res.put(key,res.get(key)+( data.containsKey("workload") ? Double.parseDouble(data.get("workload").toString()) : 0D));
    }
}
