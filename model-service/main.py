"""
海洋垃圾识别模型服务

此服务基于 FastAPI 和 YOLO（Ultralytics）实现，提供海漂垃圾识别的API接口。

主要功能：
1. 支持两种识别模式：
   - simple（简单模式）：将所有检测到的目标标记为"垃圾"
   - detailed（详细模式）：识别59种具体垃圾类型

2. API接口：
   - POST /predict：上传图片进行垃圾识别
   - GET /：返回服务状态信息

3. 技术栈：
   - FastAPI：高性能异步Web框架
   - YOLO（Ultralytics）：实时目标检测模型
   - OpenCV/PIL：图像处理
   - NumPy：数值计算

运行方式：
   python main.py
   或
   uvicorn main:app --host 0.0.0.0 --port 8000
"""

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import cv2
import numpy as np
from PIL import Image
import io
from ultralytics import YOLO
import logging
import os
from typing import Dict, List

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 应用配置
APP_CONFIG = {
    "title": "海洋垃圾识别模型服务",
    "version": "1.0.0",
    "cors_origins": ["*"],  # 在生产环境中应该设置具体的前端域名
    "model_paths": {
        "simple": os.getenv("SIMPLE_MODEL_PATH", r"D:\TraeProject\yolo\model-service\trash_detection_v16_optimized_640.pt"),  # 简单模式模型 - 所有垃圾标记为"垃圾"
        "detailed": os.getenv("DETAILED_MODEL_PATH", r"D:\TraeProject\yolo\model-service\augmented_trash_detection_v15.pt")  # 详细模式模型 - 识别具体垃圾类别（增强数据集模型）
    },
    "host": os.getenv("HOST", "0.0.0.0"),
    "port": int(os.getenv("PORT", "8000"))
}

# 创建FastAPI应用实例
app = FastAPI(
    title=APP_CONFIG["title"],
    version=APP_CONFIG["version"]
)

