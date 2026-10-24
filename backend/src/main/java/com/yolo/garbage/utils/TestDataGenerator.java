package com.yolo.garbage.utils;

import com.yolo.garbage.model.GarbageData;
import com.yolo.garbage.service.GarbageDataService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
public class TestDataGenerator {

    @Resource
    private GarbageDataService garbageDataService;

    private final Random random = new Random();

    // 模拟垃圾类型ID范围（根据实际数据库中的垃圾类型ID调整）
    private final int[] typeIds = {1, 2, 3, 4, 5};

    // 模拟经纬度范围（以中国沿海地区为例）
    private final double minLatitude = 30.0;
    private final double maxLatitude = 35.0;
    private final double minLongitude = 120.0;
    private final double maxLongitude = 125.0;

    // 生成指定数量的测试垃圾数据
    public void generateTestData(int count) {
        System.out.println("开始生成" + count + "条测试垃圾数据...");

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            GarbageData garbageData = new GarbageData();

            // 随机垃圾类型ID
            int typeIdIndex = random.nextInt(typeIds.length);
            garbageData.setTypeId(typeIds[typeIdIndex]);

            // 随机置信度（0.7-1.0）
            double confidence = 0.7 + (random.nextDouble() * 0.3);
            garbageData.setConfidence(Math.round(confidence * 100.0) / 100.0);

            // 随机经纬度
            double latitude = minLatitude + (random.nextDouble() * (maxLatitude - minLatitude));
            double longitude = minLongitude + (random.nextDouble() * (maxLongitude - minLongitude));
            garbageData.setLatitude(Math.round(latitude * 10000.0) / 10000.0);
            garbageData.setLongitude(Math.round(longitude * 10000.0) / 10000.0);

            // 随机时间戳（过去7天内）
            LocalDateTime timestamp = now.minus(random.nextInt(7 * 24), ChronoUnit.HOURS);
            garbageData.setTimestamp(timestamp);

            // 模拟图片URL
            garbageData.setImageUrl("/uploads/test-image-" + (i + 1) + ".jpg");
            garbageData.setProcessedImageUrl("/uploads/processed-test-image-" + (i + 1) + ".jpg");

            // 保存数据
            garbageDataService.create(garbageData);

            if ((i + 1) % 10 == 0) {
                System.out.println("已生成" + (i + 1) + "条测试数据");
            }
        }

        System.out.println("测试数据生成完成！共生成" + count + "条垃圾数据");
    }
}
