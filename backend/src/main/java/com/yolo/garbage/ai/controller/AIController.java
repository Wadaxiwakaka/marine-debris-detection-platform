package com.yolo.garbage.ai.controller;

import com.yolo.garbage.ai.model.AIModel;
import com.yolo.garbage.ai.model.ChatRequest;
import com.yolo.garbage.ai.service.AIService;
import com.yolo.garbage.model.Response;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/ai")
public class AIController {
    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, HttpServletResponse response) {
        // 添加防止Nginx缓冲的响应头
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Transfer-Encoding", "chunked");
        
        String username = "user";
        AIModel model = AIModel.fromName(request.getModel());
        if (model == null) {
            model = AIModel.DEEPSEEK; // 默认使用DeepSeek模型
        }
        return aiService.chat(model, request.getMessage(), username);
    }

    @GetMapping("/chat-records")
    public Response getChatRecords(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return Response.success(new ArrayList<>());  // 返回空列表
        }

        String username = "";
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                username = (String) principal;
            } else if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            }
        } catch (Exception e) {
            System.err.println("获取用户名失败: " + e.getMessage());
        }
        return Response.success(aiService.getChatRecords(username));
    }

    @PostMapping(value = "/suggestions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSuggestions(@RequestBody ChatRequest request, HttpServletResponse response) {
        // 添加防止Nginx缓冲的响应头
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Transfer-Encoding", "chunked");
        
        String username = "user";
        AIModel model = AIModel.fromName(request.getModel());
        if (model == null) {
            model = AIModel.DEEPSEEK; // 默认使用DeepSeek模型
        }
        return aiService.generateSuggestions(model, username);
    }
}