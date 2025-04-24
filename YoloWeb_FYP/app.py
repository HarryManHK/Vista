from flask import Flask, render_template
from flask_socketio import SocketIO, emit
import base64
import cv2
import numpy as np
from ultralytics import YOLO

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins='*')

# Load your YOLOv8 models once at startup
model_circle = YOLO('CircleBusStop.pt')
model_big    = YOLO('BigBusStop.pt')
model_bus    = YOLO('yolov8m.pt')   # new medium model for "bus"

@app.route('/')
def index():
    return render_template('index.html')

@socketio.on('image')
def handle_image(data_image):
    """Detect circle- and big-bus stops and emit both sets together."""
    try:
        # Decode incoming base64 image
        img_data = base64.b64decode(data_image)
        np_arr   = np.frombuffer(img_data, dtype=np.uint8)
        img      = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        # Run both models
        results_circle = model_circle(img)
        results_big    = model_big(img)

        # Helper to extract detections
        def extract(results, names):
            dets = []
            for res in results:
                for box in res.boxes:
                    x1, y1, x2, y2 = box.xyxy[0].tolist()
                    dets.append({
                        'xmin':       x1,
                        'ymin':       y1,
                        'xmax':       x2,
                        'ymax':       y2,
                        'confidence': float(box.conf[0].item()),
                        'class_id':   int(box.cls[0].item()),
                        'name':       names[int(box.cls[0].item())]
                    })
            return dets

        detections_circle = extract(results_circle, model_circle.names)
        detections_big    = extract(results_big,    model_big.names)

        # Emit both results in one payload
        emit('detections', {
            'circle': detections_circle,
            'big':    detections_big
        })

    except Exception as e:
        print(f"Error processing image: {e}")

@socketio.on('bus')
def handle_bus(data_image):
    """Detect only objects of class 'bus' using YOLOv8m and emit them."""
    try:
        # Decode incoming base64 image
        img_data = base64.b64decode(data_image)
        np_arr   = np.frombuffer(img_data, dtype=np.uint8)
        img      = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        results = model_bus(img)
        detections = []
        for res in results:
            for box in res.boxes:
                class_id   = int(box.cls[0].item())
                class_name = model_bus.names[class_id]
                if class_name.lower() == 'bus':
                    x1, y1, x2, y2 = box.xyxy[0].tolist()
                    detections.append({
                        'xmin':       x1,
                        'ymin':       y1,
                        'xmax':       x2,
                        'ymax':       y2,
                        'confidence': float(box.conf[0].item()),
                        'class_id':   class_id,
                        'name':       class_name
                    })

        emit('bus', {'detections': detections})

    except Exception as e:
        print(f"Error in bus handler: {e}")

if __name__ == '__main__':
    socketio.run(app, host='0.0.0.0', port=5050)