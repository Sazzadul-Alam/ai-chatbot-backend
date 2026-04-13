package com.ds.tracks.app;

public interface AppDao {
    Object analytics(String workspace, String project, String format, String format1, String currentUserId, String workdays);

    Object tasks(AppRequest request, String currentUserId);

    Object issues(AppRequest request, String currentUserId);

    Object backlogs(AppRequest request, String currentUserId);
    Object tasksList(AppRequest request, String userId);

    Object taskStatusPieChart(String spaceId);

    Object workloadChart(String workspace, String project, String format, String format1, String currentUserId, String workdays);

    Object invoiceList();
}
