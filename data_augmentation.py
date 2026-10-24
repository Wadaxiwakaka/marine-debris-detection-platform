import os
import cv2
import numpy as np
import random
import argparse

class DataAugmenter:
    def __init__(self, input_dir, output_dir, augment_factor=3):
        self.input_dir = input_dir
        self.output_dir = output_dir
        self.augment_factor = augment_factor
        
        # 创建输出目录
        self.image_output_dir = os.path.join(output_dir, 'images')
        self.label_output_dir = os.path.join(output_dir, 'labels')
        os.makedirs(self.image_output_dir, exist_ok=True)
        os.makedirs(self.label_output_dir, exist_ok=True)
    
    def load_label(self, label_path):
        """加载YOLO格式的标签文件"""
        labels = []
        if os.path.exists(label_path):
            with open(label_path, 'r') as f:
                for line in f.readlines():
                    line = line.strip()
                    if line:
                        parts = list(map(float, line.split()))
                        class_id = int(parts[0])
                        coords = parts[1:]
                        # 将坐标转换为(x, y)对
                        points = []
                        for i in range(0, len(coords), 2):
                            points.append((coords[i], coords[i+1]))
                        labels.append((class_id, points))
        return labels
    
    def save_label(self, label_path, labels):
        """保存YOLO格式的标签文件"""
        with open(label_path, 'w') as f:
            for class_id, points in labels:
                # 将(x, y)对转换为一维列表
                coords = []
                for x, y in points:
                    coords.extend([x, y])
                # 确保坐标在0-1范围内
                coords = [max(0, min(1, c)) for c in coords]
                line = f"{class_id} {' '.join(map(str, coords))}\n"
                f.write(line)
    
    def rotate_image(self, image, angle):
        """旋转图像"""
        h, w = image.shape[:2]
        center = (w // 2, h // 2)
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        rotated = cv2.warpAffine(image, M, (w, h), flags=cv2.INTER_LINEAR)
        return rotated, M
    
    def rotate_points(self, points, angle, image_shape):
        """旋转多边形点"""
        h, w = image_shape[:2]
        center = (w // 2, h // 2)
        
        # 将归一化坐标转换为像素坐标
        pixel_points = []
        for x, y in points:
            pixel_x = x * w
            pixel_y = y * h
            pixel_points.append((pixel_x, pixel_y))
        
        # 旋转点
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        rotated_points = []
        for x, y in pixel_points:
            # 应用旋转矩阵
            new_x = M[0, 0] * x + M[0, 1] * y + M[0, 2]
            new_y = M[1, 0] * x + M[1, 1] * y + M[1, 2]
            rotated_points.append((new_x, new_y))
        
        # 将像素坐标转换回归一化坐标
        normalized_points = []
        for x, y in rotated_points:
            norm_x = x / w
            norm_y = y / h
            normalized_points.append((norm_x, norm_y))
        
        return normalized_points
    
    def crop_image(self, image, crop_size):
        """随机裁剪图像"""
        h, w = image.shape[:2]
        crop_h, crop_w = crop_size
        
        # 确保裁剪大小不超过图像大小
        crop_h = min(crop_h, h)
        crop_w = min(crop_w, w)
        
        # 随机选择裁剪区域
        x_start = random.randint(0, w - crop_w)
        y_start = random.randint(0, h - crop_h)
        
        cropped = image[y_start:y_start+crop_h, x_start:x_start+crop_w]
        # 调整大小回原始尺寸
        resized = cv2.resize(cropped, (w, h))
        
        return resized, (x_start, y_start, crop_w, crop_h)
    
    def adjust_crop_points(self, points, crop_box, image_shape):
        """调整裁剪后的多边形点"""
        h, w = image_shape[:2]
        x_start, y_start, crop_w, crop_h = crop_box
        
        adjusted_points = []
        for x, y in points:
            # 将归一化坐标转换为像素坐标
            pixel_x = x * w
            pixel_y = y * h
            
            # 检查点是否在裁剪区域内
            if x_start <= pixel_x <= x_start + crop_w and y_start <= pixel_y <= y_start + crop_h:
                # 调整坐标到裁剪区域内
                new_x = (pixel_x - x_start) / crop_w
                new_y = (pixel_y - y_start) / crop_h
                adjusted_points.append((new_x, new_y))
        
        return adjusted_points if adjusted_points else None
    
    def adjust_brightness(self, image):
        """调整亮度、对比度、饱和度"""
        # 随机调整亮度
        alpha = random.uniform(0.7, 1.3)  # 对比度因子
        beta = random.uniform(-30, 30)  # 亮度偏移
        adjusted = cv2.convertScaleAbs(image, alpha=alpha, beta=beta)
        
        # 随机调整饱和度
        hsv = cv2.cvtColor(adjusted, cv2.COLOR_BGR2HSV)
        saturation_factor = random.uniform(0.7, 1.3)
        hsv[:, :, 1] = hsv[:, :, 1] * saturation_factor
        hsv[:, :, 1] = np.clip(hsv[:, :, 1], 0, 255)
        adjusted = cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)
        
        return adjusted
    
    def blur_image(self, image):
        """添加高斯模糊"""
        kernel_size = random.choice([3, 5])
        blurred = cv2.GaussianBlur(image, (kernel_size, kernel_size), 0)
        return blurred
    
    def flip_image(self, image, flip_code):
        """翻转图像"""
        flipped = cv2.flip(image, flip_code)
        return flipped
    
    def flip_points(self, points, flip_code, image_shape):
        """翻转多边形点"""
        h, w = image_shape[:2]
        flipped_points = []
        
        for x, y in points:
            if flip_code == 0:  # 垂直翻转
                new_x = x
                new_y = 1 - y
            elif flip_code == 1:  # 水平翻转
                new_x = 1 - x
                new_y = y
            else:  # 水平垂直翻转
                new_x = 1 - x
                new_y = 1 - y
            flipped_points.append((new_x, new_y))
        
        return flipped_points
    
    def augment_image(self, image_path, label_path):
        """增强单张图像"""
        # 读取图像
        image = cv2.imread(image_path)
        if image is None:
            print(f"无法读取图像: {image_path}")
            return
        
        # 读取标签
        labels = self.load_label(label_path)
        
        # 生成增强图像
        for i in range(self.augment_factor):
            # 复制原始图像和标签
            augmented_image = image.copy()
            augmented_labels = labels.copy()
            
            # 随机选择增强方法
            augment_methods = random.sample([
                'rotate', 'crop', 'brightness', 'blur', 'flip'
            ], random.randint(1, 3))
            
            for method in augment_methods:
                if method == 'rotate':
                    # 随机旋转角度
                    angle = random.uniform(0, 360)
                    augmented_image, _ = self.rotate_image(augmented_image, angle)
                    # 更新标签
                    new_labels = []
                    for class_id, points in augmented_labels:
                        rotated_points = self.rotate_points(points, angle, image.shape)
                        new_labels.append((class_id, rotated_points))
                    augmented_labels = new_labels
                
                elif method == 'crop':
                    # 随机裁剪大小
                    crop_size = (random.randint(480, 640), random.randint(480, 640))
                    augmented_image, crop_box = self.crop_image(augmented_image, crop_size)
                    # 更新标签
                    new_labels = []
                    for class_id, points in augmented_labels:
                        adjusted_points = self.adjust_crop_points(points, crop_box, image.shape)
                        if adjusted_points:
                            new_labels.append((class_id, adjusted_points))
                    augmented_labels = new_labels
                
                elif method == 'brightness':
                    augmented_image = self.adjust_brightness(augmented_image)
                    # 亮度调整不影响标签
                
                elif method == 'blur':
                    augmented_image = self.blur_image(augmented_image)
                    # 模糊不影响标签
                
                elif method == 'flip':
                    # 随机选择翻转方式
                    flip_code = random.choice([0, 1, -1])  # 0: 垂直, 1: 水平, -1: 水平垂直
                    augmented_image = self.flip_image(augmented_image, flip_code)
                    # 更新标签
                    new_labels = []
                    for class_id, points in augmented_labels:
                        flipped_points = self.flip_points(points, flip_code, image.shape)
                        new_labels.append((class_id, flipped_points))
                    augmented_labels = new_labels
            
            # 保存增强后的图像和标签
            base_name = os.path.basename(image_path)
            name_without_ext = os.path.splitext(base_name)[0]
            ext = os.path.splitext(base_name)[1]
            
            output_image_name = f"{name_without_ext}_aug{i}{ext}"
            output_label_name = f"{name_without_ext}_aug{i}.txt"
            
            output_image_path = os.path.join(self.image_output_dir, output_image_name)
            output_label_path = os.path.join(self.label_output_dir, output_label_name)
            
            # 保存图像
            cv2.imwrite(output_image_path, augmented_image)
            # 保存标签
            self.save_label(output_label_path, augmented_labels)
            
            print(f"保存增强图像: {output_image_path}")
            print(f"保存增强标签: {output_label_path}")
    
    def process_directory(self):
        """处理目录中的所有图像"""
        image_dir = os.path.join(self.input_dir, 'images')
        label_dir = os.path.join(self.input_dir, 'labels')
        
        # 获取所有图像文件
        image_files = [f for f in os.listdir(image_dir) if f.endswith(('.jpg', '.jpeg', '.png'))]
        
        print(f"找到 {len(image_files)} 张图像")
        
        for image_file in image_files:
            image_path = os.path.join(image_dir, image_file)
            label_file = os.path.splitext(image_file)[0] + '.txt'
            label_path = os.path.join(label_dir, label_file)
            
            print(f"处理: {image_file}")
            self.augment_image(image_path, label_path)

if __name__ == "__main__":
    input_dir = r'D:\TraeProject\yolo\yolo\TACO- Trash Annotations in Context Dataset.v16-resize640-aug3x-allclasses-mappedtotrash.yolov8\train'
    output_dir = r'D:\TraeProject\yolo\yolo\augmented_data_v16'
    factor = 3
    augmenter = DataAugmenter(input_dir, output_dir, factor)
    augmenter.process_directory()
    
    print("数据增强完成！")
