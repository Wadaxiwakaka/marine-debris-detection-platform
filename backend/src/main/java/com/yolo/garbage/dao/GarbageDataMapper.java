package com.yolo.garbage.dao;

import com.yolo.garbage.model.GarbageData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface GarbageDataMapper {
    List<GarbageData> selectAll(@Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);
    List<GarbageData> selectAllForExport();
    int selectTotalCount();
    GarbageData selectById(Long id);
    List<GarbageData> selectByCondition(Map<String, Object> condition);
    int selectTotalCountByCondition(Map<String, Object> condition);
    List<GarbageData> selectByKeyword(@Param("keyword") String keyword, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);
    int selectTotalCountByKeyword(@Param("keyword") String keyword);
    int insert(GarbageData garbageData);
    int update(GarbageData garbageData);
    int delete(Long id);
    int deleteBatch(@Param("ids") List<Long> ids);
    List<Map<String, Object>> selectStatisticsByType(LocalDateTime startTime, LocalDateTime endTime);
    List<Map<String, Object>> selectStatisticsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
}
