# SenseVoiceSmall API Connector

本專案示範如何使用 Python 呼叫 SiliconFlow 的語音轉文字 API，並利用 FunAudioLLM/SenseVoiceSmall 模型對音訊檔案進行轉錄。

## 功能簡介

- 透過 HTTP POST 請求上傳音訊檔案至 SiliconFlow API 進行轉錄
- 支援 MP3、WAV 等常見音訊格式
- 輸出轉錄後的文字

## 前置條件

- Python 3.7 及以上版本
- 已安裝 `pip` 套件管理工具

## 安裝依賴

在專案根目錄下執行以下命令以安裝依賴庫：

```bash
pip install -r requirements.txt