#!/usr/bin/env python3
"""Compare Fish Audio S2 demo TTS to official Google Chirp 3 pt-BR male voice examples.

No paid APIs. Fish generated through community-run public HF Space if it is running.
Google uses OFFICIAL prerecorded demo audio, not same-text synthesis.
"""
from pathlib import Path
import os, json, re, wave, subprocess, requests, traceback
from bs4 import BeautifulSoup
from gradio_client import Client
OUT=Path("voice-premium-evaluation")
OUT.mkdir(exist_ok=True)
STATUS={}
TEXT="Bom dia. Sou o Flux, seu assistente pessoal. Seus sistemas estão funcionando normalmente. Como posso ajudar você hoje?"
def mark(name,state,why=""):
    STATUS[name]={"state":state,"detail":str(why)[:380]}
    (OUT/"resultados.json").write_text(json.dumps(STATUS,ensure_ascii=False,indent=2),encoding="utf-8")
def valid(filename):
    p=subprocess.run(["ffprobe","-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",str(filename)],capture_output=True,text=True)
    if p.returncode: raise RuntimeError("Invalid audio file: "+p.stderr[-180:])
    duration=float(p.stdout.strip())
    if not 0.5 < duration < 180: raise RuntimeError(f"Audio duration outside expected range: {duration}")
    print("AUDIO VERIFIED",filename,round(duration,1),"seconds",filename.stat().st_size,"bytes",flush=True)
def store(url,path):
    r=requests.get(url,timeout=45,headers={"User-Agent":"Mozilla/5.0"})
    r.raise_for_status()
    typ=r.headers.get("content-type","")
    if not (("audio" in typ) or r.content[:4] in (b"RIFF",b"OggS",b"fLaC") or r.content[:3]==b"ID3" or r.content[:2]==b"\xff\xfb"):
        raise RuntimeError("Sample URL did not return audio: "+typ)
    path.write_bytes(r.content)
    valid(path)

def google_samples():
    u="https://cloud.google.com/text-to-speech/docs/voices"
    page=requests.get(u,timeout=30,headers={"User-Agent":"Mozilla/5.0"});page.raise_for_status()
    soup=BeautifulSoup(page.text,"html.parser")
    print("GOOGLE HTML",len(page.text),"voice count",page.text.count("pt-BR-Chirp3-HD-Charon"),flush=True)
    targetnames=["Charon","Orus","Algieba"]
    for name in targetnames:
        code="pt-BR-Chirp3-HD-"+name
        try:
            rows=soup.find_all("tr")
            matches=[row for row in rows if code in row.get_text(" ",strip=True)]
            if not matches: raise RuntimeError("No rendered row for "+code)
            row=matches[0]
            urls=[]
            for tag in row.find_all(["source","audio"]):
                for attr in ["src","data-src"]:
                    if tag.get(attr): urls.append(tag.get(attr))
            if not urls:
                for a in row.find_all("a",href=True):
                    if any(x in a["href"] for x in [".mp3",".wav",".ogg",".flac"]): urls.append(a["href"])
            if not urls:
                print("GOOGLE HTML AUDIO ROW",str(row)[:1100],flush=True)
                raise RuntimeError("No audio source in official pt-BR row")
            from urllib.parse import urljoin, urlparse
            src=urls[0]
            ext=Path(urlparse(src).path).suffix.lower()
            if ext not in (".mp3",".wav",".ogg",".flac"): ext=".mp3"
            target=OUT/("Google-Chirp3-HD-ptBR-"+name+ext)
            tried=[]
            for origin in ("https://docs.cloud.google.com","https://cloud.google.com"):
                candidate=urljoin(origin,src)
                try:
                    store(candidate,target)
                    mark(name,"success","Official prerecorded pt-BR sample (not user TEXT) from "+candidate)
                    break
                except Exception as err:
                    tried.append(str(err)[:150])
            else: raise RuntimeError("; ".join(tried))
        except Exception as e:
            print("GOOGLE SAMPLE ERROR",name,str(e)[:450],flush=True)
            mark(name,"unavailable",e)
try:google_samples()
except Exception as e:
    print("GOOGLE FETCH ERROR",type(e).__name__,str(e)[:300],flush=True)
    mark("Google-Chirp","unavailable",e)

def fish_sample():
    space="artificialguybr/fish-s2-pro-zero"
    client=Client(space,verbose=False)
    api=client.view_api(return_format="dict", print_info=False)
    print("FISH ENDPOINTS:",list(api.get("named_endpoints",{}).keys()),flush=True)
    for endpoint,props in api.get("named_endpoints",{}).items():
        print("ENDPOINT",endpoint,"ARGS",[(p.get("parameter_name"),p.get("parameter_default")) for p in props.get("parameters",[])],flush=True)
    endpoints=[(n,p) for n,p in api.get("named_endpoints",{}).items() if len(p.get("parameters",[]))==8 and p.get("returns")]
    if not endpoints:raise RuntimeError("Fish demo did not expose a usable public 8-param inference endpoint")
    endpoint=endpoints[0][0]
    result=client.predict(TEXT,None,"",1024,200,0.7,1.2,0.7,api_name=endpoint)
    print("FISH RESULT",str(result)[:350],flush=True)
    if isinstance(result, (list,tuple)):result=result[0]
    if isinstance(result,dict):result=result.get("path") or result.get("url")
    src=Path(result)
    if not src.is_file(): raise RuntimeError("Fish did not return downloaded audio")
    import shutil
    target=OUT/("Fish-Audio-S2-Pro-public-demo"+src.suffix.lower())
    shutil.copy(src,target)
    valid(target)
    mark("Fish-Audio-S2-Pro","success","Live generated same TEXT without cloning. pt-BR/accent and gender NOT guaranteed.")
try:fish_sample()
except Exception as e:
    print("FISH ERROR",type(e).__name__,str(e)[:500],flush=True)
    traceback.print_exc(limit=2)
    mark("Fish-Audio-S2-Pro","unavailable",e)
(OUT/"LEIA-ME.txt").write_text(
    "FLUX VOICE 01 — comparação premium 17/10/2026\n"
    "Fish S2 Pro: geração da frase \n"+TEXT+"\n"
    "Google Chirp 3 HD: amostras pregravadas oficiais para vozes pt-BR; NÃO dizem a mesma frase.\n"
    "Nenhum desses áudios significa que uma conta de serviço paga foi ativada.\n"
    "Valores: Google 1 milhão de caracteres mensais gratuitos, depois US$30 por milhão.\n"
    "Fish S2.1 Pro API: US$15 por milhão de bytes UTF-8, sem mensalidade obrigatória; testes e franquias sujeitos aos termos.\n"
    "Nada foi integrado no FLUX ainda.\n",encoding="utf-8")
print("FINAL STATUS",json.dumps(STATUS,ensure_ascii=False),flush=True)
