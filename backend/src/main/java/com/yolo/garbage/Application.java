package com.yolo.garbage;

import com.yolo.garbage.utils.TestDataGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class Application {

    // @Resource
    // private TestDataGenerator testDataGenerator;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    // @PostConstruct
    // public void init() {
    //     // 生成100条测试垃圾数据
    //     testDataGenerator.generateTestData(100);
    // }
    
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // 配置静态资源访问路径
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:uploads/");
        }
    }
}