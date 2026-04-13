package com.ds.tracks.effort.dao;

import java.util.List;
import java.util.Map;

public interface EffortLogDao {

    List<Map<String, Object>> findEffortLogsById(String id, boolean isSubTask);

}
