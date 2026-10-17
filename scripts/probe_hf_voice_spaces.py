#!/usr/bin/env python3
"""Probe public Hugging Face Gradio demo API contracts without credentials.

No voice-generation call, download of models, or billable inference.
"""
from gradio_client import Client
import json, traceback

SPACES = [
    "Qwen/Qwen3-TTS",
    "ResembleAI/Chatterbox-Multilingual-TTS-pt-br",
]
for space in SPACES:
    print("\n================== SPACE", space, "==================", flush=True)
    try:
        client = Client(space, verbose=False)
        api = client.view_api(return_format="dict", print_info=False)
        print(json.dumps(api, ensure_ascii=False, indent=2, default=str)[:25000], flush=True)
    except Exception as e:
        print("PROBE ERROR:", type(e).__name__, str(e)[:1200], flush=True)
        traceback.print_exc(limit=2)
