package com.yolo.garbage.service;

import com.yolo.garbage.dao.UserMapper;
import com.yolo.garbage.model.User;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    public List<User> getAll() {
        return userMapper.selectAll();
    }

    public User getById(Integer id) {
        return userMapper.selectById(id);
    }

    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public int create(User user) {
        // 对密码进行BCrypt加密
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.insert(user);
    }

    public int update(User user) {
        // 对密码进行BCrypt加密（如果密码不为空）
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.update(user);
    }

    public int delete(Integer id) {
        return userMapper.delete(id);
    }

    public boolean login(String username, String password) {
        User user = getByUsername(username);
        if (user == null) {
            return false;
        }
        
        String storedPassword = user.getPassword();
        boolean matches;
        
        // 检查密码是否已经是 BCrypt 加密格式
        // 覆盖 $2a$, $2b$ 等常见前缀，确保所有 BCrypt 变体都被正确处理
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            // 使用 BCrypt 验证密码
            matches = passwordEncoder.matches(password, storedPassword);
        } else {
            // 返回验证失败
            matches = false;
        }
        return matches;
    }

    public int updatePassword(String oldPassword, String newPassword) {
        // 从SecurityContextHolder获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return 0;
        }
        
        String username = authentication.getName();
        User user = getByUsername(username);
        if (user == null) {
            return 0;
        }
        
        // 验证旧密码
        String storedPassword = user.getPassword();
        boolean matches;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            matches = passwordEncoder.matches(oldPassword, storedPassword);
        } else {
            // 返回验证失败
            matches = false;
        }
        
        if (!matches) {
            return 0;
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.update(user);
    }
}
