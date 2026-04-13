package com.ds.tracks.commons.models.strings;

public class TasksStatus {
    // PROGRESS STATUS
    public static String NEW = "New";
    public static String PENDING = "Pending";
    public static String IN_PROGRESS = "In Progress";
    public static String COMPLETE = "Complete";
    public static String IN_REVIEW = "In Review";
    public static String CLOSED = "Closed";

    // History Status
    public static String TASK_CREATED = "Created Task";
    public static String SUB_TASK_CREATED = "Created SubTask";
    public static String CHANGE_STATUS(String to){
     return "Changed Status to "+to;
    }

}
