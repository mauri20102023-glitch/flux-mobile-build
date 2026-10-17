import importlib.util
import math
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import wave

spec=importlib.util.spec_from_file_location("studio",Path(__file__).with_name("studio.py"))
studio=importlib.util.module_from_spec(spec)
spec.loader.exec_module(studio)

def make_wav(path,seconds=7,stereo=False):
    rate=16000
    with wave.open(str(path),"wb") as wav:
        wav.setnchannels(2 if stereo else 1)
        wav.setsampwidth(2)
        wav.setframerate(rate)
        data=[]
        for i in range(int(rate*seconds)):
            s=int(3500*math.sin(2*math.pi*220*i/rate))
            data.extend((s,s) if stereo else (s,))
        wav.writeframes(struct.pack("<"+"h"*len(data),*data))

class TestVoiceStudio(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        self.audio=self.root/"reference.wav"
        make_wav(self.audio)
        self.patcher=patch.object(studio,"ROOT",self.root/"private")
        self.patcher.start()
        self.addCleanup(self.patcher.stop)

    def test_ptbr_profile_and_hash(self):
        p=studio.enroll(self.audio,"own",None)
        self.assertEqual(p["locale"],"pt-BR")
        self.assertEqual(studio.identity()[0]["sha256"],p["sha256"])

    def test_other_voice_requires_license(self):
        with self.assertRaises(studio.InvalidVoice):
            studio.enroll(self.audio,"licensed",None)
        proof=self.root/"consent.txt"
        proof.write_text("speaker authorized AI cloning",encoding="utf-8")
        p=studio.enroll(self.audio,"licensed",proof)
        self.assertTrue(p["proof_sha256"])

    def test_invalid_audio_rejected(self):
        stereo=self.root/"stereo.wav"
        make_wav(stereo,stereo=True)
        with self.assertRaises(studio.InvalidVoice):
            studio.check_audio(stereo)
        short=self.root/"short.wav"
        make_wav(short,seconds=2)
        with self.assertRaises(studio.InvalidVoice):
            studio.check_audio(short)

    def test_audio_integrity(self):
        studio.enroll(self.audio,"own",None)
        with (studio.ROOT/"reference.wav").open("ab") as f:
            f.write(b"tampered")
        with self.assertRaises(studio.InvalidVoice):
            studio.identity()

    def test_explicit_brazilian_weights_only(self):
        self.assertEqual(studio.MODEL,"ResembleAI/Chatterbox-Multilingual-pt-br")
        import inspect
        src=inspect.getsource(studio.model_ptbr)
        self.assertIn("t3_pt_br.safetensors",src)
        self.assertIn("s3gen_v3.safetensors",src)
        self.assertIn("strict=True",src)
        self.assertNotIn("except Exception: pass",src)

if __name__=="__main__":
    unittest.main()
