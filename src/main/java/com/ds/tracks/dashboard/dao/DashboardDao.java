package com.ds.tracks.dashboard.dao;

import com.ds.tracks.dashboard.model.DashboardDto;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface DashboardDao {

    Document generate( String spaceId, String startDate, String endDate, String defaultCapacity, String workdays);
    Document generateOverall(List<String> ids, String startDate, String endDate, String userId, String s);



    List<Document> workload(List<String> spaceIds, String startDate, String endDate, String currentUserId, String valueOf);
    List<Document> tasks(List<String> spaceIds, String startDate, String endDate, String currentUserId);
    Document effort(List<String> spaceIds, String startDate, String endDate, String currentUserId);
    Object tasksSummary(List<String> spaceIds, String currentUserId);

}
