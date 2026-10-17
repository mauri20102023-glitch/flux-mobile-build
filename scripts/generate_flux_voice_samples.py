#!/usr/bin/env python3
"""Generate user-requested FLUX VOICE 01 samples via public Hugging Face demos.

No API secrets; no subscriptions. Audio is stored as GitHub Actions artifacts, not
committed to app code. This is a preview, not a running Android TTS engine.
"""
from gradio_client import Client, handle_file
from concurrent.futures import TimeoutError as FutureTimeout
from pathlib import Path
import shutil
import json
import time
import traceback
import subprocess

OUT = Path("flux-voice-preview-audios")
OUT.mkdir(exist_ok=True)
TEXT = (
    "Bom dia. Eu sou o Flux, seu assistente pessoal. "
    "Seus sistemas estão funcionando normalmente. "
    "Já organizei suas prioridades. Como posso ajudar você hoje?"
)
VOICE = (
    "A genuine native Brazilian Portuguese adult male voice around 40 years old, "
    "a smooth medium-low baritone, unmistakably masculine, natural, warm and refined. "
    "Subtle calm confidence, sophisticated intelligence, clean clear articulation, "
    "fluid modern Brazilian Portuguese conversational accent and normal speed. "
    "Realistic pacing and intonation with modest pauses. No robotic sound, no theatrical "
    "movie narration, no distorted bass, no drawn-out syllables, no exaggeration. "
    "Like a thoughtful highly competent personal assistant chatting naturally."
)
STATUS = {}
def status(name, state, message=""):
    STATUS[name] = {"status": state, "message": str(message)[:450]}
    (OUT/"status.json").write_text(json.dumps(STATUS, indent=2, ensure_ascii=False))

def do_submit(client, api_name, args, timeout=300):
    print("CALL", api_name, "with", len(args), "arguments", flush=True)
    job = client.submit(*args, api_name=api_name)
    print("JOB SUBMITTED", job, flush=True)
    return job.result(timeout=timeout)

def media_path(result):
    if isinstance(result, (list, tuple)):
        for item in result:
            p=media_path(item)
            if p: return p
        return None
    if isinstance(result, dict):
        p=result.get("path") or result.get("url")
        return p if p and isinstance(p,str) else None
    if isinstance(result,str) and result.endswith((".wav",".mp3",".flac",".ogg",".m4a")):
        return result
    return None

def copy_audio(path, filename):
    if not path: raise RuntimeError("Demo returned no audio file")
    src=Path(path)
    if not src.is_file(): raise RuntimeError("Audio not downloaded locally: "+str(path)[:150])
    dst=OUT/filename
    shutil.copyfile(src,dst)
    p=subprocess.run(["ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1:nk=1",str(dst)],capture_output=True,text=True)
    if p.returncode != 0: raise RuntimeError("Not valid audio: "+p.stderr[:200])
    print("AUDIO OK", dst, "duration", p.stdout.strip(), "size", dst.stat().st_size,flush=True)
    return dst

def task(label, fn):
    try:
        status(label,"running")
        result=fn()
        status(label,"success",result)
        return result
    except Exception as e:
        status(label,"failed",str(e))
        print("ERROR", label, type(e).__name__,str(e)[:450],flush=True)
        traceback.print_exc(limit=2)
        return None

qwen=None
qwenClient=None
def qwen_design():
    global qwenClient
    qwenClient=Client("Qwen/Qwen3-TTS", verbose=False)
    result=do_submit(qwenClient, "/generate_voice_design", (TEXT,"Portuguese",VOICE), timeout=400)
    print("QWEN RESPONSE:",str(result)[:300],flush=True)
    return str(copy_audio(media_path(result),"01-Qwen-VoiceDesign.wav"))
qwen=task("qwen_voice_design", qwen_design)

# A second option: male preset with Portuguese instructions, in case
# free VoiceDesign is limited.
def qwen_custom():
    c=qwenClient or Client("Qwen/Qwen3-TTS", verbose=False)
    result=do_submit(c, "/generate_custom_voice", (
        TEXT,"Portuguese","Ryan",
        "Speak calm and naturally. Native Brazilian Portuguese, warm low masculine timbre, clear enunciation, neutral speed, no dramatic tone.",
        "1.7B"
    ),timeout=260)
    print("QWEN CUSTOM RESPONSE:",str(result)[:300],flush=True)
    return str(copy_audio(media_path(result),"02-Qwen-MalePreset.wav"))
# Generate the alternate only when the first one fails or if demo queues are free.
if not qwen:
    qwen=task("qwen_male_preset", qwen_custom)
else:
    status("qwen_male_preset","skipped","VoiceDesign already generated successfully")

def chatterbox():
    if not qwen:
        raise RuntimeError("Chatterbox pt-br needs an original male reference; not using the female default voice")
    c=Client("ResembleAI/Chatterbox-Multilingual-TTS-pt-br",verbose=False)
    result=do_submit(c, "/generate_tts_audio", (
      TEXT, handle_file(qwen), 0.42, 0.75, 42, 0.4
    ), timeout=300)
    print("CHATTERBOX RESPONSE:",str(result)[:300],flush=True)
    return str(copy_audio(media_path(result),"03-Chatterbox-ptBR-QwenReference.wav"))
if qwen: task("chatterbox_with_male_reference", chatterbox)
else: status("chatterbox_with_male_reference","not_attempted","Male reference unavailable")

(OUT/"LEIA-ME.txt").write_text(
    "FLUX VOICE 01 — prévias Qwen3-TTS e Chatterbox pt-BR\n\n"
    "Frase: "+TEXT+"\n\n"
    "01: Qwen3-TTS VoiceDesign: criação original por descrição.\n"
    "02: Qwen3-TTS male preset Ryan (alternativa se VoiceDesign indisponível).\n"
    "03: Chatterbox pt-BR usando áudio masculino gerado pelo Qwen como referência.\n\n"
    "São testes de áudio feitos nas demonstrações públicas, não a voz definitiva instalada no FLUX.\n"
    "Não usam ElevenLabs; sem assinatura de voz.\n"
    "A qualidade, sotaque e consistência devem ser avaliados ouvindo os arquivos.\n",
    encoding="utf-8"
)
files=list(OUT.glob("*.wav"))
print("FINAL RESULT",len(files),"audio sample(s).", json.dumps(STATUS,ensure_ascii=False),flush=True)
if not files: raise RuntimeError("No voice audio was produced from free demos")
