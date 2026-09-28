# FLUX 1.7

Assistente pessoal de Maurício em duas experiências sincronizadas:

- **FLUX Web/PWA**: central responsiva instalável no celular e no computador.
- **FLUX Mobile**: aplicativo Android nativo com assistente do sistema, ativação por voz e painel FLUX Vision.

O FLUX Core roda em Cloudflare Workers. Texto e voz usam Gemini; a chave permanente fica somente no servidor. Para voz, o Core cria uma credencial efêmera de uso único e o cliente abre a sessão Gemini Live diretamente. Nenhuma chave de IA é incluída no site ou no APK.

## Recursos

- FLUX Chat para conversas livres, com modos rápido, padrão e profundo.
- FLUX Live com áudio bidirecional, transcrição e interrupção natural.
- FLUX Vision para compartilhar a tela sob comando explícito.
- FLUX Studio para geração de imagens pelo Workers AI.
- Planner com tarefas e projetos locais.
- Memory Center com memórias privadas criptografadas no navegador e protegidas por PIN.
- FLUX Link para WhatsApp, Instagram, e-mail, agenda, Canva, Drive, YouTube, Spotify, Mercado Livre e SmartThings.
- FLUX Nexus para celular, Samsung Crystal, relógio, EDITH e futuro Home Hub.
- FLUX Lab com Builder, Test Lab, Security, Update, Recovery e Developer Mode.
- Quatro temas visuais e interface adaptada para celular, TV e desktop.

## Segurança

O repositório não armazena chaves de API, tokens ou dados pessoais. O Android faz pareamento por assinatura RSA e guarda a credencial do aparelho no Android Keystore. Ações externas, como enviar mensagens ou controlar dispositivos, sempre exigem confirmação no aplicativo oficial correspondente.

Configure os segredos no Cloudflare, nunca no `wrangler.jsonc`:

```sh
npx wrangler secret put GEMINI_API_KEY
npx wrangler secret put FLUX_AUTH_TOKEN
```

## Desenvolvimento

```sh
npm ci
npm run check
npm run deploy
```

O conteúdo de `web/flux-ath` é publicado como Static Assets pelo mesmo Worker. O Android está em `apps/android` e usa Java 17.

## Limites de plataforma

No navegador, microfone e compartilhamento de tela só podem começar após um gesto do usuário. A ativação contínua por “Flux” e o painel sobre outros aplicativos são recursos exclusivos do Android. Aplicativos protegidos podem impedir a leitura da tela.
