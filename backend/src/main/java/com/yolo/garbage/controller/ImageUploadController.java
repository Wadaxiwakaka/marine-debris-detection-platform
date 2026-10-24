package com.yolo.garbage.controller;

import com.yolo.garbage.model.Response;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class ImageUploadController {

    // 图片存储路径，使用绝对路径确保目录存在
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
    // 图片访问基础URL
    private static final String BASE_URL = "/uploads/";

    @PostMapping("/image")
    public Response<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Response.error("文件为空");
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

            // 返回文件访问URL
            String fileUrl = BASE_URL + fileName;
            return Response.success(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.error("文件上传失败：" + e.getMessage());
        }
    }
}
