package com.yolo.garbage.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

public class ValidationUtil {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,50}$");
    private static final Pattern ROLE_PATTERN = Pattern.compile("^(admin|user)$");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    public static boolean validateUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    public static boolean validatePassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static boolean validateRole(String role) {
        return role != null && ROLE_PATTERN.matcher(role).matches();
    }
    
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // 处理带时区的时间字符串，移除Z后缀
            String cleanStr = dateTimeStr.replace("Z", "");
            return LocalDateTime.parse(cleanStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static boolean validateGarbageTypeCondition(Map<String, Object> condition) {
        if (condition == null) {
            return true;
        }
        
        // 验证typeId
        if (condition.containsKey("typeId")) {
            Object typeIdObj = condition.get("typeId");
            if (!(typeIdObj instanceof Integer)) {
                return false;
            }
        }
        
        // 验证时间范围
        if (condition.containsKey("startTime")) {
            Object startTimeObj = condition.get("startTime");
            if (startTimeObj != null && !(startTimeObj instanceof LocalDateTime)) {
                return false;
            }
        }
        
        if (condition.containsKey("endTime")) {
            Object endTimeObj = condition.get("endTime");
            if (endTimeObj != null && !(endTimeObj instanceof LocalDateTime)) {
                return false;
            }
        }
        
        // 验证经纬度范围
        if (condition.containsKey("minLatitude")) {
            Object minLatObj = condition.get("minLatitude");
            if (!(minLatObj instanceof Double)) {
                return false;
            }
        }
        
        if (condition.containsKey("maxLatitude")) {
            Object maxLatObj = condition.get("maxLatitude");
            if (!(maxLatObj instanceof Double)) {
                return false;
            }
        }
        
        if (condition.containsKey("minLongitude")) {
            Object minLonObj = condition.get("minLongitude");
            if (!(minLonObj instanceof Double)) {
                return false;
            }
        }
        
        if (condition.containsKey("maxLongitude")) {
            Object maxLonObj = condition.get("maxLongitude");
            if (!(maxLonObj instanceof Double)) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean validateId(Integer id) {
        return id != null && id > 0;
    }
    
    public static boolean validateLongId(Long id) {
        return id != null && id > 0;
    }
}