# 添加CORS支持
app.add_middleware(
    CORSMiddleware,
    allow_origins=APP_CONFIG["cors_origins"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 模型缓存
model_cache = {}

# 垃圾类型映射 - 两种模式
# 模式1: 简单模式（只有一类）
SIMPLE_GARBAGE_CLASSES = {
    0: '垃圾'
}

# 模式2: 详细模式（59种垃圾类型）
# 注意：模型输出的类别ID从0开始，对应data.yaml中的索引
DETAILED_GARBAGE_CLASSES = {
    0: '气溶胶罐',      # Aerosol
    1: '铝塑泡罩包装',    # Aluminium blister pack
    2: '铝箔',           # Aluminium foil
    3: '电池',           # Battery
    4: '碎玻璃',         # Broken glass
    5: '卡片式泡罩包装',  # Carded blister pack
    6: '香烟',           # Cigarette
    7: '透明塑料瓶',      # Clear plastic bottle
    8: '瓦楞纸箱',        # Corrugated carton
    9: '薯片袋',          # Crisp packet
    10: '一次性食品容器',  # Disposable food container
    11: '一次性塑料杯',    # Disposable plastic cup
    12: '饮料罐',         # Drink can
    13: '饮料纸盒',        # Drink carton
    14: '鸡蛋盒',         # Egg carton
    15: '泡沫杯',         # Foam cup
    16: '泡沫食品容器',    # Foam food container
    17: '食品罐',         # Food Can
    18: '食物残渣',        # Food waste
    19: '垃圾袋',          # Garbage bag
    20: '玻璃瓶',          # Glass bottle
    21: '玻璃杯',          # Glass cup
    22: '玻璃罐',          # Glass jar
    23: '杂志纸',          # Magazine paper
    24: '餐盒',           # Meal carton
    25: '金属瓶盖',         # Metal bottle cap
    26: '金属盖',          # Metal lid
    27: '普通纸',          # Normal paper
    28: '其他纸箱',         # Other carton
    29: '其他塑料瓶',        # Other plastic bottle
    30: '其他塑料容器',       # Other plastic container
    31: '其他塑料杯',         # Other plastic cup
    32: '其他塑料包装',        # Other plastic wrapper
    33: '其他塑料',          # Other plastic
    34: '纸袋',           # Paper bag
    35: '纸杯',           # Paper cup
    36: '纸吸管',          # Paper straw
    37: '披萨盒',          # Pizza box
    38: '塑料瓶盖',         # Plastic bottle cap
    39: '塑料膜',          # Plastic film
    40: '塑料手套',         # Plastic glooves
    41: '塑料盖',          # Plastic lid
    42: '塑料吸管',         # Plastic straw
    43: '塑料餐具',         # Plastic utensils
    44: '聚丙烯袋',         # Polypropylene bag
    45: '易拉罐拉环',        # Pop tab
    46: '绳索',           # Rope - strings
    47: '废金属',          # Scrap metal
    48: '鞋子',           # Shoe
    49: '一次性购物袋',       # Single-use carrier bag
    50: '六联包装环',        # Six pack rings
    51: '酱料罐',          # Spread tub
    52: '挤压管',          # Squeezable tube
    53: '泡沫塑料块',        # Styrofoam piece
    54: '纸巾',           # Tissues
    55: '卫生纸管',         # Toilet tube
    56: '保鲜盒',          # Tupperware
    57: '未标记垃圾',        # Unlabeled litter
    58: '包装纸'           # Wrapping paper
}

def get_model(model_type: str) -> YOLO:
    """
    懒加载模型，从缓存中获取或加载新模型
    
    Args:
        model_type: 模型类型，'simple' 或 'detailed'
    
    Returns:
        YOLO: 加载的YOLO模型实例
    """
    if model_type not in model_cache:
        logger.info(f"Loading {model_type} model from {APP_CONFIG['model_paths'][model_type]}")
        try:
            model_cache[model_type] = YOLO(APP_CONFIG['model_paths'][model_type])
            logger.info(f"{model_type} model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load {model_type} model: {str(e)}")
            raise
    return model_cache[model_type]

def process_image(image_data: bytes) -> np.ndarray:
    """
    处理上传的图像数据
    
    Args:
        image_data: 图像字节数据
    
    Returns:
        np.ndarray: 处理后的图像数组
    """
    # 尝试打开图像
    try:
        image = Image.open(io.BytesIO(image_data))
        logger.info(f"Image opened successfully: {image.size} {image.mode}")
    except Exception as img_error:
        logger.error(f"Image opening error: {str(img_error)}")
        raise ValueError(f"Image opening error: {str(img_error)}")
    
    # 转换为OpenCV格式
    img_array = np.array(image)
    # 检查图像数组是否为3通道彩色图像
    if len(img_array.shape) == 3 and img_array.shape[2] == 3:
        # 不同库的色彩通道顺序不同，需要转换为OpenCV的BGR格式
        img_array = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
        logger.info("Image converted to BGR format")
    
    return img_array

def parse_predictions(results, garbage_classes: Dict[int, str]) -> List[Dict]:
    """
    解析模型预测结果
    
    Args:
        results: 模型预测结果
        garbage_classes: 垃圾类型映射字典
    
    Returns:
        List[Dict]: 解析后的预测结果列表
    """
    predictions = []
    
    for result in results:
        if hasattr(result, 'boxes'):
            for box in result.boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                confidence = box.conf[0].item()
                class_id = int(box.cls[0].item())
                
                # 记录模型输出的原始类别ID
                logger.debug(f"Model output class_id: {class_id}, confidence: {confidence}")
                
                # 映射到垃圾类型
                garbage_type = garbage_classes.get(class_id, '其他')
                
                predictions.append({
                    "type": garbage_type,
                    "confidence": confidence,
                    "bbox": [x1, y1, x2, y2]
                })
    
    logger.info(f"Processed {len(predictions)} predictions")
    return predictions

@app.post("/predict")
async def predict(file: UploadFile = File(...), mode: str = Form("simple")):
    """
    预测垃圾类型的API接口
    
    Args:
        file: 上传的图像文件
        mode: 预测模式，'simple' 或 'detailed'
    
    Returns:
        JSONResponse: 预测结果
    """
    try:
        # 读取上传的文件
        contents = await file.read()
        logger.info(f"Received file: {file.filename}, size: {len(contents)} bytes")
        
        # 检查文件大小
        if len(contents) == 0:
            logger.warning("Empty file received")
            return JSONResponse(content={
                "status": "error",
                "message": "Empty file"
            }, status_code=400)
        
        # 处理图像
        try:
            img_array = process_image(contents)
        except ValueError as e:
            return JSONResponse(content={
                "status": "error",
                "message": str(e)
            }, status_code=400)
        
        # 根据模式选择模型和类别映射
        if mode == "detailed":
            model = get_model("detailed")
            garbage_classes = DETAILED_GARBAGE_CLASSES
        else:
            model = get_model("simple")
            garbage_classes = SIMPLE_GARBAGE_CLASSES
        
        # 使用YOLO模型进行预测
        logger.info(f"Performing prediction in {mode} mode")
        
        # 记录推理开始时间
        import time
        start_time = time.time()
        
        results = model(img_array)
        
        # 计算推理时间（秒）
        inference_time = time.time() - start_time
        
        # 处理预测结果
        predictions = parse_predictions(results, garbage_classes)
        
        return JSONResponse(content={
            "status": "success",
            "mode": mode,
            "inference_time": round(inference_time, 3),
            "predictions": predictions
        })
    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
        return JSONResponse(content={
            "status": "error",
            "message": str(e)
        }, status_code=500)

@app.get("/")
async def root():
    """
    根路径接口，返回服务信息
    
    Returns:
        Dict: 服务信息
    """
    return {"message": "海洋垃圾识别模型服务", "version": APP_CONFIG["version"]}

if __name__ == "__main__":
    import uvicorn
    logger.info(f"Starting server on {APP_CONFIG['host']}:{APP_CONFIG['port']}")
    uvicorn.run(app, host=APP_CONFIG["host"], port=APP_CONFIG["port"])
