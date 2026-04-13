package com.ds.tracks.holiday.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
public class Holiday {
    private Date date;
    private String eventName;
    private String description;
}