import os
import pymysql

# 数据库连接信息
config = {
    'host': 'localhost',
    'user': 'root',
    'password': os.environ['DB_PASSWORD'],
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# 60种垃圾类型
garbage_types = [
    (0, '垃圾', '简单模式下的通用垃圾类型'),
    (1, '气溶胶罐', '各种气溶胶喷雾罐'),
    (2, '铝塑泡罩包装', '药品、保健品等铝塑包装'),
    (3, '铝箔', '食品包装用铝箔'),
    (4, '电池', '各种废电池'),
    (5, '碎玻璃', '破碎的玻璃制品'),
    (6, '卡片式泡罩包装', '类似信用卡大小的泡罩包装'),
    (7, '香烟', '烟头、烟盒等'),
    (8, '透明塑料瓶', '透明的塑料饮料瓶'),
    (9, '瓦楞纸箱', '纸板箱、纸箱等'),
    (10, '薯片袋', '零食包装袋'),
    (11, '一次性食品容器', '一次性饭盒、餐盒等'),
    (12, '一次性塑料杯', '一次性塑料水杯'),
    (13, '饮料罐', '金属饮料罐'),
    (14, '饮料纸盒', '纸质饮料盒'),
    (15, '鸡蛋盒', '纸质或塑料鸡蛋盒'),
    (16, '泡沫杯', '泡沫塑料杯'),
    (17, '泡沫食品容器', '泡沫塑料食品包装盒'),
    (18, '食品罐', '金属食品罐'),
    (19, '食物残渣', '剩余食物'),
    (20, '垃圾袋', '各种垃圾袋'),
    (21, '玻璃瓶', '玻璃饮料瓶、酒瓶等'),
    (22, '玻璃杯', '玻璃杯子'),
    (23, '玻璃罐', '玻璃食品罐'),
    (24, '杂志纸', '杂志、期刊等'),
    (25, '餐盒', '各种餐盒'),
    (26, '金属瓶盖', '金属饮料瓶盖'),
    (27, '金属盖', '其他金属盖子'),
    (28, '普通纸', '办公用纸、打印纸等'),
    (29, '其他纸箱', '除瓦楞纸箱外的其他纸箱'),
    (30, '其他塑料瓶', '非透明塑料瓶'),
    (31, '其他塑料容器', '除瓶子外的其他塑料容器'),
    (32, '其他塑料杯', '除一次性塑料杯外的其他塑料杯'),
    (33, '其他塑料包装', '各种塑料包装材料'),
    (34, '其他塑料', '其他塑料制品'),
    (35, '纸袋', '纸质购物袋'),
    (36, '纸杯', '纸质杯子'),
    (37, '纸吸管', '纸质吸管'),
    (38, '披萨盒', '披萨包装盒'),
    (39, '塑料瓶盖', '塑料饮料瓶盖'),
    (40, '塑料膜', '各种塑料薄膜'),
    (41, '塑料手套', '一次性塑料手套'),
    (42, '塑料盖', '其他塑料盖子'),
    (43, '塑料吸管', '塑料吸管'),
    (44, '塑料餐具', '一次性塑料餐具'),
    (45, '聚丙烯袋', 'PP材质的塑料袋'),
    (46, '易拉罐拉环', '饮料罐拉环'),
    (47, '绳索', '各种绳索'),
    (48, '废金属', '各种废弃金属'),
    (49, '鞋子', '旧鞋子'),
    (50, '一次性购物袋', '超市购物袋'),
    (51, '六联包装环', '饮料包装的塑料环'),
    (52, '酱料罐', '调味品罐'),
    (53, '挤压管', '牙膏、护肤品等挤压管'),
    (54, '泡沫塑料块', '各种泡沫塑料块'),
    (55, '纸巾', '卫生纸、餐巾纸等'),
    (56, '卫生纸管', '卫生纸内芯'),
    (57, '保鲜盒', '塑料保鲜盒'),
    (58, '未标记垃圾', '无法识别的垃圾'),
    (59, '包装纸', '各种包装纸')
]

try:
    # 连接数据库（不指定数据库名）
    connection = pymysql.connect(**config)
    print("成功连接到数据库")
    
    with connection.cursor() as cursor:
        # 删除现有数据库
        sql = "DROP DATABASE IF EXISTS yolo_garbage"
        cursor.execute(sql)
        print("已删除现有数据库")
        
        # 创建数据库
        sql = "CREATE DATABASE IF NOT EXISTS yolo_garbage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        cursor.execute(sql)
        print("已创建新数据库")
        
        # 选择数据库
        sql = "USE yolo_garbage"
        cursor.execute(sql)
        print("已选择数据库")
        
        # 创建垃圾类型表
        sql = """
        CREATE TABLE IF NOT EXISTS garbage_types (
            id INT PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description VARCHAR(255),
            create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
        cursor.execute(sql)
        print("已创建垃圾类型表")
        
        # 创建用户表
        sql = """
        CREATE TABLE IF NOT EXISTS users (
            id INT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(50) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            role VARCHAR(20) NOT NULL DEFAULT 'user',
            avatar VARCHAR(255) DEFAULT NULL,
            create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
        cursor.execute(sql)
        print("已创建用户表")
        
        # 创建垃圾数据表
        sql = """
        CREATE TABLE IF NOT EXISTS garbage_data (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            type_id INT NOT NULL,
            confidence DECIMAL(5,4) NOT NULL,
            latitude DECIMAL(10,6) NOT NULL,
            longitude DECIMAL(10,6) NOT NULL,
            timestamp TIMESTAMP NOT NULL,
            image_url VARCHAR(255),
            processed_image_url VARCHAR(255),
            create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (type_id) REFERENCES garbage_types(id)
        )
        """
        cursor.execute(sql)
        print("已创建垃圾数据表")
        
        # 创建索引
        sql = "CREATE INDEX idx_garbage_data_type ON garbage_data(type_id)"
        cursor.execute(sql)
        sql = "CREATE INDEX idx_garbage_data_timestamp ON garbage_data(timestamp)"
        cursor.execute(sql)
        sql = "CREATE INDEX idx_garbage_data_location ON garbage_data(latitude, longitude)"
        cursor.execute(sql)
        print("已创建索引")
        
        # 插入垃圾类型
        sql = "INSERT INTO garbage_types (id, name, description) VALUES (%s, %s, %s)"
        cursor.executemany(sql, garbage_types)
        print(f"已插入 {len(garbage_types)} 种垃圾类型")
        
        # 插入默认用户
        sql = "INSERT INTO users (username, password, role) VALUES (%s, %s, %s)"
        users = [
            ('admin', '$2a$10$e8vB2z5v7j5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5', 'admin'),
            ('user', '$2a$10$e8vB2z5v7j5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5', 'user')
        ]
        cursor.executemany(sql, users)
        print("已插入默认用户")
        
        # 提交事务
        connection.commit()
        print("事务提交成功")
        
        # 验证垃圾类型
        sql = "SELECT COUNT(*) as count FROM garbage_types"
        cursor.execute(sql)
        result = cursor.fetchone()
        print(f"数据库中垃圾类型数量: {result['count']}")
        
        # 查看前10条垃圾类型
        sql = "SELECT * FROM garbage_types ORDER BY id LIMIT 10"
        cursor.execute(sql)
        results = cursor.fetchall()
        print("\n前10条垃圾类型记录:")
        for row in results:
            print(f"ID: {row['id']}, 名称: {row['name']}, 描述: {row['description']}")
            
except Exception as e:
    print(f"错误: {e}")
finally:
    if 'connection' in locals():
        connection.close()
        print("数据库连接已关闭")
