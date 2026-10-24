package com.yolo.garbage.service;

import com.yolo.garbage.dao.GarbageTypeMapper;
import com.yolo.garbage.model.GarbageType;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class GarbageTypeService {
    @Resource
    private GarbageTypeMapper garbageTypeMapper;

    public List<GarbageType> getAll() {
        return garbageTypeMapper.selectAll();
    }

    public GarbageType getById(Integer id) {
        return garbageTypeMapper.selectById(id);
    }

    public int create(GarbageType garbageType) {
        return garbageTypeMapper.insert(garbageType);
    }

    public int update(GarbageType garbageType) {
        return garbageTypeMapper.update(garbageType);
    }

    public int delete(Integer id) {
        return garbageTypeMapper.delete(id);
    }
}