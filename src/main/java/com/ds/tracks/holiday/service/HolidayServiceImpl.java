package com.ds.tracks.holiday.service;

import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.holiday.model.HolidayDto;
import com.ds.tracks.holiday.model.HolidayFailure;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.repository.SpaceRepository;
import com.ds.tracks.space.repository.SubSpaceRepository;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DateValidator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final SpaceDao spaceDao;
    private final SpaceRepository spaceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SubSpaceRepository subSpaceRepository;

    public Boolean dateValidator(String date) {
        DateValidator dateValidator = DateValidator.getInstance();
        String dateFormat = "yyyy-MM-dd";
        return  dateValidator.isValid(date, dateFormat);
    }

    private void saveHoliday(List<Holiday> holidayList, String spaceId){
        spaceDao.saveHoliday(holidayList, spaceId);
    }

    private boolean HeaderValidation(String date, String eventName, String description) {
        return Objects.equals("Date (yyyy-MM-dd)", date) && Objects.equals("Event Name", eventName) && Objects.equals("Description", description);
    }

    @Override
    public ResponseEntity<String> save(HolidayDto holidayDto) {
        try{
            if(Objects.equals(holidayDto.getSource(), "space")){
                saveHoliday(holidayDto.getHolidays(), holidayDto.getSpaceId());
            } else {
                saveWorkspaceHoliday(holidayDto.getHolidays(), holidayDto.getWorkspaceId());
            }
            return new ResponseEntity<>("Holiday saved",HttpStatus.OK);
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void saveWorkspaceHoliday(List<Holiday> holidays, String workspaceId) {
        spaceDao.saveWorkspaceHoliday(holidays, workspaceId);
    }

    @Override
    public ResponseEntity<?> upload(MultipartFile file, String source, String spaceId, String workspaceId) {
        try{
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet worksheet = workbook.getSheetAt(0);
            HashMap<String, Object> fileResponseMap = new HashMap<>();
            if (worksheet.getPhysicalNumberOfRows() > 0) {
                XSSFRow row = worksheet.getRow(0);
                if (row.getCell(0) != null && row.getCell(1) != null && row.getCell(2) != null) {
                    if (HeaderValidation(row.getCell(0).getStringCellValue(),row.getCell(1).getStringCellValue(),row.getCell(2).getStringCellValue())) {
                        if (worksheet.getPhysicalNumberOfRows() > 1) {
                            ArrayList<Holiday> successList = new ArrayList<>();
                            ArrayList<HolidayFailure> failureList = new ArrayList<>();
                            for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
                                List<String> fails = new ArrayList<>();
                                Holiday holidayData = new Holiday();
                                HolidayFailure failureData = new HolidayFailure();
                                row = worksheet.getRow(i);
                                try{
                                    if(Objects.nonNull(row.getCell(0))){
                                        if (!dateValidator(row.getCell(0).getStringCellValue())) {
                                            fails.add("Invalid Date");
                                        } else{
                                            holidayData.setDate(formatter.parse(row.getCell(0).getStringCellValue()));
                                        }
                                    } else {
                                        fails.add("Date Missing");
                                    }
                                } catch (Exception e){
                                    fails.add("Invalid Date");
                                }
                                try{
                                    if(Objects.nonNull(row.getCell(1))){
                                        holidayData.setEventName(row.getCell(1).getStringCellValue());
                                        if(holidayData.getEventName().length() > 50){
                                            fails.add("Event Name Should be less than 50 characters");
                                        }
                                    } else {
                                        fails.add("Event Name Missing");
                                    }
                                } catch (Exception e){
                                    fails.add("Invalid Event Name");
                                }
                                try{
                                    holidayData.setDescription(Objects.nonNull(row.getCell(1)) ? row.getCell(1).getStringCellValue() : "");
                                    if(holidayData.getDescription().length() > 250){
                                        fails.add("Event Description Should be less than 250 characters");
                                    }
                                } catch (Exception e){
                                    fails.add("Invalid Description");
                                }
                                if(fails.isEmpty()) {
                                    successList.add(holidayData);
                                } else {
                                    failureData.setDate(holidayData.getDate());
                                    failureData.setEventName(holidayData.getEventName());
                                    failureData.setDescription(holidayData.getDescription());
                                    failureData.setReason(String.join(", ",fails));
                                    failureData.setRowNo(i);
                                    failureList.add(failureData);
                                }
                            }
                            if(failureList.isEmpty()){
                                if(Objects.equals(source, "space")){
                                    saveHoliday(successList, spaceId);
                                } else {
                                    saveWorkspaceHoliday(successList, workspaceId);
                                }
                                fileResponseMap.put("Status", "Complete");
                            } else {
                                fileResponseMap.put("Status", "Hold");
                                fileResponseMap.put("Success", successList);
                                fileResponseMap.put("Failed", failureList);
                            }
                            return new ResponseEntity<>(fileResponseMap, HttpStatus.OK);
                        }
                    }
                }
            }
            return new ResponseEntity<>("Invalid File", HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> delete(HolidayDto holidayDto) {
        if(Objects.equals(holidayDto.getSource(), "space")){
            if(Objects.nonNull(holidayDto.getSpaceId())){
                Space space = spaceRepository.findFirstById(holidayDto.getSpaceId());
                if(Objects.nonNull(space) && Objects.nonNull(space.getConfigurations())&& Objects.nonNull(space.getConfigurations().getHoliday())){
                    int index = -1;
                    Holiday givenHoliday = holidayDto.getHolidays().get(0);
                    for(int i=0; i<space.getConfigurations().getHoliday().size(); i++){
                        Holiday holiday = space.getConfigurations().getHoliday().get(i);
                        if(Objects.equals(givenHoliday.getEventName(), holiday.getEventName())
                                && Objects.equals(givenHoliday.getDate(), holiday.getDate())
                                && Objects.equals(givenHoliday.getDescription(), holiday.getDescription())
                        ){
                            index = i ;
                            break;
                        }
                    }
                    if(index > -1){
                        space.getConfigurations().getHoliday().remove(index);
                        spaceRepository.save(space);

                    }
                }
            }
        } else {
            if(Objects.nonNull(holidayDto.getWorkspaceId())){
                Workspace space = workspaceRepository.findFirstById(holidayDto.getWorkspaceId());
                if(Objects.nonNull(space) && Objects.nonNull(space.getConfigurations())&& Objects.nonNull(space.getConfigurations().getHoliday())){
                    int index = -1;
                    Holiday givenHoliday = holidayDto.getHolidays().get(0);
                    for(int i=0; i<space.getConfigurations().getHoliday().size(); i++){
                        Holiday holiday = space.getConfigurations().getHoliday().get(i);
                        if(Objects.equals(givenHoliday.getEventName(), holiday.getEventName())
                                && Objects.equals(givenHoliday.getDate(), holiday.getDate())
                                && Objects.equals(givenHoliday.getDescription(), holiday.getDescription())
                        ){
                            index = i ;
                            break;
                        }
                    }
                    if(index > -1){
                        space.getConfigurations().getHoliday().remove(index);
                        workspaceRepository.save(space);

                    }
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getHolidayList(String spaceId) {
        List<Space> spaces = spaceRepository.findHolidayConfigurationsById(spaceId);
        if (Objects.nonNull(spaces) && !spaces.isEmpty() && Objects.nonNull(spaces.get(0).getConfigurations()) && Objects.nonNull(spaces.get(0).getConfigurations().getHoliday())) {
            return new ResponseEntity<>(spaces.get(0).getConfigurations().getHoliday(), HttpStatus.OK);
        }
        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
    }
}
