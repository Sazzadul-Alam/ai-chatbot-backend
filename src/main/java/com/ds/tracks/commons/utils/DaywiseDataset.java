package com.ds.tracks.commons.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DaywiseDataset {
    private Double sunday = 0.0;
    private Double monday = 0.0;
    private Double tuesday = 0.0;
    private Double wednesday = 0.0;
    private Double thursday = 0.0;
    private Double friday = 0.0;
    private Double saturday = 0.0;
    private Double bookedHour = 0.0;
    private Double totalBookedHour = 0.0;
    private Double totalActualDuration = 0.0;
    private Double totalCompletion = 0.0;
    private Double numberOfUnit = 0.0;

    public Map<String,Object> getDayData(List<Map<String,Object>> dataList, Double workinghour){
        String userId="";
        String userName="";
        Integer capacityHour=0;
        String image="";
        for(Map<String, Object> data:dataList){
            userId=(data.containsKey("userId") ? (String)data.get("userId") : null);
            userName=(data.containsKey("userName") ? (String)data.get("userName") : null);
            capacityHour=(data.containsKey("capacityHour") ? (Integer)data.get("capacityHour") : 0);
            image=(data.containsKey("image") ? (String)data.get("image") : null);
            totalBookedHour=totalBookedHour+(data.containsKey("totalBookedHour") ? (Double)data.get("totalBookedHour") : 0.0);
            totalActualDuration=totalActualDuration+(data.containsKey("actualDuration") ? (Double)data.get("actualDuration") : 0.0);
            totalCompletion=totalCompletion+(data.containsKey("completion") ? (Double)data.get("completion") : 0.0);
            numberOfUnit=numberOfUnit+1;
            data.keySet().forEach(key->{
                switch (key) {
                    case "Sunday":
                        sunday = sunday + (Double)data.get(key);
                        break;
                    case "Monday":
                        monday = monday + (Double)data.get(key);
                        break;
                    case "Tuesday":
                        tuesday = tuesday + (Double)data.get(key);
                        break;
                    case "Wednesday":
                        wednesday = wednesday + (Double)data.get(key);
                        break;
                    case "Thursday":
                        thursday = thursday + (Double)data.get(key);
                        break;
                    case "Friday":
                        friday = friday + (Double)data.get(key);
                        break;
                    case "Saturday":
                        saturday = saturday + (Double)data.get(key);
                        break;
                    default:
                        break;
                }
            });

        }
        Map<String,Object> map =new HashMap<>();
        Map<String,Object> childMap =new HashMap<>();
        map.put("userId",userId);
        map.put("userName",userName);
        map.put("capacityHour",capacityHour);
        map.put("image",image);
        map.put("totalBookedHour",totalBookedHour);
        String loadType=this.getLoadType(sunday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,sunday)));
        childMap.put("engagedHour",sunday);
        childMap.put("extraHour",this.getExtraHour(sunday,workinghour));
        map.put("Sunday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(monday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,monday)));
        childMap.put("extraHour",this.getExtraHour(monday,workinghour));
        childMap.put("engagedHour",monday);
        map.put("Monday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(tuesday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,tuesday)));
        childMap.put("extraHour",this.getExtraHour(tuesday,workinghour));
        childMap.put("engagedHour",tuesday);
        map.put("Tuesday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(wednesday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,wednesday)));
        childMap.put("extraHour",this.getExtraHour(wednesday,workinghour));
        childMap.put("engagedHour",wednesday);
        map.put("Wednesday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(thursday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,thursday)));
        childMap.put("extraHour",this.getExtraHour(thursday,workinghour));
        childMap.put("engagedHour",thursday);
        map.put("Thursday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(friday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,friday)));
        childMap.put("extraHour",this.getExtraHour(friday,workinghour));
        childMap.put("engagedHour",friday);
        map.put("Friday",childMap);
        childMap =new HashMap<>();
        loadType=this.getLoadType(saturday,workinghour);
        childMap.put("loadType",loadType);
        childMap.put("barHeight",(loadType.equals("over") || loadType.equals("full"))?100:Math.min(100, this.getBarHeight(workinghour,saturday)));
        childMap.put("extraHour",this.getExtraHour(saturday,workinghour));
        childMap.put("engagedHour",saturday);
        map.put("Saturday",childMap);
        map.put("actualDuration",totalActualDuration);
        map.put("completion",totalCompletion/numberOfUnit);
        return map;
    }

    public String getLoadType(Double bookedHour,Double workingHour){
        if(bookedHour > workingHour){
            return "over";
        }else if(bookedHour.equals(workingHour)){
            return "full";
        }else if(bookedHour < workingHour && bookedHour > 0 ){
            return"under";
        }else{
            return"empty";
        }
    }

    private Double getExtraHour(Double bookedHour, Double workingHour){
        return (bookedHour-workingHour)>0?Math.round(bookedHour-workingHour):0.0;
    }
    private Double getBarHeight(Double workingHour,Double bookedHour){
        return (bookedHour/workingHour)*100;
    }

}
