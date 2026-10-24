package com.yolo.garbage.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GarbageType {
    private Integer id;
    private String name;
    private String description;
    private LocalDateTime createTime;
}
