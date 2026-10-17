# FLUX VOICE 01 — primeiro protótipo Android, sem mensalidade

**Status: integração Android experimental em branch de avaliação, não testada no celular do proprietário.** Não altera o FLUX publicado.

## O que foi implementado
- Novo modo **FLUX VOICE 01 — voz neural local (experimental)** em Ajustes.
- Android se conecta **exclusivamente** ao `TextToSpeechService` do Sherpa-ONNX com package `com.k2fsa.sherpa.onnx.tts.engine`. Não usa a voz do Google, nem faz fallback silencioso para a rota de síntese paga do Core. Se não encontrar o serviço ou o idioma, informa erro.
- Quando ativo, o controlador de voz faz **reconhecimento Android → texto no FLUX Core → síntese neural pelo motor instalado no Android**.
- Modo FLUX Live/WebRTC original é preservado; a voz offline é opt-in. Alterar o modo encerra a sessão anterior.
- Nenhum modelo pesado ou binário baixado automaticamente, nenhuma chave embutida, nenhum valor pago.

## Como testar no telefone (após CI e instalação de APK de avaliação)
1. **Antes de instalar qualquer APK externo**, confirme a origem e assinatura do motor, arquitetura do aparelho, licença do modelo, permissões e possibilidade de remoção.
2. Verifique na **lista oficial de APKs de TTS engine do sherpa-onnx** a versão **Android arm64 / Português Brasileiro**, modelo masculino pré-configurado `pt_BR-jeff-medium`, `pt_BR-cadu-medium` ou `pt_BR-miro-high`:
   https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html
   A página lista pacotes oficiais por versão, arquitetura, idioma e modelo. Os APKs do tipo **tts-engine**, diferentemente dos de demonstração tipo **tts**, expõem serviço TextToSpeech para apps como o FLUX. Cada APK deve fornecer um serviço com package `com.k2fsa.sherpa.onnx.tts.engine`; confirmar após instalar. Atenção: APK diferente pode ter assinatura/app ID diferente. Não reinstalar apps sem conferir integridade.
3. Nas configurações **Saída de texto para fala** do Android, selecione **Sherpa-ONNX** como **motor padrão** e teste a voz fora do FLUX. Isto é necessário para impedir fallback silencioso do Android a outro motor quando o serviço solicitado falhar. Confirme que fala português brasileiro offline.
4. Instale a versão de **avaliação** do FLUX nesta branch e abra **Ajustes → Inteligência e Voz → FLUX VOICE 01**.
5. Ative o toggle, toque em **Iniciar teste de voz**, fale uma frase. Motor externo deve falar a resposta em pt-BR, sem depender de computador ligado.
6. Desative o modo e confirme que o FLUX Live anterior continua com o comportamento original.
7. Faça 10 testes de frase: números, marcas, nomes brasileiros, inglês, perguntas curtas/longas, períodos com pausa. Registre qualidade, aquecimento, RAM, interrupção, latência de início, tempo total e áudio com tela bloqueada.

## Limitações que NÃO podem ser mascaradas
- Ainda **não é uma voz totalmente original clonada**. É integração a motor neural local com **vozes predefinidas**, candidato inicial para FLUX VOICE 01. A identidade vocal única será uma etapa posterior quando houver um modelo compatível com Android em velocidade aceitável.
- **O computador pode ficar desligado**, pois a síntese ocorre no celular, mas **o FLUX Core e o reconhecimento por voz podem necessitar internet**, dependendo de disponibilidade local do Android.
- Motor e modelo externos são necessários; este PR **não instala** nem distribui o pacote, e o uso no aparelho não foi medido. Modelos mais pesados podem ser lentos.
- Qualidade de voz varia por speaker. O pacote `Piper Jeff` é uma opção inicial leve, e **não garante naturalidade igual a ElevenLabs**.
- Não há assinatura mensal própria da voz; o serviço de IA/FLUX Core pode ter outros custos atuais, que não são eliminados por esta mudança.
- A versão atual de modo local usa **turnos**: primeiro reconhece frase, depois obtém resposta textual, depois sintetiza. Não promete interrupção por fala/barrage-in no meio do TTS em tempo real.

## Critérios para avançar à produção
- Android CI compilou a branch e Android instrumentado não quebrou.
- A instalação e o teste de voz reais no telefone do usuário foram feitos (não apenas testes com mocks).
- Idioma `pt-BR`, timbre masculino natural, tempo de primeira fala e taxa de erros aprovados pelo usuário.
- Nenhum TTS Google automático e nenhuma chamada para `/v1/tts` com modo local selecionado.
- Controlar mute, playback, áudio Bluetooth, atualização e assinatura do APK.
- Se a qualidade não agradar, manter atual modo estável enquanto pesquisa alternativa open-source.

## Fontes técnicas
- https://github.com/k2-fsa/sherpa-onnx/tree/master/android/SherpaOnnxTtsEngine
- https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html
- https://github.com/k2-fsa/sherpa-onnx/blob/master/android/SherpaOnnxTtsEngine/app/src/main/java/com/k2fsa/sherpa/onnx/tts/engine/TtsService.kt
