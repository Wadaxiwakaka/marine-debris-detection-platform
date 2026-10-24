package com.yolo.garbage.ai.model;

public enum AIModel {
    DEEPSEEK("DeepSeek"),
    GLM_4_7_FLASH("GLM-4.7-Flash");


    private final String name;

    AIModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AIModel fromName(String name) {
        for (AIModel model : values()) {
            if (model.getName().equals(name)) {
                return model;
            }
        }
        return null;
    }
}