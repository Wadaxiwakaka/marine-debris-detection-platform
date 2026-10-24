package com.yolo.garbage.ai.service;

import com.yolo.garbage.ai.config.AIConfig;
import com.yolo.garbage.ai.model.AIModel;
import com.yolo.garbage.ai.model.ChatRecord;
import com.yolo.garbage.model.User;
import com.yolo.garbage.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class AIServiceImpl implements AIService {
    private final AIConfig aiConfig;
    private final UserService userService;
    private final ConcurrentHashMap<String, List<ChatRecord>> chatRecords = new ConcurrentHashMap<>();

    @Autowired
    public AIServiceImpl(AIConfig aiConfig, UserService userService) {
        this.aiConfig = aiConfig;
        this.userService = userService;
    }

    @Override
    public SseEmitter chat(AIModel model, String message, String username) {
        // 1. 创建 SseEmitter，0L 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 2. 必须开启一个异步线程去请求大模型，千万不能阻塞当前的主线程！
        CompletableFuture.runAsync(() -> {
            try {
                System.err.println("开始处理聊天请求，模型: " + model.getName() + ", 用户名: " + username);
                String systemPrompt = readSystemPrompt();

                // 根据模型类型调用相应的API
                String apiKey = "";
                String apiUrl = "";
                String modelName = "";

                switch (model) {
                    case DEEPSEEK:
                        apiKey = aiConfig.getDeepSeekApiKey();
                        apiUrl = "https://api.deepseek.com/chat/completions";
                        modelName = "deepseek-v4-flash";
                        break;
                    case GLM_4_7_FLASH:
                        apiKey = aiConfig.getGlmApiKey();
                        apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
                        modelName = "glm-4.7-flash";
                        break;
                    default:
                        // 默认使用DeepSeek
                        apiKey = aiConfig.getDeepSeekApiKey();
                        apiUrl = "https://api.deepseek.com/chat/completions";
                        modelName = "deepseek-v4-flash";
                }

                System.err.println("模型: " + model.getName() + ", API密钥: " + apiKey + ", API URL: " + apiUrl);

                // 检查API密钥是否有效
                if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your-")) {
                    String errorMessage = "抱歉，AI服务未配置，请联系管理员设置API密钥。";
                    System.err.println("API密钥无效，返回错误信息: " + errorMessage);
                    emitter.send(errorMessage);
                    emitter.complete();
                    return;
                }

                // 构建请求体
                String requestBody = "{" +
                        '"' + "model" + '"' + ": " + '"' + modelName + '"' + "," +
                        '"' + "messages" + '"' + ": [" +
                        "{" + '"' + "role" + '"' + ": " + '"' + "system" + '"' + ", " + '"' +
                        "content" + '"' + ": " + '"' + systemPrompt.replace("\"", "\\\"") + '"' +
                        "}," +
                        "{" + '"' + "role" + '"' + ": " + '"' + "user" + '"' + ", " + '"' + "content"
                        + '"' + ": " + '"' + message.replace("\"", "\\\"") + '"' + "}" +
                        "] ," +
                        '"' + "stream" + '"' + ": true" +
                        "}";

                // 发送HTTP请求
                URI uri = new URI(apiUrl);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setChunkedStreamingMode(0);
                conn.setUseCaches(false);

                // 写入请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                } catch (IOException e) {
                    System.err.println("写入请求体失败: " + e.getMessage());
                    emitter.send("抱歉，写入请求体失败:稍后重试。");
                }

                // 读取响应 (强烈建议使用 BufferedReader，防止乱码和截断)
                try (InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty())
                            continue; // 跳过空行

                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if (data.equals("[DONE]")) {
                                break; // 读取结束
                            }

                            try {
                                JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();

                                // 检查是否有错误
                                if (jsonObject.has("error")) {
                                    JsonObject error = jsonObject.getAsJsonObject("error");
                                    String errorMessage = error.has("message") ? error.get("message").getAsString()
                                            : "API调用失败";
                                    System.err.println("API错误: " + errorMessage);
                                    emitter.send("抱歉，AI服务调用失败: " + errorMessage);
                                    break;
                                }

                                // 格式1: OpenAI兼容格式 (DeepSeek, Kimi)
                                if (jsonObject.has("choices")) {
                                    JsonObject choice = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject();
                                    if (choice.has("delta")) {
                                        JsonObject delta = choice.getAsJsonObject("delta");
                                        // 创建一个新的JSON对象，包含content和reasoning_content字段
                                        JsonObject responseDelta = new JsonObject();
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            // 当delta.get("content")不为null时，delta.get("reasoning_content")为空时，是模型生成的内容
                                            responseDelta.addProperty("content", delta.get("content").getAsString());
                                            responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                        } else if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                                            // 当delta.get("content")为null时，delta.get("reasoning_content")不为空时，是模型思考过程
                                            responseDelta.add("content", com.google.gson.JsonNull.INSTANCE);
                                            responseDelta.addProperty("reasoning_content", delta.get("reasoning_content").getAsString());
                                        }
                                        if (responseDelta.size() > 0) {
                                            // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                            String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                            System.err.println("发送内容: " + safeContent);
                                            emitter.send(safeContent);
                                        }
                                    } else if (choice.has("message")) {
                                        JsonObject choiceMessage = choice.getAsJsonObject("message");
                                        if (choiceMessage.has("content") && !choiceMessage.get("content").isJsonNull()) {
                                            // 非流式响应格式
                                            JsonObject responseDelta = new JsonObject();
                                            responseDelta.addProperty("content", choiceMessage.get("content").getAsString());
                                            responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                            // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                            String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                            System.err.println("发送内容: " + safeContent);
                                            emitter.send(safeContent);
                                        } else System.err.println("测试message:" + choiceMessage);
                                    }
                                } else if (jsonObject.has("data")) {
                                    // 格式2: 其他可能的格式
                                    JsonObject responseDelta = new JsonObject();
                                    responseDelta.addProperty("content", jsonObject.get("data").getAsString());
                                    responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                    // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                    String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                    System.err.println("发送内容: " + safeContent);
                                    emitter.send(safeContent);
                                }
                            } catch (Exception e) {
                                // 打印错误信息和原始数据用于调试
                                System.err.println("JSON解析失败: " + e.getMessage());
                                System.err.println("原始数据: " + data);
                                // JSON解析失败时，返回友好的错误信息
                                emitter.send("抱歉，解析AI响应时出现错误，请稍后重试。");
                                // 不再继续处理当前响应
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // 处理HTTP错误，尝试读取错误响应
                    try {
                        int responseCode = conn.getResponseCode();
                        System.err.println("HTTP错误代码: " + responseCode);
                        // 尝试读取错误响应
                        try (BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                            StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorResponse.append(errorLine);
                            }
                            System.err.println("错误响应: " + errorResponse.toString());
                            // 尝试解析错误响应
                            try {
                                JsonObject errorJson = JsonParser.parseString(errorResponse.toString())
                                        .getAsJsonObject();
                                if (errorJson.has("error")) {
                                    JsonObject error = errorJson.getAsJsonObject("error");
                                    String errorMessage = error.has("message") ? error.get("message").getAsString()
                                            : "API调用失败";
                                    emitter.send("抱歉，AI服务调用失败: " + errorMessage);
                                } else {
                                    emitter.send("抱歉，AI服务调用失败，HTTP错误代码: " + responseCode);
                                }
                            } catch (Exception ex) {
                                emitter.send("抱歉，AI服务调用失败，HTTP错误代码: " + responseCode);
                            }
                        }
                    } catch (Exception ex) {
                        throw e;
                    }
                }

                // 保存聊天记录
                saveChatRecord(username, model.getName(), message, "API response");

                emitter.complete();
            } catch (Exception e) {
                System.err.println("发生异常: " + e.getMessage());
                e.printStackTrace();
                try {
                    // 发生错误时，发送友好提示给前端，并关闭流
                    emitter.send("抱歉，AI 服务暂时不可用，请稍后重试。");
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        // 3. 立即将 emitter 返回给 Controller，主线程直接释放！
        return emitter;
    }

    @Override
    public SseEmitter generateSuggestions(AIModel model, String username) {
        // 1. 创建 SseEmitter，0L 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 2. 必须开启一个异步线程去请求大模型，千万不能阻塞当前的主线程！
        CompletableFuture.runAsync(() -> {
            try {
                System.err.println("开始处理聊天请求，模型: " + model.getName() + ", 用户名: " + username);
                String systemPrompt = readSystemPrompt();

                // 根据模型类型调用相应的API
                String apiKey = "";
                String apiUrl = "";
                String modelName = "";

                switch (model) {
                    case DEEPSEEK:
                        apiKey = aiConfig.getDeepSeekApiKey();
                        apiUrl = "https://api.deepseek.com/chat/completions";
                        modelName = "deepseek-v4-flash";
                        break;
                    case GLM_4_7_FLASH:
                        apiKey = aiConfig.getGlmApiKey();
                        apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
                        modelName = "glm-4.7-flash";
                        break;
                    default:
                        // 默认使用DeepSeek
                        apiKey = aiConfig.getDeepSeekApiKey();
                        apiUrl = "https://api.deepseek.com/chat/completions";
                        modelName = "deepseek-v4-flash";
                }

                System.err.println("模型: " + model.getName() + ", API密钥: " + apiKey + ", API URL: " + apiUrl);

                // 检查API密钥是否有效
                if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your-")) {
                    String errorMessage = "抱歉，AI服务未配置，请联系管理员设置API密钥。";
                    System.err.println("API密钥无效，返回错误信息: " + errorMessage);
                    emitter.send(errorMessage);
                    emitter.complete();
                    return;
                }

                // 构建请求体，生成网站改进建议
                String requestBody = "{" +
                        '"' + "model" + '"' + ": " + '"' + modelName + '"' + "," +
                        '"' + "messages" + '"' + ": [" +
                        "{" + '"' + "role" + '"' + ": " + '"' + "system" + '"' + ", " + '"' + "content" + '"' + ": "
                        + '"' + systemPrompt.replace("\"", "\\\"") + '"' + "}," +
                        "{" + '"' + "role" + '"' + ": " + '"' + "user" + '"' + ", " + '"' + "content" + '"' + ": " + '"'
                        + "请生成关于OceanGuard YOLO系统的网站改进建议，包括功能优化、用户体验、性能等方面。" + '"' + "}" +
                        "]," +
                        '"' + "stream" + '"' + ": true" +
                        "}";

                // 发送HTTP请求
                URI uri = new URI(apiUrl);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setChunkedStreamingMode(0);
                conn.setUseCaches(false);

                // 写入请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                } catch (IOException e) {
                    System.err.println("写入请求体失败: " + e.getMessage());
                    emitter.send("抱歉，写入请求体失败:稍后重试。");
                }

                // 读取响应
                try (InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty())
                            continue; // 跳过空行

                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if (data.equals("[DONE]")) {
                                break; // 读取结束
                            }

                            try {
                                JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();

                                // 检查是否有错误
                                if (jsonObject.has("error")) {
                                    JsonObject error = jsonObject.getAsJsonObject("error");
                                    String errorMessage = error.has("message") ? error.get("message").getAsString()
                                            : "API调用失败";
                                    System.err.println("API错误: " + errorMessage);
                                    emitter.send("抱歉，AI服务调用失败: " + errorMessage);
                                    break;
                                }

                                String content = null;
                                // 格式1: OpenAI兼容格式 (DeepSeek, Kimi)
                                if (jsonObject.has("choices")) {
                                    JsonObject choice = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject();
                                    if (choice.has("delta")) {
                                        JsonObject delta = choice.getAsJsonObject("delta");
                                        // 创建一个新的JSON对象，包含content和reasoning_content字段
                                        JsonObject responseDelta = new JsonObject();
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            // 当delta.get("content")不为null时，delta.get("reasoning_content")为空时，是模型生成的内容
                                            responseDelta.addProperty("content", delta.get("content").getAsString());
                                            responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                        } else if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                                            // 当delta.get("content")为null时，delta.get("reasoning_content")不为空时，是模型思考过程
                                            responseDelta.add("content", com.google.gson.JsonNull.INSTANCE);
                                            responseDelta.addProperty("reasoning_content", delta.get("reasoning_content").getAsString());
                                        }
                                        if (responseDelta.size() > 0) {
                                            // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                            String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                            System.err.println("发送内容: " + safeContent);
                                            emitter.send(safeContent);
                                        }
                                    } else if (choice.has("message")) {
                                        JsonObject choiceMessage = choice.getAsJsonObject("message");
                                        if (choiceMessage.has("content") && !choiceMessage.get("content").isJsonNull()) {
                                            // 非流式响应格式
                                            JsonObject responseDelta = new JsonObject();
                                            responseDelta.addProperty("content", choiceMessage.get("content").getAsString());
                                            responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                            // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                            String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                            System.err.println("发送内容: " + safeContent);
                                            emitter.send(safeContent);
                                        }
                                    }
                                } else if (jsonObject.has("data")) {
                                    // 格式2: 其他可能的格式
                                    JsonObject responseDelta = new JsonObject();
                                    responseDelta.addProperty("content", jsonObject.get("data").getAsString());
                                    responseDelta.add("reasoning_content", com.google.gson.JsonNull.INSTANCE);
                                    // 把 \n 替换成特殊标记，防止被 Spring 吞掉
                                    String safeContent = responseDelta.toString().replace("\n", "[NEWLINE]");
                                    System.err.println("发送内容: " + safeContent);
                                    emitter.send(safeContent);
                                }
                            } catch (Exception e) {
                                // 打印错误信息和原始数据用于调试
                                System.err.println("JSON解析失败: " + e.getMessage());
                                System.err.println("原始数据: " + data);
                                // JSON解析失败时，返回友好的错误信息
                                emitter.send("抱歉，解析AI响应时出现错误，请稍后重试。");
                                // 不再继续处理当前响应
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // 处理HTTP错误，尝试读取错误响应
                    try {
                        int responseCode = conn.getResponseCode();
                        System.err.println("HTTP错误代码: " + responseCode);
                        // 尝试读取错误响应
                        try (BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                            StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorResponse.append(errorLine);
                            }
                            System.err.println("错误响应: " + errorResponse.toString());
                            // 尝试解析错误响应
                            try {
                                JsonObject errorJson = JsonParser.parseString(errorResponse.toString())
                                        .getAsJsonObject();
                                if (errorJson.has("error")) {
                                    JsonObject error = errorJson.getAsJsonObject("error");
                                    String errorMessage = error.has("message") ? error.get("message").getAsString()
                                            : "API调用失败";
                                    emitter.send("抱歉，AI服务调用失败: " + errorMessage);
                                } else {
                                    emitter.send("抱歉，AI服务调用失败，HTTP错误代码: " + responseCode);
                                }
                            } catch (Exception ex) {
                                emitter.send("抱歉，AI服务调用失败，HTTP错误代码: " + responseCode);
                            }
                        }
                    } catch (Exception ex) {
                        throw e;
                    }
                }
                // 关闭流
                emitter.complete();
            } catch (Exception e) {
                System.err.println("发生异常: " + e.getMessage());
                e.printStackTrace();
                try {
                    // 发生错误时，发送友好提示给前端，并关闭流
                    emitter.send("抱歉，生成建议失败，请稍后重试。");
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        // 3. 立即将 emitter 返回给 Controller，主线程直接释放！
        return emitter;
    }

    @Override
    public void saveChatRecord(String username, String model, String userMessage, String aiResponse) {
        ChatRecord record = new ChatRecord();
        record.setId(System.currentTimeMillis());
        // 使用真实的用户ID
        User user = userService.getByUsername(username);
        record.setUserId(user != null ? user.getId().longValue() : 1L);
        record.setModel(model);
        record.setUserMessage(userMessage);
        record.setAiResponse(aiResponse);
        record.setCreatedAt(LocalDateTime.now());

        chatRecords.computeIfAbsent(username, k -> new ArrayList<>()).add(record);
    }

    @Override
    public List<ChatRecord> getChatRecords(String username) {
        return chatRecords.getOrDefault(username, new ArrayList<>());
    }

    private String readSystemPrompt() {
        try (BufferedReader reader = new BufferedReader(new FileReader(aiConfig.getSystemPromptFile()))) {
            StringBuilder prompt = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                prompt.append(line).append("\n");
            }
            // 过滤控制字符
            return filterControlCharacters(prompt.toString());
        } catch (IOException e) {
            // 文件读取失败时使用默认提示词
            String defaultPrompt = "你是OceanGuard YOLO系统的智能助手，负责回答用户关于网站使用的问题。\n\nOceanGuard YOLO是一个基于YOLO的海漂垃圾识别与可视化系统，主要功能包括：\n1. 首页：展示系统概览和主要功能入口\n2. 垃圾数据：展示垃圾数据列表和详情\n3. 数据统计：展示垃圾数据的统计分析\n4. 垃圾识别：上传图片进行垃圾识别\n5. 用户管理：仅管理员可访问，管理系统用户\n6. 关于系统：系统介绍和使用说明\n7. AI助手：帮助用户了解网站，给管理员提供建议\n\n请以友好、专业的语气回答用户问题，帮助用户了解和使用系统的各项功能。\n\n如果用户的问题与系统功能无关，请礼貌地告知用户你只能回答与系统相关的问题。";
            // 过滤控制字符
            return filterControlCharacters(defaultPrompt);
        }
    }

    private String filterControlCharacters(String input) {
        // 过滤控制字符（\u0000-\u001F）
        return input.replaceAll("[\\u0000-\\u001F]", "");
    }
}