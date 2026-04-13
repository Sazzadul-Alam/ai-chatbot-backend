package com.ds.tracks.commons.utils;

import com.ds.tracks.commons.models.KeyValuePair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {
    public static boolean isValidEmail(String susEmail){
        return Pattern.compile("^(.+)@(.+)$").matcher(susEmail).matches();
    }
    public static boolean isValidString(String susText){
        return Objects.nonNull(susText) && !susText.trim().isEmpty();
    }
    public static String randomIdGenerate(){
        String strNum = "";
        for(int i = 0; i < 4; i++) {
            strNum = strNum + new Random().nextInt(10);
        }
        return strNum;
    }

    public static Date addHoursToJavaUtilDate(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    public static String generateTaskId(String spaceMnemonic, String type){
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        StringBuilder typeMnemonic = new StringBuilder();
        if(isValidString(type)){
            String[] typeWords = type.trim().split(" ");
            for(int i=0; i< typeWords.length ; i++){
                typeMnemonic.append(typeWords[i].charAt(0));
            }
        }

        return new StringBuilder()
                .append(spaceMnemonic.toUpperCase()).append("-")
                .append(typeMnemonic.toString().toUpperCase()).append(sdf.format(new Date())).append("-")
                .append(randomIdGenerate()).toString();
    }

    public static String spaceSubSpaceAggStage(String spaceId, String subSpaceId){
        if (Objects.isNull(subSpaceId) || subSpaceId.equals("")) return  "{$match: {spaceId:'" + spaceId + "', subSpaceId:{$exists:false}}},";
        else return "{$match: {subSpaceId:'" + subSpaceId + "'}},";
    }

    public static Double calculateStoryPoint(Double value) {
        if(Objects.isNull(value)){
            return 0D;
        }
        Double root = 16.66;
        if(value<root){
            return 0D;
        } else if(value<(root*2)){
            return 1D;
        } else if(value<(root*3)){
            return 2D;

        } else if(value<(root*4)){
            return 3D;

        } else if(value<(root*5)){
            return 5D;

        } else if(value<(root*6)){
            return 8D;

        } else {
            return 13D;
        }
    }

    public static String dateRange(String startDate, String endDate){
        return "{'$gte':ISODate('"+startDate+"T00:00:00.000+06:00'), '$lte':ISODate('"+endDate+"T23:59:59.999+06:00')}";
    }
    public static String quotedString(String value){
        return "\""+value+"\"";
    }

    public static KeyValuePair createRelation(String key, Object value){
        return new KeyValuePair(key, value);
    }
    public static Map<String, Object> noDataResponse(){
        Map<String, Object> response = new HashMap<>();
        response.put("data", Collections.EMPTY_LIST);
        response.put("totalData", 0);
        return response;
    }

    public static String listToStringQuery(String field, List<?> input){
        String query = "";
        StringBuilder rString = new StringBuilder();
        if(Objects.nonNull(input) && !input.isEmpty()){
            for (Object each : input) {
                rString.append("'").append(each).append("', ");
            }
            query = "'"+field+"':{ $in:["+rString.toString()+" ] },";
        }
        return query;
    }
    public static long daysBetween(Date firstDate, Date secondDate) {
        return ChronoUnit.DAYS.between(firstDate.toInstant(), secondDate.toInstant());
    }
    public static ResponseEntity<?> unauthorized() {
        return new ResponseEntity<>("You don't have permission permission to perform this action", HttpStatus.UNAUTHORIZED);
    }
    public static ResponseEntity<?> exception() {
        return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<?> badRequest() {
        return new ResponseEntity<>("Invalid Parameters", HttpStatus.BAD_REQUEST);
    }
    public static ResponseEntity<?> forbidden() {
        return new ResponseEntity<>("You don't have permission permission to perform this action", HttpStatus.FORBIDDEN);
    }


    public static String sourceToCollectionName(String source){
        return Objects.equals(source,  "task") ? CollectionName.task : Objects.equals(source,  "space") ? CollectionName.spaces : null;
    }

}
