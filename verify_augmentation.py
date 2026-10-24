import os
import cv2
import numpy as np

class AugmentationVerifier:
    def __init__(self, image_dir, label_dir):
        self.image_dir = image_dir
        self.label_dir = label_dir
    
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
    
    def draw_bounding_boxes(self, image_path, label_path):
        """在图像上绘制边界框"""
        # 读取图像
        image = cv2.imread(image_path)
        if image is None:
            print(f"无法读取图像: {image_path}")
            return None
        
        # 读取标签
        labels = self.load_label(label_path)
        
        h, w = image.shape[:2]
        
        # 绘制每个目标的边界框
        for class_id, points in labels:
            # 计算边界框的最小和最大坐标
            if len(points) >= 2:
                # 对于多边形，计算边界矩形
                x_coords = [p[0] for p in points]
                y_coords = [p[1] for p in points]
                min_x = min(x_coords)
                max_x = max(x_coords)
                min_y = min(y_coords)
                max_y = max(y_coords)
                
                # 转换为像素坐标
                x1 = int(min_x * w)
                y1 = int(min_y * h)
                x2 = int(max_x * w)
                y2 = int(max_y * h)
                
                # 绘制矩形
                cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 2)
                # 绘制类别ID
                cv2.putText(image, str(class_id), (x1, y1 - 10), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
            else:
                # 对于单点，绘制一个小圆点
                for x, y in points:
                    pixel_x = int(x * w)
                    pixel_y = int(y * h)
                    cv2.circle(image, (pixel_x, pixel_y), 5, (0, 255, 0), -1)
        
        return image
    
    def verify_directory(self, output_dir=None):
        """验证目录中的所有图像"""
        # 获取所有图像文件
        image_files = [f for f in os.listdir(self.image_dir) if f.endswith(('.jpg', '.jpeg', '.png'))]
        
        print(f"找到 {len(image_files)} 张图像")
        
        if output_dir:
            os.makedirs(output_dir, exist_ok=True)
        
        for i, image_file in enumerate(image_files):
            image_path = os.path.join(self.image_dir, image_file)
            label_file = os.path.splitext(image_file)[0] + '.txt'
            label_path = os.path.join(self.label_dir, label_file)
            
            print(f"处理 {i+1}/{len(image_files)}: {image_file}")
            
            # 绘制边界框
            result_image = self.draw_bounding_boxes(image_path, label_path)
            
            if result_image is not None:
                if output_dir:
                    # 保存结果图像
                    output_path = os.path.join(output_dir, f"verified_{image_file}")
                    cv2.imwrite(output_path, result_image)
                    print(f"保存验证结果: {output_path}")
                else:
                    # 显示结果图像
                    cv2.imshow(f"验证: {image_file}", result_image)
                    # 按任意键继续
                    cv2.waitKey(0)
                    cv2.destroyAllWindows()
        
        if not output_dir:
            cv2.destroyAllWindows()

if __name__ == "__main__":
    image_dir = r'D:\TraeProject\yolo\yolo\augmented_data\images'
    label_dir = r'D:\TraeProject\yolo\yolo\augmented_data\labels'
    verifier = AugmentationVerifier(image_dir, label_dir)
    output_dir = r'D:\TraeProject\yolo\yolo\augmented_data\canvas_verify'
    verifier.verify_directory(output_dir)
    
    print("验证完成！")
