package com.ds.tracks.workload;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyWorkload {
    private Double barHeight = 100D;
    private Double engagedHour = 0D;
    private Double extraHour = 0D;
    private LoadType loadType = LoadType.empty;
}
