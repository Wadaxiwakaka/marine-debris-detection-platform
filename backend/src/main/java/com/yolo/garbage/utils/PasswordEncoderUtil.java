package com.yolo.garbage.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        
        // 验证密码
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Password matches: " + matches);
        
        // 为 user 用户生成密码
        String userRawPassword = "user123";
        String userEncodedPassword = encoder.encode(userRawPassword);
        System.out.println("\nUser raw password: " + userRawPassword);
        System.out.println("User encoded password: " + userEncodedPassword);
        
        // 验证 user 密码
        boolean userMatches = encoder.matches(userRawPassword, userEncodedPassword);
        System.out.println("User password matches: " + userMatches);
    }
}