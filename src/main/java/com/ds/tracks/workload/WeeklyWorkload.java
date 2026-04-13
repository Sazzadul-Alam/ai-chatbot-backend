package com.ds.tracks.workload;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWorkload {
    private String userId;
    private String image;
    private String userName;

    private DailyWorkload Saturday;
    private DailyWorkload Sunday;
    private DailyWorkload Monday;
    private DailyWorkload Tuesday;
    private DailyWorkload Wednesday;
    private DailyWorkload Thursday;
    private DailyWorkload Friday;

    private Double completion;
    private Double actualDuration;
    private Double capacityHour;
    private Double totalBookedHour;
}
