# Use a CPU-only base image with Python 3.10
FROM python:3.10-slim

# Set environment variables to avoid interactive prompts during installation
ENV DEBIAN_FRONTEND=noninteractive

# Install system dependencies for audio processing and compilation
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libsndfile1 \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy your application files
COPY app.py .

# Expose the FastAPI port
EXPOSE 5050

# Run the FastAPI app with Uvicorn
CMD ["uvicorn", "app.py:app", "--host", "0.0.0.0", "--port", "5050"]