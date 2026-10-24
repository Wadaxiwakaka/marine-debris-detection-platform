import os
from ultralytics import YOLO
import torch

# 解决多线程问题，避免内存泄漏
os.environ['KMP_DUPLICATE_LIB_OK'] = 'TRUE'

def train_optimized_model():
    # 清理显存碎片
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
    
    # 使用 s 版本
    model = YOLO('yolo11s.pt')

    # 使用 TACO 数据集（已处理、增强、resize到640）
    dataset_yaml = r'D:\TraeProject\yolo\model-training\TACO- Trash Annotations in Context Dataset.v16-resize640-aug3x-allclasses-mappedtotrash.yolov11\data.yaml' 
    
    results = model.train(
        data=dataset_yaml,
        
        # --- 基础配置 ---
        epochs=400,
        patience=50,
        imgsz=640,
        workers=2,
        device='0',
        project='runs/detect',
        name='trash_detection_v16_optimized',
        
        # --- 显存与硬件优化 ---
        batch=-1,               # 让YOLO自己算最大安全Batch，压榨性能
        amp=True,               # 混合精度，省显存
        
        # --- 优化器与学习率 (治漏检) ---
        optimizer='AdamW',      # 对复杂场景更好
        lr0=0.001,              # AdamW 的初始学习率应该比 SGD 小
        lrf=0.01,
        cos_lr=True,            # 余弦退火，收敛更好
        
        # --- 损失权重 (治框不准) ---
        box=15.0,               # 从7.5翻倍到15.0，逼迫画准框！
        dfl=2.0,                # 从1.5提高，细化框边界
        cls=0.5,                # 如果是单一类别，其实0.5甚至0.3都可以
        
        # --- 数据增强 (适应海洋真实场景) ---
        mosaic=1.0,             # 保持1.0，对小目标好
        mixup=0.15,             # 从0增加到0.15，抗遮挡重叠
        degrees=45.0,           # 从0增加到45，适应垃圾的各种翻滚角度
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        perspective=0.0,
        shear=0.0,
        
        # --- 其他 ---
        save=True,
        plots=True,
        profile=False
    )

if __name__ == '__main__':
    train_optimized_model()