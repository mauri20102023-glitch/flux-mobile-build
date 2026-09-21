# FLUX Mobile 1.3.2

Cliente Android do ecossistema FLUX.

Este repositório não armazena chaves de API, tokens de acesso ou dados pessoais. O Android faz pareamento automático por assinatura RSA: a chave privada existe somente no APK particular e o servidor publica apenas a chave pública. Depois do pareamento, cada aparelho recebe uma credencial própria protegida pelo Android Keystore.

O FLUX usa OpenAI e ElevenLabs quando as chaves privadas estão configuradas. O binding nativo Workers AI mantém conversa e voz funcionando como fallback automático.
