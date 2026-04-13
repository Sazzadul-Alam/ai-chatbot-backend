package com.ds.tracks.isage.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "country_code")
public class CountryCode {
    @Id
    private String id;
    private String country;
    private String countryCode;
    private String isoCodes;
}