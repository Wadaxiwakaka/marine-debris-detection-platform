package com.yolo.garbage.controller;

import com.yolo.garbage.model.GarbageData;
import com.yolo.garbage.model.Response;
import com.yolo.garbage.service.GarbageDataService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/garbage-data")
public class GarbageDataController {
    @Resource
    private GarbageDataService garbageDataService;

    @GetMapping
    public Response<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<GarbageData> garbageDataList = garbageDataService.getAll(page, pageSize);
        int total = garbageDataService.getTotalCount();
        Map<String, Object> result = new HashMap<>();
        result.put("data", garbageDataList);
        result.put("total", total);
        return Response.success(result);
    }

    @GetMapping("/export")
    public Response<List<GarbageData>> getAllForExport() {
        List<GarbageData> garbageDataList = garbageDataService.getAllForExport();
        return Response.success(garbageDataList);
    }

    @GetMapping("/{id}")
    public Response<GarbageData> getById(@PathVariable Long id) {
        GarbageData garbageData = garbageDataService.getById(id);
        if (garbageData == null) {
            return Response.error("Garbage data not found");
        }
        return Response.success(garbageData);
    }

    @PostMapping("/condition")
    public Response<List<GarbageData>> getByCondition(@RequestBody Map<String, Object> condition) {
        List<GarbageData> garbageDataList = garbageDataService.getByCondition(condition);
        return Response.success(garbageDataList);
    }

    @PostMapping("/condition/page")
    public Response<Map<String, Object>> getByConditionWithPage(
            @RequestBody Map<String, Object> condition,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        condition.put("page", page);
        condition.put("pageSize", pageSize);
        List<GarbageData> garbageDataList = garbageDataService.getByCondition(condition);
        int total = garbageDataService.getTotalCountByCondition(condition);
        Map<String, Object> result = new HashMap<>();
        result.put("data", garbageDataList);
        result.put("total", total);
        return Response.success(result);
    }

    @GetMapping("/search")
    public Response<Map<String, Object>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<GarbageData> garbageDataList = garbageDataService.getByKeyword(keyword, page, pageSize);
        int total = garbageDataService.getTotalCountByKeyword(keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("data", garbageDataList);
        result.put("total", total);
        return Response.success(result);
    }

    @PostMapping
    public Response<Integer> create(@RequestBody GarbageData garbageData) {
        int result = garbageDataService.create(garbageData);
        return Response.success(result);
    }

    @PutMapping
    public Response<Integer> update(@RequestBody GarbageData garbageData) {
        int result = garbageDataService.update(garbageData);
        return Response.success(result);
    }

    @DeleteMapping("/{id}")
    public Response<Integer> delete(@PathVariable Long id) {
        int result = garbageDataService.delete(id);
        return Response.success(result);
    }

    @DeleteMapping("/batch")
    public Response<Integer> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        int result = garbageDataService.batchDelete(ids);
        return Response.success(result);
    }

    @GetMapping("/statistics/type")
    public Response<List<Map<String, Object>>> getStatisticsByType(
            @RequestParam String startTime, 
            @RequestParam String endTime) {
        List<Map<String, Object>> statistics;
        if (startTime.isEmpty() || endTime.isEmpty()) {
            // 当传入空字符串时，获取所有数据
            statistics = garbageDataService.getStatisticsByType(null, null);
        } else {
            // 处理带时区的时间字符串，移除Z后缀并转换为LocalDateTime
            String startStr = startTime.replace("Z", "");
            String endStr = endTime.replace("Z", "");
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);
            statistics = garbageDataService.getStatisticsByType(start, end);
        }
        return Response.success(statistics);
    }

    @GetMapping("/statistics/time-range")
    public Response<List<Map<String, Object>>> getStatisticsByTimeRange(
            @RequestParam String startTime, 
            @RequestParam String endTime) {
        List<Map<String, Object>> statistics;
        if (startTime.isEmpty() || endTime.isEmpty()) {
            // 当传入空字符串时，获取所有数据
            statistics = garbageDataService.getStatisticsByTimeRange(null, null);
        } else {
            // 处理带时区的时间字符串，移除Z后缀并转换为LocalDateTime
            String startStr = startTime.replace("Z", "");
            String endStr = endTime.replace("Z", "");
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);
            statistics = garbageDataService.getStatisticsByTimeRange(start, end);
        }
        return Response.success(statistics);
    }
}
