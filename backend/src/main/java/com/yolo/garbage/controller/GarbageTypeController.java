package com.yolo.garbage.controller;

import com.yolo.garbage.model.GarbageType;
import com.yolo.garbage.model.Response;
import com.yolo.garbage.service.GarbageTypeService;
import com.yolo.garbage.utils.ValidationUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/garbage-type")
public class GarbageTypeController {
    @Resource
    private GarbageTypeService garbageTypeService;

    @GetMapping
    public Response<List<GarbageType>> getAll() {
        List<GarbageType> garbageTypes = garbageTypeService.getAll();
        return Response.success(garbageTypes);
    }

    @GetMapping("/{id}")
    public Response<GarbageType> getById(@PathVariable Integer id) {
        if (!ValidationUtil.validateId(id)) {
            return Response.error("Invalid garbage type ID");
        }
        GarbageType garbageType = garbageTypeService.getById(id);
        if (garbageType == null) {
            return Response.error("Garbage type not found");
        }
        return Response.success(garbageType);
    }

    @PostMapping
    public Response<Integer> create(@RequestBody GarbageType garbageType) {
        if (garbageType.getName() == null || garbageType.getName().isEmpty() || garbageType.getName().length() > 50) {
            return Response.error("Invalid garbage type name");
        }
        if (garbageType.getDescription() != null && garbageType.getDescription().length() > 200) {
            return Response.error("Invalid garbage type description");
        }
        int result = garbageTypeService.create(garbageType);
        return Response.success(result);
    }

    @PutMapping
    public Response<Integer> update(@RequestBody GarbageType garbageType) {
        if (!ValidationUtil.validateId(garbageType.getId())) {
            return Response.error("Invalid garbage type ID");
        }
        if (garbageType.getName() == null || garbageType.getName().isEmpty() || garbageType.getName().length() > 50) {
            return Response.error("Invalid garbage type name");
        }
        if (garbageType.getDescription() != null && garbageType.getDescription().length() > 200) {
            return Response.error("Invalid garbage type description");
        }
        int result = garbageTypeService.update(garbageType);
        return Response.success(result);
    }

    @DeleteMapping("/{id}")
    public Response<Integer> delete(@PathVariable Integer id) {
        if (!ValidationUtil.validateId(id)) {
            return Response.error("Invalid garbage type ID");
        }
        int result = garbageTypeService.delete(id);
        return Response.success(result);
    }
}
