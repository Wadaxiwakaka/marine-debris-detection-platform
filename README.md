# Marine Debris Detection Platform

## 基于 YOLO 的海漂垃圾识别与可视化平台

Vue 3 / Spring Boot 3 / FastAPI / YOLO 驱动的海漂垃圾识别、数据管理与地图可视化项目。

> 本仓库为源码公开版本，不包含原仓库历史、数据集、模型权重、训练产物或用户上传图片。运行推理服务前需自行准备模型权重，训练数据请遵守其来源的许可。

### 公开版本配置

- 后端启动前设置环境变量 `DB_PASSWORD` 和 `JWT_SECRET`（至少 32 字节的随机字符串），可选设置 `DB_USERNAME`。
- AI 助手按需设置 `DEEPSEEK_API_KEY` / `GLM_API_KEY`。
- 将 `frontend/.env.example` 复制为 `frontend/.env.local`，填入自己的百度地图浏览器端 AK，并在百度地图控制台限制允许的域名。
- 数据库初始化及重置脚本包含演示账号；仅用于本地学习，公开部署前请删除或修改默认账号。
- 下方原始开发说明中的模型文件和数据集路径需要按本机实际情况调整。

## 项目概述

基于YOLO的海漂垃圾识别与可视化系统是一个集成了人工智能识别、数据管理和可视化展示的综合性平台。该系统通过YOLO模型对海漂垃圾进行智能识别，并将识别结果通过地图可视化的方式展示出来，帮助用户直观了解海洋垃圾的分布情况和变化趋势。

本系统旨在通过人工智能技术提高海洋垃圾监测的效率和准确性，为海洋环境保护提供数据支持和决策依据。

## 项目结构

```
yolo/
├── PRD.md              # 产品需求文档
├── README.md           # 项目说明文档
├── schema.sql          # 数据库Schema
├── backend/            # Spring Boot后端
├── frontend/           # Vue3前端
├── model-service/      # FastAPI模型推理服务
└── model-training/     # 模型训练相关代码
```

### 后端结构 (backend/)

```
backend/
├── src/main/java/com/yolo/garbage/
│   ├── controller/     # 控制器
│   ├── service/        # 服务层
│   ├── dao/            # 数据访问层
│   ├── model/          # 模型类
│   ├── config/         # 配置类
│   └── Application.java # 主启动类
├── src/main/resources/
│   ├── mapper/         # MyBatis映射文件
│   └── application.yml # 配置文件
└── pom.xml             # Maven依赖文件
```

### 前端结构 (frontend/)

```
frontend/
├── src/
│   ├── components/     # 组件
│   ├── stores/         # Pinia状态管理
│   ├── views/          # 页面
│   ├── router.ts       # 路由配置
│   ├── main.ts         # 主入口
│   └── App.vue         # 根组件
├── index.html          # HTML模板
├── package.json        # 依赖配置
└── vite.config.ts      # Vite配置
```

### 模型推理服务结构 (model-service/)

```
model-service/
├── main.py             # FastAPI服务
└── requirements.txt    # 依赖配置
```

### 模型训练结构 (model-training/)

```
model-training/
├── train.py            # 训练脚本
├── data/               # 数据集
│   ├── images/         # 图像数据
│   ├── labels/         # 标签数据
│   └── garbage.yaml    # 数据集配置
└── runs/               # 训练结果
```

## 技术栈

- **后端**：Spring Boot 3 + MySQL8 + MyBatis
- **前端**：Vue3 + Vite + Pinia + Naive UI
- **模型推理**：FastAPI + YOLO
- **地图SDK**：百度地图

## 核心功能

1. **智能垃圾识别**：使用YOLO模型对海洋垃圾进行实时识别，支持多种垃圾类型的分类
2. **数据管理系统**：存储、管理和查询垃圾识别数据，支持分页、筛选和导出功能
3. **地图可视化**：通过百度地图展示垃圾分布情况，支持热点图展示
4. **数据统计分析**：通过图表展示垃圾类型分布和时间趋势分析
5. **用户管理系统**：管理系统用户和权限，支持注册、登录和个人信息管理
6. **AI助手**：集成AI模型，提供智能问答和分析功能

## 快速开始

### 1. 环境要求

