package com.ds.tracks.holiday.service;

import com.ds.tracks.holiday.model.HolidayDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface HolidayService {
    ResponseEntity<String> save(HolidayDto holidayDto);
    ResponseEntity<?> upload(MultipartFile file, String source, String spaceId, String workspaceId);
    ResponseEntity<String> delete(HolidayDto id);

    ResponseEntity<?> getHolidayList(String spaceId);
}
