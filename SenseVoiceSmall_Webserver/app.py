from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import base64
import uvicorn
from funasr import AutoModel
from funasr.utils.postprocess_utils import rich_transcription_postprocess
from pydantic import BaseModel

# Create FastAPI app
app = FastAPI()

# Add CORS middleware to allow cross-origin requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins; for production, specify allowed domains
    allow_credentials=True,
    allow_methods=["*"],  # Allows all HTTP methods (GET, POST, OPTIONS, etc.)
    allow_headers=["*"],  # Allows all headers
)

# ASR model
model = AutoModel(
    model="iic/SenseVoiceSmall",
    trust_remote_code=True,
    remote_code="./model.py",
    vad_model="fsmn-vad",
    vad_kwargs={"max_single_segment_time": 30000},
    device="cuda:0",
)

# Define ASR data model to receive data from POST request
class ASRItem(BaseModel):
    wav: str  # Input audio as base64

@app.post("/asr")
async def asr(item: ASRItem):
    try:
        data = base64.b64decode(item.wav)
        with open("test.wav", "wb") as f:
            f.write(data)
        res = model.generate("test.wav", 
                            language="auto",  # "zn", "en", "yue", "ja", "ko", "nospeech"
                            use_itn=True,
                            batch_size_s=60,
                            merge_vad=True,  #
                            merge_length_s=15,)
        text = rich_transcription_postprocess(res[0]["text"])
        result_dict = {"code": 0, "msg": "ok", "res": text}
    except Exception as e:
        result_dict = {"code": 1, "msg": str(e)}
    return result_dict

if __name__ == '__main__':
    uvicorn.run(app, host='0.0.0.0', port=2002)