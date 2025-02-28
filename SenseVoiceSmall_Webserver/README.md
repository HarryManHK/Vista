# Real-Time Speech-to-Text API

This project provides a speech-to-text service using the **SenseVoiceSmall** model for automatic speech recognition (ASR). The service is built with **FastAPI** and uses **Uvicorn** as the ASGI server.

## Requirements

- Python 3.8+
- CUDA-enabled GPU (if using the `cuda:0` device for running the model)

## Setup

1. **Clone the repository** (or place all files in your project folder).

2. **Create a virtual environment** (optional but recommended):
    ```bash
    python3 -m venv venv
    source venv/bin/activate  # On Windows, use `venv\Scripts\activate`
    ```

3. **Install the dependencies**:
    ```bash
    pip install -r requirements.txt
    ```

4. **Download the necessary model files**:
    - The application requires the `SenseVoiceSmall` model. You may need to download it from the model repository or a specific location and place it in the appropriate directory.
    - Ensure that the `remote_code` path in the code (`"./model.py"`) points to the correct location of the model's Python code.

## Running the API

Once the dependencies are installed and the model is set up, you can run the API using **Uvicorn**.

Run the following command to start the server:

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 2002