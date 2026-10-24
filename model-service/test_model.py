import requests

# 测试模型推理服务
url = "http://localhost:8000/predict"

# 选择一张测试图片
file_path = "../model-training/data/images/train/plastic_bottle_1.jpg"

# 发送POST请求
with open(file_path, "rb") as f:
    files = {"file": ("plastic_bottle_1.jpg", f, "image/jpeg")}
    response = requests.post(url, files=files)

# 打印响应结果
print("Status Code:", response.status_code)
print("Response:", response.json())
