package com.yolo.garbage.controller;

import com.yolo.garbage.model.Response;
import com.yolo.garbage.model.User;
import com.yolo.garbage.service.UserService;
import com.yolo.garbage.utils.ValidationUtil;
import com.yolo.garbage.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    // 图片存储路径，使用绝对路径确保目录存在
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
    // 图片访问基础URL
    private static final String BASE_URL = "/uploads/";

    @GetMapping
    public Response<List<User>> getAll() {
        List<User> users = userService.getAll();
        return Response.success(users);
    }

    @GetMapping("/{id}")
    public Response<User> getById(@PathVariable Integer id) {
        if (!ValidationUtil.validateId(id)) {
            return Response.error("用户ID格式错误");
        }
        User user = userService.getById(id);
        if (user == null) {
            return Response.error("用户不存在");
        }
        return Response.success(user);
    }

    @PostMapping("/login")
    public Response<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        logger.info("Login attempt received: username={}, password length={}", username, password != null ? password.length() : 0);
        
        if (!ValidationUtil.validateUsername(username)) {
            logger.warn("Login failed: invalid username format - {}", username);
            return Response.error("用户名格式错误");
        }
        if (!ValidationUtil.validatePassword(password)) {
            logger.warn("Login failed: invalid password format - length={}", password != null ? password.length() : 0);
            return Response.error("密码格式错误");
        }
        
        logger.info("Validation passed, attempting to authenticate user: {}", username);
        boolean success = userService.login(username, password);
        logger.info("Login attempt result: username={}, success={}", username, success);
        
        if (!success) {
            logger.warn("Login failed: invalid username or password - {}", username);
            return Response.error("用户名或密码错误");
        }
        
        User user = userService.getByUsername(username);
        if (user == null) {
            logger.error("Login failed: user not found after successful authentication - {}", username);
            return Response.error("用户不存在");
        }
        
        logger.info("Login successful: user found - id={}, username={}", user.getId(), user.getUsername());
        // 生成JWT token
        String token = jwtUtil.generateToken(username);
        // 清除密码信息
        user.setPassword(null);
        // 构建响应数据
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", user);
        responseData.put("token", token);
        logger.info("Login completed successfully for user: {}", username);
        return Response.success(responseData);
    }

    @PostMapping("/register")
    public Response<Integer> create(@RequestBody User user) {
        if (!ValidationUtil.validateUsername(user.getUsername())) {
            return Response.error("用户名格式错误");
        }
        if (!ValidationUtil.validatePassword(user.getPassword())) {
            return Response.error("密码格式错误");
        }
        if (user.getRole() != null && !ValidationUtil.validateRole(user.getRole())) {
            return Response.error("角色格式错误");
        }
        // 检查用户名是否已存在
        User existingUser = userService.getByUsername(user.getUsername());
        if (existingUser != null) {
            return Response.error("用户名已存在");
        }
        int result = userService.create(user);
        return Response.success(result);
    }

    @PutMapping
    public Response<Integer> update(@RequestBody User user) {
        if (!ValidationUtil.validateId(user.getId())) {
            return Response.error("用户ID格式错误");
        }
        if (user.getUsername() != null && !ValidationUtil.validateUsername(user.getUsername())) {
            return Response.error("用户名格式错误");
        }
        if (user.getPassword() != null && !ValidationUtil.validatePassword(user.getPassword())) {
            return Response.error("密码格式错误");
        }
        if (user.getRole() != null && !ValidationUtil.validateRole(user.getRole())) {
            return Response.error("角色格式错误");
        }
        int result = userService.update(user);
        return Response.success(result);
    }

    @DeleteMapping("/{id}")
    public Response<Integer> delete(@PathVariable Integer id) {
        if (!ValidationUtil.validateId(id)) {
            return Response.error("用户ID格式错误");
        }
        int result = userService.delete(id);
        return Response.success(result);
    }

    @PutMapping("/password")
    public Response<Integer> updatePassword(@RequestBody Map<String, String> passwordData) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (!ValidationUtil.validatePassword(oldPassword)) {
            return Response.error("旧密码格式错误");
        }
        if (!ValidationUtil.validatePassword(newPassword)) {
            return Response.error("新密码格式错误");
        }
        int result = userService.updatePassword(oldPassword, newPassword);
        if (result == 0) {
            return Response.error("旧密码错误");
        }
        return Response.success(result);
    }
    
    @PostMapping("/avatar")
    public Response<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("userId") Integer userId) {
        if (file.isEmpty()) {
            return Response.error("文件为空");
        }
        
        if (!ValidationUtil.validateId(userId)) {
            return Response.error("用户ID格式错误");
        }
        
        try {
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID() + extension;
            
            // 保存文件
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);
            
            // 更新用户头像信息
            User user = userService.getById(userId);
            if (user == null) {
                return Response.error("用户不存在");
            }
            
            String fileUrl = BASE_URL + fileName;
            user.setAvatar(fileUrl);
            // 重要：将密码字段设置为null，避免再次加密导致密码被修改
            user.setPassword(null);
            userService.update(user);
            
            // 返回文件访问URL
            return Response.success(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.error("文件上传失败：" + e.getMessage());
        }
    }
}
