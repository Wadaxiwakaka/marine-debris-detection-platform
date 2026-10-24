package com.yolo.garbage.service;

import com.yolo.garbage.dao.GarbageDataMapper;
import com.yolo.garbage.model.GarbageData;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GarbageDataService {
    @Resource
    private GarbageDataMapper garbageDataMapper;

    public List<GarbageData> getAll(Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return garbageDataMapper.selectAll(page, pageSize, offset);
    }

    public List<GarbageData> getAllForExport() {
        return garbageDataMapper.selectAllForExport();
    }

    public GarbageData getById(Long id) {
        return garbageDataMapper.selectById(id);
    }

    public List<GarbageData> getByCondition(Map<String, Object> condition) {
        // 计算offset参数
        if (condition.containsKey("page") && condition.containsKey("pageSize")) {
            Integer page = (Integer) condition.get("page");
            Integer pageSize = (Integer) condition.get("pageSize");
            int offset = (page - 1) * pageSize;
            condition.put("offset", offset);
        }
        return garbageDataMapper.selectByCondition(condition);
    }

    public int create(GarbageData garbageData) {
        return garbageDataMapper.insert(garbageData);
    }

    public int update(GarbageData garbageData) {
        return garbageDataMapper.update(garbageData);
    }

    public int delete(Long id) {
        return garbageDataMapper.delete(id);
    }

    public int batchDelete(List<Long> ids) {
        return garbageDataMapper.deleteBatch(ids);
    }

    public List<Map<String, Object>> getStatisticsByType(LocalDateTime startTime, LocalDateTime endTime) {
        return garbageDataMapper.selectStatisticsByType(startTime, endTime);
    }

    public List<Map<String, Object>> getStatisticsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return garbageDataMapper.selectStatisticsByTimeRange(startTime, endTime);
    }

    public int getTotalCount() {
        return garbageDataMapper.selectTotalCount();
    }

    public int getTotalCountByCondition(Map<String, Object> condition) {
        return garbageDataMapper.selectTotalCountByCondition(condition);
    }

    public List<GarbageData> getByKeyword(String keyword, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return garbageDataMapper.selectByKeyword(keyword, page, pageSize, offset);
    }

    public int getTotalCountByKeyword(String keyword) {
        return garbageDataMapper.selectTotalCountByKeyword(keyword);
    }
}