- **后端**：JDK 17+, Maven 3.6+
- **前端**：Node.js 16+, npm 7+
- **模型服务**：Python 3.8+, pip 20+
- **数据库**：MySQL 8.0+

### 2. 数据库初始化

1. 安装并启动MySQL数据库
2. 执行 `schema.sql`文件创建数据库和表结构：

```bash
mysql -u root -p < schema.sql
```

<!-- 2. 执行 `reinit_database.py`脚本创建数据库和表结构： -->

```bash
python reinit_database.py
```

### 3. 启动后端服务

1. 进入backend目录
2. 安装依赖：

```bash
mvn clean install
```

3. 检查端口是否被占用：

```bash
netstat -ano | findstr :8080
```

4. 如果端口被占用，终止进程：

```bash
taskkill /PID 进程ID /F
```

5. 启动服务：

```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080/api`上运行

### 4. 启动模型推理服务

1. 进入model-service目录
2. 安装依赖：

```bash
pip install -r requirements.txt
```

3. 启动服务：

```bash
python main.py
```

服务将在 `http://localhost:8000`上运行

### 5. 启动前端服务

1. 进入frontend目录
2. 安装依赖：

```bash
npm install
```

3. 启动开发服务器：

```bash
npm run dev
```

服务将在 `http://localhost:5173`上运行

4. 构建生产版本：

```bash
npm run build
```

## 使用说明

### 1. 登录系统

1. 打开浏览器，访问 `http://localhost:5173`
2. 使用默认账号登录：
   - 用户名：admin
   - 密码：admin123
   - 注：数据库中的密码已使用 BCrypt 加密存储，登录时前端输入的明文密码 admin123 将通过后端的 BCrypt 验证进行匹配。

### 2. 垃圾识别

1. **通过模型推理服务API**：
   - 进入模型推理服务的API文档：`http://localhost:8000/docs`
   - 使用 `/predict`接口上传图像进行垃圾识别
   - 查看识别结果，包括垃圾类型、置信度和位置信息

2. **通过前端界面**：
   - 登录系统后，点击左侧菜单的"垃圾检测"
   - 上传图像文件
   - 点击"开始检测"按钮
   - 查看检测结果和识别精度

### 3. 数据管理

1. 登录系统后，点击左侧菜单的"垃圾数据"
2. 在垃圾数据页面，可以：
   - 查看垃圾数据列表
   - 按垃圾类型、时间范围等条件筛选数据
   - 查看单条数据的详细信息
   - 导出数据为Excel格式

### 4. 数据统计

1. 登录系统后，点击左侧菜单的"数据统计"
2. 在数据统计页面，可以：
   - 查看垃圾类型分布饼图
   - 查看垃圾数量时间趋势图
   - 查看不同区域的垃圾分布情况

### 5. 地图可视化

1. 登录系统后，在首页可以看到垃圾分布地图
2. 地图功能：
   - 支持缩放、平移等操作
   - 点击标记点查看垃圾详细信息
   - 点击"显示热点图"查看垃圾分布的热点区域
   - 支持按时间范围筛选数据

### 6. AI助手

1. 登录系统后，点击左侧菜单的"AI助手"
2. 在AI助手页面，可以：
   - 输入问题进行智能问答
   - 获取垃圾识别和分类的相关信息
   - 分析垃圾数据的趋势和模式

## 模型训练

### 1. 数据集准备

数据集来源：https://universe.roboflow.com/mohamed-traore-2ekkp/taco-trash-annotations-in-context/dataset/15

1. 进入model-training目录
2. 准备数据集，按照以下结构组织：
   - `data/images/train/`：训练图像
   - `data/images/val/`：验证图像
   - `data/labels/train/`：训练标签
   - `data/labels/val/`：验证标签
3. 配置数据集信息，修改 `data/garbage.yaml`文件：

```yaml
train: data/images/train/
val: data/images/val/
nc: 5  # 垃圾类别数量
names: ['塑料瓶', '塑料袋', '泡沫塑料', '渔网', '其他']  # 垃圾类别名称
```

### 2. 数据增强

使用 `data_augmentation.py`脚本对数据集进行增强：

```bash
python data_augmentation.py --input_dir data/images/train --output_dir augmented_data/images --label_dir data/labels/train --output_label_dir augmented_data/labels
```

