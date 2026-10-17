#!/usr/bin/env python3
"""FLUX VOICE 01: a self-hosted Brazilian Portuguese voice studio (experimental).

Models: MIT-licensed Chatterbox V3 + dedicated pt-BR checkpoints.
Never uses ElevenLabs, Google Chirp APIs, remote inference or paid fallback.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
import wave

MODEL = "ResembleAI/Chatterbox-Multilingual-pt-br"
ROOT = Path.home() / ".flux" / "voice-01"

class InvalidVoice(ValueError):
    pass

def sha(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def check_audio(path):
    if not path.is_file() or path.stat().st_size > 25_000_000:
        raise InvalidVoice("WAV ausente ou acima de 25 MB.")
    with wave.open(str(path), "rb") as w:
        if w.getnchannels() != 1 or w.getsampwidth() != 2 or w.getcomptype() != "NONE":
            raise InvalidVoice("Use WAV PCM 16-bit mono.")
        seconds = w.getnframes() / w.getframerate() if w.getframerate() else 0
        if not (16000 <= w.getframerate() <= 48000 and 6 <= seconds <= 35):
            raise InvalidVoice("Use 6 a 35 segundos, 16–48 kHz.")
        raw = w.readframes(w.getnframes())
    if not raw or raw.count(b"\x00") > len(raw) * 0.9:
        raise InvalidVoice("Amostra sem fala audível.")
    return round(seconds, 2)

def enroll(reference, rights, proof):
    duration = check_audio(reference)
    if rights == "licensed" and (proof is None or not proof.is_file()):
        raise InvalidVoice("Para clonar voz de outro locutor é obrigatória autorização expressa.")
    ROOT.mkdir(parents=True, exist_ok=True, mode=0o700)
    target = ROOT / "reference.wav"
    if reference.resolve() != target.resolve():
        shutil.copyfile(reference, target)
    target.chmod(0o600)
    profile = {
        "name": "FLUX VOICE 01 — JARVIS Elegante",
        "locale": "pt-BR", "model": MODEL, "rights": rights,
        "sha256": sha(target), "reference_seconds": duration,
        "proof_sha256": sha(proof) if proof else None,
        "status": "voice_reference_registered_not_model_trained",
    }
    path = ROOT / "voice-profile.json"
    path.write_text(json.dumps(profile, ensure_ascii=False, indent=2), encoding="utf-8")
    path.chmod(0o600)
    return profile

def identity():
    audio = ROOT / "reference.wav"
    data = ROOT / "voice-profile.json"
    if not audio.is_file() or not data.is_file():
        raise InvalidVoice("Cadastre primeiro uma referência vocal autorizada.")
    profile = json.loads(data.read_text(encoding="utf-8"))
    if profile.get("locale") != "pt-BR" or profile.get("model") != MODEL or profile.get("sha256") != sha(audio):
        raise InvalidVoice("Perfil inválido ou gravação alterada.")
    check_audio(audio)
    return profile, audio

def model_ptbr(device):
    # Import heavy libraries only during synthesis, not during enrolment.
    from chatterbox.mtl_tts import ChatterboxMultilingualTTS
    from huggingface_hub import snapshot_download
    from safetensors.torch import load_file
    model = ChatterboxMultilingualTTS.from_pretrained(device=device, t3_model="v3")
    directory = Path(snapshot_download(repo_id=MODEL, allow_patterns=[
        "t3_pt_br.safetensors", "s3gen_v3.safetensors"
    ]))
    for name in ("t3_pt_br.safetensors", "s3gen_v3.safetensors"):
        if not (directory / name).is_file():
            raise InvalidVoice("Pesos dedicados pt-BR não encontrados: " + name)
    try:
        model.t3.load_state_dict(load_file(str(directory / "t3_pt_br.safetensors")), strict=True)
        model.s3gen.load_state_dict(load_file(str(directory / "s3gen_v3.safetensors")), strict=True)
    except Exception as exc:
        raise InvalidVoice("Checkpoint pt-BR incompatível. Não usar fallback multilíngue.") from exc
    model.t3.to(device).eval()
    model.s3gen.to(device).eval()
    return model

def speak(text, output, device):
    _, reference = identity()
    text = " ".join(text.split())
    if not (1 <= len(text) <= 450):
        raise InvalidVoice("Use uma frase de 1 a 450 caracteres.")
    import torch
    import torchaudio
    if device == "auto":
        device = "cuda" if torch.cuda.is_available() else "cpu"
    print("Carregando modelo pt-BR local:", device, "(sem API paga)", flush=True)
    model = model_ptbr(device)
    audio = model.generate(
        text, language_id="pt", audio_prompt_path=str(reference),
        exaggeration=0.45, cfg_weight=0.32
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    torchaudio.save(str(output), audio.cpu(), model.sr)
    return output

def main():
    parser = argparse.ArgumentParser(description="FLUX VOICE 01 — estúdio vocal próprio")
    p = parser.add_subparsers(dest="action", required=True)
    en = p.add_parser("enroll")
    en.add_argument("--reference", required=True, type=Path)
    en.add_argument("--rights", required=True, choices=["own", "licensed"])
    en.add_argument("--license-document", type=Path)
    sp = p.add_parser("speak")
    sp.add_argument("--text", required=True)
    sp.add_argument("--output", type=Path, required=True)
    sp.add_argument("--device", choices=["auto", "cuda", "cpu"], default="auto")
    p.add_parser("status")
    a = parser.parse_args()
    try:
        if a.action == "enroll": result = enroll(a.reference, a.rights, a.license_document)
        elif a.action == "status": result = identity()[0]
        else: result = {"file": str(speak(a.text, a.output, a.device))}
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except (InvalidVoice, OSError, wave.Error, RuntimeError) as exc:
        print("FLUX VOICE 01: " + str(exc), file=sys.stderr)
        return 2
    return 0

if __name__ == "__main__":
    sys.exit(main())
