#!/usr/bin/env python3
"""FLUX VOICE 01 - pt-BR accent correction audition via free public demos.
No subscriptions, no API keys. Output quality and accent require human listening.
"""
from gradio_client import Client, handle_file
from pathlib import Path
import json, wave, shutil, traceback

OUT = Path("flux-voice-ptbr-audios")
OUT.mkdir(exist_ok=True)
TEXT = ("Bom dia. Eu sou o Flux. Já organizei sua agenda e as prioridades da Mauright. "
        "Posso mostrar o que precisa da sua atenção hoje?")

DESCRIPTIONS = [
("01-Qwen-paulistano",
 """A distinctly BRAZILIAN man born, raised and living in the city of Sao Paulo, Brazil.
 He is 38 to 45 years old and speaks native contemporary BRAZILIAN PORTUGUESE with a natural
 Sao Paulo Brazilian accent. NOT European Portuguese, NOT Portugal, NOT Lisbon,
 NOT Portuguese from Portugal. Pronounce everyday words like 'você', 'dia',
 'atenção', 'hoje', 'agenda', 'ajudar' as a real person from Sao Paulo, Brazil would.
 Medium-low smooth male baritone, mature, refined but casual, clear, calm and warm,
 intelligent personal assistant, understated confidence, completely natural speed,
 no exaggerated gravity, no robotic or theatrical voice, no British or European accent."""),
("02-Qwen-brasileiro",
 """Voz de um homem brasileiro, nascido e criado no Brasil, com sotaque brasileiro
 natural de Sao Paulo, cerca de quarenta anos. Portugues do BRASIL, jamais portugues
 europeu nem sotaque de Portugal. Timbre masculino medio-grave, voz acolhedora,
 sofisticada, segura e inteligente, como um assistente pessoal moderno brasileiro.
 Falar do jeito que um brasileiro fala no dia a dia, com entonacao humana, vogais
 brasileiras, boa diccao, velocidade natural e pausas discretas. Sem narracao de
 trailer, sem voz robotica ou graves artificiais, sem prolongar vogais."""),
]
status={}
def mark(k,v,error=""):
    status[k]={"status":v,"message":str(error)[:500]}
    (OUT/"resultados.json").write_text(json.dumps(status,indent=2,ensure_ascii=False),encoding="utf-8")

def save_result(response, filename):
    path=response[0] if isinstance(response, (tuple,list)) else response
    if isinstance(path,dict): path=path.get("path") or path.get("url")
    assert isinstance(path,str) and Path(path).is_file(), f"Expected local file: {str(path)[:150]}"
    target=OUT/(filename+".wav")
    shutil.copy(path,target)
    with wave.open(str(target),"rb") as wav:
        seconds=wav.getnframes()/wav.getframerate()
    assert 1<seconds<45
    print("AUDIO OK",target,round(seconds,2),"sec",target.stat().st_size,"bytes",flush=True)
    return target

qwen=Client("Qwen/Qwen3-TTS",verbose=False)
generated={}
for label,description in DESCRIPTIONS:
    mark(label,"running")
    try:
        print("Generating",label,flush=True)
        audio=qwen.predict(TEXT,"Portuguese",description,api_name="/generate_voice_design")
        target=save_result(audio,label)
        generated[label]=target
        mark(label,"success",target)
    except Exception as exc:
        print("ERROR",label,type(exc).__name__,str(exc)[:300],flush=True)
        traceback.print_exc(limit=2)
        mark(label,"failed",exc)

if generated:
    sample=generated.get("01-Qwen-paulistano") or next(iter(generated.values()))
    label="03-Chatterbox-ptBR"
    mark(label,"running")
    try:
        chatterbox=Client("ResembleAI/Chatterbox-Multilingual-TTS-pt-br",verbose=False)
        print("Generating",label,"with reference",sample,flush=True)
        audio=chatterbox.predict(TEXT,handle_file(str(sample)),0.48,0.7,20261017,0.4,api_name="/generate_tts_audio")
        mark(label,"success",save_result(audio,label))
    except Exception as exc:
        print("ERROR",label,type(exc).__name__,str(exc)[:300],flush=True)
        traceback.print_exc(limit=2)
        mark(label,"failed",exc)

(OUT/"LEIA-ME.txt").write_text(
    "FLUX VOICE 01 - português BRASILEIRO, segunda rodada (sotaque corrigido)\n\n"
    "Frase: "+TEXT+"\n\n"
    "01-Qwen-paulistano: descricao em ingles exigindo sotaque brasileiro de Sao Paulo.\n"
    "02-Qwen-brasileiro: descricao em portugues exigindo sotaque do Brasil.\n"
    "03-Chatterbox-ptBR: voz gerada pelo modelo pt-BR com referencia masculina 01.\n"
    "Comparar ouvindo; o nome nao garante que o sotaque foi corretamente reproduzido.\n"
    "Amostras gratuitas de demos Qwen/Chatterbox, sem ElevenLabs.\n",encoding="utf-8")
print("FINAL",json.dumps(status,ensure_ascii=False),flush=True)
assert generated, "No Qwen voice generated"
