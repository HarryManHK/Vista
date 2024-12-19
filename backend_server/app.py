from flask import Flask, request, jsonify, render_template
import requests
import base64

app = Flask(__name__)

# Your AI model endpoint details
INFERENCE_API_ENDPOINT = "http://192.168.1.245:1234/v1/chat/completions"
MODEL_NAME = "minicpm-v-2_6@q5_0"

@app.route("/", methods=["GET"])
def index():
    """
    Renders the homepage with a file upload form.
    """
    return render_template("index.html")


@app.route("/api/image2text", methods=["POST"])
def image_to_text():
    """
    Receives an image from the HTML form (multipart/form-data),
    sends it to the AI model endpoint, and returns JSON with the inference result.
    """
    if "image_file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    image_file = request.files["image_file"]
    if image_file.filename == '':
        return jsonify({"error": "Empty file name"}), 400

    # Read the file and convert to Base64
    file_bytes = image_file.read()
    base64_str = base64.b64encode(file_bytes).decode("utf-8")

    # Construct the payload for the AI model
    payload = {
        "model": MODEL_NAME,
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": "What is this image?"
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            # Data URI with Base64
                            "url": f"data:image/png;base64,{base64_str}"
                        }
                    }
                ]
            }
        ],
        "temperature": 0.7,
        "max_tokens": -1,
        "stream": False
    }

    # Send request to AI model
    try:
        response = requests.post(INFERENCE_API_ENDPOINT, json=payload, timeout=3000)
        response.raise_for_status()
        ai_result = response.json()
    except requests.RequestException as e:
        return jsonify({"error": str(e)}), 500
    except ValueError:
        return jsonify({"error": "Failed to parse AI server response as JSON"}), 500

    return jsonify(ai_result), 200


if __name__ == "__main__":
    # Run the Flask app (dev server)
    app.run(host="0.0.0.0", port=5050, debug=True)