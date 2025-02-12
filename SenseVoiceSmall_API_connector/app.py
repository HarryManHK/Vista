import requests

url = "https://api.siliconflow.cn/v1/audio/transcriptions"
api_token = "key"  # 替换成你的 API token

headers = {
    "Authorization": f"Bearer {api_token}"
}

# 设置要上传的音频文件路径
audio_file_path = "1.mp3"  # 替换为你的音频文件路径

files = {
    "file": open(audio_file_path, "rb")
}
data = {
    "model": "FunAudioLLM/SenseVoiceSmall"
}

response = requests.post(url, headers=headers, files=files, data=data)

print(response.text)