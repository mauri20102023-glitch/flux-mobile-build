# FLUX VOICE 01 — JARVIS Elegante: ferramenta experimental de voz própria

**Objetivo:** gerar frases em português brasileiro com checkpoint aberto **ResembleAI/Chatterbox-Multilingual-pt-br**, preservando uma gravação masculina com autorização de uso. Sem API do Google, sem ElevenLabs e sem mensalidade obrigatória de TTS.

**Status:** ferramenta de estúdio local. **Ainda não há voz exclusiva treinada, síntese validada em GPU nem execução autônoma no Android.** Este experimento não modifica o aplicativo FLUX publicado. Não clonar Algieba, ator ou locutor sem autorização.

- Modelo: https://huggingface.co/ResembleAI/Chatterbox-Multilingual-pt-br (licença MIT)
- Pacote oficial: https://github.com/resemble-ai/chatterbox
- O checkpoint pt-BR tem arquivos grandes (4+ GB); não promete execução rápida no telefone.

## Como começar

1. Grave 10 a 25 segundos de um homem brasileiro falando naturalmente. O locutor deve ser você ou ter dado permissão **expressa para clonagem por IA e uso no FLUX**. Salve WAV PCM 16 bits mono, 16–48 kHz, com 6 a 35 segundos.
2. Em Python 3.11 e em ambiente com memória/GPU apropriadas, instale `pip install chatterbox-tts`.
3. Registre sua gravação autorizada:

```bash
python tools/flux-voice-01/studio.py enroll --reference minha-voz.wav --rights own
```

Para voz de outra pessoa: `--rights licensed --license-document autorizacao.txt`.

4. Gere uma fala (não usa API paga):

```bash
python tools/flux-voice-01/studio.py speak --text "Bom dia. Sou o Flux." --output teste.wav
```

O programa guarda referência e metadados somente em `~/.flux/voice-01/`. **Jamais envie áudio pessoal nem documento de consentimento ao repositório.**

## Viabilidade e limites

- MIT permite uso dos modelos sob suas condições. Uma amostra de fala usada como referência **não se torna automaticamente uma voz exclusiva adquirida**; os direitos vêm do consentimento de seu locutor.
- O carregador substitui explicitamente os pesos **T3 pt-BR e S3Gen V3** sobre o modelo multilíngue V3. Uma incompatibilidade falha sem usar o sotaque europeu como substituto.
- O comando de fala é um protótipo de código. **Não foi demonstrada inferência completa com pesos de 4+ GB**. Precisa de teste real de ambiente e compatibilidade entre versões.
- O computador desligado é compatível com o objetivo final, mas **este protótipo ainda precisa de uma máquina com recursos para síntese**. Portar ao Android e medir latência/bateria são outra etapa.
- Sem novas compras. Teto de R$ 50 somente após verificar qualidade. Instalação local elimina tarifa por chamada, não o custo de hardware/energia.

## Próximas etapas para Work

1. Testar os checkpoints reais, comparar em dez frases e validar sotaque brasileiro.
2. Recolher um áudio de referência original e autorizado; nunca usar a Algieba de demonstração nem dublagem de ator.
3. Medir latência e RAM. Estudar otimização e quantização no Android; nenhuma alegação de execução no celular antes de teste.
4. Só integrar ao FLUX Android após validação e aprovação. Não misturar com o modo TTS atual nem iniciar cobrança.
