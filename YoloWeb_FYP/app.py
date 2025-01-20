from flask import Flask, render_template
from flask_socketio import SocketIO, emit
import base64
import cv2
import numpy as np
from ultralytics import YOLO

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins='*')

# 加载您的自定义 YOLOv8 模型
model = YOLO('best.pt')

@app.route('/')
def index():
    return render_template('index.html')

@socketio.on('image')
def handle_image(data_image):
    try:
        # 解码来自客户端的图像数据
        img_data = base64.b64decode(data_image)
        np_arr = np.frombuffer(img_data, dtype=np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        # 使用 YOLOv8 模型进行物体检测
        results = model(img)

        # 准备检测结果以发送回客户端
        detections = []
        for result in results:
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                confidence = box.conf[0].item()
                class_id = int(box.cls[0].item())
                class_name = model.names[class_id]
                detection = {
                    'xmin': x1,
                    'ymin': y1,
                    'xmax': x2,
                    'ymax': y2,
                    'confidence': confidence,
                    'class_id': class_id,
                    'name': class_name
                }
                detections.append(detection)

        # 将结果发送回客户端
        emit('detections', {'detections': detections})
    except Exception as e:
        print(f"处理图像时出错：{e}")

if __name__ == '__main__':
    socketio.run(app, host='0.0.0.0', port=5050)