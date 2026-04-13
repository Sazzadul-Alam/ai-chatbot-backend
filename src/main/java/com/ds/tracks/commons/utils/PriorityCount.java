package com.ds.tracks.commons.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriorityCount {
    private Integer urgent = 0;
    private Integer high = 0;
    private Integer normal = 0;
    private Integer low = 0;
    private Integer noPriority = 0;

    public void setPriority(Integer priority) {
        switch (priority) {
            case 1:
                urgent = urgent + 1;
                break;
            case 2:
                high = high + 1;
                break;
            case 3:
                normal = normal + 1;
                break;
            case 4:
                low = low + 1;
                break;
            default:
                noPriority = noPriority + 1;
                break;
        }
    }

    public List<Map<String, Object>> getPriority() {
        List<Map<String, Object>> countList = new ArrayList<>();
        Map<String, Object> countMap = null;

        if (this.urgent > 0) {
            countMap = new HashMap<>();
            countMap.put("name", this.urgent);
            countMap.put("id", 1);
            countList.add(countMap);
        }
        if (this.high > 0) {
            countMap = new HashMap<>();
            countMap.put("name", this.high);
            countMap.put("id", 2);
            countList.add(countMap);
        }
        if (this.normal > 0) {
            countMap = new HashMap<>();
            countMap.put("name", this.normal);
            countMap.put("id", 3);
            countList.add(countMap);
        }
        if (this.low > 0) {
            countMap = new HashMap<>();
            countMap.put("name", this.low);
            countMap.put("id", 4);
            countList.add(countMap);
        }
        if (this.noPriority > 0) {
            countMap=new HashMap<>();
            countMap.put("name", this.noPriority);
            countMap.put("id", 5);
            countList.add(countMap);
        }
        return countList;
    }
}
