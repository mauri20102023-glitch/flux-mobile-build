# FLUX Mobile 1.4.0

Cliente Android do ecossistema FLUX.

Este repositório não armazena chaves de API, tokens de acesso ou dados pessoais. O Android faz pareamento automático por assinatura RSA: a chave privada existe somente no APK particular e o servidor publica apenas a chave pública. Depois do pareamento, cada aparelho recebe uma credencial própria protegida pelo Android Keystore.

Na conversa por voz, o aplicativo abre uma sessão privada e em tempo real com o agente ElevenLabs **FLUX ATH**. A chave da ElevenLabs permanece no Cloudflare Worker e o APK recebe somente um token temporário de conversa. O app não usa Google Speech, Android TextToSpeech nem outra voz como fallback.

O chat digitado continua passando pelo FLUX Core. A voz, a transcrição e as respostas faladas vêm do mesmo agente FLUX ATH.
