package com.yolo.garbage.ai.service;

import java.util.List;

import com.yolo.garbage.ai.model.AIModel;
import com.yolo.garbage.ai.model.ChatRecord;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIService {
    SseEmitter chat(AIModel model, String message, String username);
    SseEmitter generateSuggestions(AIModel model, String username);
    void saveChatRecord(String username, String model, String userMessage, String aiResponse);
    List<ChatRecord> getChatRecords(String username);
}