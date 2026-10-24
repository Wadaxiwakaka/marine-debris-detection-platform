package com.yolo.garbage.dao;

import com.yolo.garbage.model.GarbageType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GarbageTypeMapper {
    List<GarbageType> selectAll();
    GarbageType selectById(Integer id);
    int insert(GarbageType garbageType);
    int update(GarbageType garbageType);
    int delete(Integer id);
}
