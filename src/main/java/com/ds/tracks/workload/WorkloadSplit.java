package com.ds.tracks.workload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
public class WorkloadSplit {
    public Date updateStartDate;
    public Date updateDeadline;
    public Double updateDuration;
    public Date nextStartDate;
    public Date nextDeadline;
    public Double nextDuration;
    public String nextIssuedTo;
    public Boolean isNextSplitExecuted;
}
