package com.yolo.garbage.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GarbageData {
    private Long id;
    private Integer typeId;
    private Double confidence;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private String imageUrl;
    private String processedImageUrl;
    private LocalDateTime createTime;
    
    // 关联字段，非数据库表字段
    private GarbageType garbageType;
}