### 3. 模型训练

运行训练脚本：

```bash
python train.py --model yolov5s.pt --data data/garbage.yaml --epochs 50 --batch 16 --img 640
```

训练参数说明：
- `--model`：预训练模型路径
- `--data`：数据集配置文件
- `--epochs`：训练轮数
- `--batch`：批次大小
- `--img`：输入图像大小

### 4. 模型评估

训练完成后，模型评估结果将保存在 `runs/train/`目录，包括：
- 混淆矩阵
- 精度-召回率曲线
- 训练损失曲线
- 验证结果

### 5. 模型部署

将训练好的模型文件（如 `best.pt`）复制到 `model-service`目录，修改 `main.py`文件中的模型路径：

```python
# 加载YOLO模型
model = YOLO('best.pt')
```

## 配置说明

### 1. 后端配置

修改 `backend/src/main/resources/application.yml`文件：

```yaml
spring:
  application:
    name: garbage-detection
  datasource:
    url: jdbc:mysql://localhost:3306/yolo_garbage?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis:
  type-aliases-package: com.yolo.garbage.model
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

logging:
  level:
    com.yolo.garbage: info

ai:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:your-deepseek-api-key}  # 修改为您的DeepSeek API密钥
  glm:
    api-key: ${GLM_API_KEY:your-glm-api-key}  # 修改为您的GLM API密钥
  system-prompt:
    file: system-prompt.txt
```

### 2. 前端配置

1. **百度地图API密钥**：
   修改 `frontend/index.html`文件中的百度地图API密钥：

   ```html
   <script type="text/javascript" src="https://api.map.baidu.com/api?v=3.0&ak=%VITE_BAIDU_MAP_AK%"></script>
   ```

2. **API基础URL**：
   修改 `frontend/src/utils/request.ts`文件中的API基础URL：

   ```typescript
   const service = axios.create({
     baseURL: 'http://localhost:8080/api',  // 修改为您的后端服务地址
     timeout: 10000
   });
   ```

### 3. 模型推理服务配置

修改 `model-service/main.py`文件中的模型路径和垃圾类型映射：

```python
# 加载YOLO模型
model = YOLO('trash_detection_v16.pt')  # 修改为您的模型文件路径

# 垃圾类型映射
GARBAGE_CLASSES = {
    0: '塑料瓶',
    1: '塑料袋',
    2: '泡沫塑料',
    3: '渔网',
    4: '其他'
}
```

## 注意事项

### 1. 系统使用注意事项

1. **百度地图API**：需要申请百度地图API密钥才能使用地图功能，请在 `frontend/index.html`文件中配置您的API密钥
2. **模型推理服务**：在CPU上运行速度较慢，建议在有GPU的环境中运行以获得更好的性能
3. **数据库配置**：请确保MySQL数据库已正确安装和配置，并且 `reinit_database.py`脚本中的数据库连接信息正确
4. **端口占用**：启动服务前请检查端口是否被占用，特别是8080（后端）、8000（模型服务）和5173（前端）端口

### 2. 模型训练注意事项

1. **数据集质量**：模型的识别准确率很大程度上取决于数据集的质量和多样性，建议使用实际的海洋垃圾数据集进行训练
2. **数据增强**：使用 `data_augmentation.py`脚本对数据集进行增强，可以提高模型的泛化能力
3. **训练参数**：根据硬件配置调整训练参数，如批次大小和输入图像大小
4. **模型评估**：训练完成后，仔细分析模型评估结果，如混淆矩阵和精度-召回率曲线

### 3. 部署注意事项

1. **环境配置**：确保所有依赖已正确安装，包括Java、Node.js、Python和相关库
2. **安全性**：在生产环境中，建议修改默认的JWT密钥和数据库密码
3. **性能优化**：对于大规模部署，建议使用负载均衡和缓存机制
4. **监控和日志**：配置适当的监控和日志系统，以便及时发现和解决问题

### 4. 其他注意事项

1. 本系统仅作为演示，实际应用中需要更多的优化和测试
2. 垃圾识别的准确率受到多种因素影响，如光照、角度、遮挡等
3. 系统的数据存储和处理能力有限，建议根据实际需求进行扩展

## 许可证

MIT
