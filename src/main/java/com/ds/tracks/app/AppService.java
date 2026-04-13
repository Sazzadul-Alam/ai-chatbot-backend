package com.ds.tracks.app;

import org.springframework.http.ResponseEntity;

public interface AppService {
    
    ResponseEntity<?> analytics(AppRequest request);
    
    ResponseEntity<?> tasks(AppRequest request);

    ResponseEntity<?> issues(AppRequest request);

    ResponseEntity<?> backlogs(AppRequest request);

    ResponseEntity<?> findClientList();

    ResponseEntity<?> tasksList(AppRequest request);

    ResponseEntity<?> taskStatusPieChart(String spaceId);

    ResponseEntity<?> workloadChart(AppRequest request);

    ResponseEntity<?> configurations();

    ResponseEntity<?> invoiceList();
}
