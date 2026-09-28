# FLUX 1.7

Assistente pessoal de Maurício em duas experiências sincronizadas:

- **FLUX Web/PWA**: central responsiva instalável no celular e no computador.
- **FLUX Mobile**: aplicativo Android nativo com assistente do sistema, ativação por voz e painel FLUX Vision.

O FLUX Core roda em Cloudflare Workers. Quando o Core está pareado, texto e voz usam Gemini com a chave permanente no servidor; para voz, o Core cria uma credencial efêmera. O Android também permite que Maurício configure sua própria chave Gemini no aparelho, protegida pelo Android Keystore, para voz e chat sem pareamento com o Core. Nenhuma chave de IA é incluída no site ou no APK.

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

O repositório não armazena chaves de API, tokens ou dados pessoais. Quando a credencial de pareamento está configurada no build, o Android faz pareamento por assinatura RSA e guarda o token do aparelho no Android Keystore. Sem ela, os recursos do Core ficam indisponíveis; a chave Gemini pessoal permite chat e voz diretamente. Ações externas, como enviar mensagens ou controlar dispositivos, sempre exigem confirmação no aplicativo oficial correspondente.

Configure os segredos no Cloudflare, nunca no `wrangler.jsonc`:

```sh
npx wrangler secret put GEMINI_API_KEY
npx wrangler secret put FLUX_AUTH_TOKEN
```

## Publicar o Core

O APK 1.7.3 aponta para `flux-mobile-build2.mauri20102023.workers.dev`, enquanto
o Worker deste repositório se chama `flux-core-12`. Compilar o APK não publica
o Worker. Confirme a URL real da implantação antes de gerar outro APK.

Para publicar pelo GitHub Actions, configure os segredos do repositório
`CLOUDFLARE_API_TOKEN` (token restrito à edição dos Workers desta conta) e
`CLOUDFLARE_ACCOUNT_ID`. Após integrar o PR, o fluxo **Deploy FLUX Core**
executa automaticamente; ele também pode ser iniciado manualmente.
Ele testa o bundle, publica o Worker e valida a resposta de `/health` na URL
retornada pela Cloudflare. O resultado indica separadamente se chat e Gemini
Live estão configurados. As chaves de IA continuam como segredos do Worker.

O APK atual não contém credencial de pareamento. Mesmo com o Core publicado,
ele só usa chat e voz diretamente com a chave Gemini pessoal até existir um
fluxo seguro de autorização do aparelho. Não inclua a chave privada de
pareamento no APK.

## Desenvolvimento

```sh
npm ci
npm run check
npm run deploy
```

O conteúdo de `web/flux-ath` é publicado como Static Assets pelo mesmo Worker. O Android está em `apps/android` e usa Java 17.

## Limites de plataforma

No navegador, microfone e compartilhamento de tela só podem começar após um gesto do usuário. A ativação contínua por “Flux” e o painel sobre outros aplicativos são recursos exclusivos do Android. Aplicativos protegidos podem impedir a leitura da tela.
