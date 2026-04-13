package com.ds.tracks.holiday.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Getter
@Setter
public class HolidayFailure {
    private Date date;
    private String eventName;
    private Integer rowNo;
    private String description;
    private String reason;
}
