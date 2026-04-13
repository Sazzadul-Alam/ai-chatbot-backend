package com.ds.tracks.isage;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface IsageService {
    ResponseEntity<?> getCountryCode();
    ResponseEntity<?> saveRequest(Map<String, Object> request);

    ResponseEntity<?> saveConv(Map<String, Object> convStore,String loginId);
    ResponseEntity<?> getConv(String loginId);
}
