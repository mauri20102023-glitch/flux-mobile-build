# FLUX + ChatGPT Plus/Pro — teste de viabilidade (15/10/2026)

Este experimento **não altera o FLUX publicado**. O objetivo inicial é demonstrar, em um computador local, que o proprietário pode autorizar uma aplicação de código aberto a utilizar a franquia do ChatGPT em uma chamada elegível de texto à Responses API.

## Achados confirmados

- O FLUX existente é um aplicativo Android nativo e uma PWA, ambos ligados a um **Core remoto em Cloudflare Workers** (arquivo `src/index.ts`). O Core já oferece `/v1/chat`, roteamento FAST/STANDARD/DEEP, memória curta em Durable Object, Inworld, Gemini, OpenAI API, Workers AI e outras funções.
- **Sign in with ChatGPT — plan usage**: fluxo oficial para ferramentas open-source/locais, com registro dinâmico `dynamic_agent_client`, OAuth Authorization Code + PKCE, `ext_agent_host_id` estável, validação do ID token, consentimento `chatgpt.tokens.use.direct` e credenciais locais.
- **Aplicativos pagos ou hospedados remotamente**: a documentação direciona ao formulário de interesse; **não assumir** que o Worker atual pode receber/usar um token do plano sem a autorização adequada.
- **Limites da preview**: `POST /v1/responses` com `store:false`, `stream:true`, contexto em `input`, sem `max_output_tokens`; não há ferramenta hospedada de geração de imagem, áudio/vídeo de entrada nem ferramentas hospedadas file_search, interpreter, MCP ou computer-use nesse fluxo.
- **Cobrança**: uso elegível consome a franquia e os limites do Plus/Pro. API paga, serviços de voz, imagens, hospedagem e outros provedores **não são incluídos automaticamente**. Não contratar Pro para o teste.
- **Memória**: o plano compartilhado não traz histórico/memórias do ChatGPT para o FLUX. A memória deve ser implementada no FLUX com consentimento.

Fontes oficiais:
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions

## Experimento local (Node.js 20+)

**Atenção:** executar em um computador local sob seu controle; **não implantar este script em Cloudflare, Vercel, Worker, página web nem usar navegador público**.

Da raiz do repositório:

```sh
node --check experiments/siwc-local/flux-siwc.mjs
node experiments/siwc-local/flux-siwc.mjs login
node experiments/siwc-local/flux-siwc.mjs models
node experiments/siwc-local/flux-siwc.mjs ask "Olá, FLUX. Responda em uma frase."
```

Ao iniciar `login`, copie o endereço apresentado **para o navegador do mesmo computador**, entre na sua própria conta e revise a autorização exibida pela OpenAI. Nada é compartilhado até que a pessoa aceite. O script usa callback em `127.0.0.1`, PKCE, `state` e `nonce` novos, valida assinatura e claims do ID token, compara a permissão concedida e armazena os tokens no computador em `~/.flux/siwc-local.json`, com permissões restritas (em sistemas que as respeitam). **Nunca envie esse arquivo ao chat nem o adicione ao GitHub.**

O comando `models` exibe os modelos efetivamente listados para a conta. O comando `ask` usa o primeiro modelo listado, ou um `slug` escolhido com `FLUX_SIWC_MODEL`:

```sh
FLUX_SIWC_MODEL=<slug-retornado-em-models> node experiments/siwc-local/flux-siwc.mjs ask "Teste de resposta"
```

Para desconectar, revogue a conexão nas configurações de Segurança e login do ChatGPT e apague o arquivo local de credenciais. **Apagar somente o arquivo não revoga uma autorização ativa**.

### O que precisa passar no teste real

1. Login oficial com conta Plus e permissão `chatgpt.tokens.use.direct`.
2. Endpoint de modelos retorna pelo menos um modelo elegível.
3. Requisição `/v1/responses` gera deltas e termina com `response.completed`.
4. Duas perguntas simples concluem; a falha de franquia recebe mensagem clara (sem fallback que gere cobrança).
5. Não aparecem tokens em repositório, logs, analytics, console ou navegador; os dados ficam apenas no dispositivo.
6. Revogação da conexão interrompe as chamadas.

**Estado atual:** código de prova de conceito preparado, **sem autenticação executada com a conta do usuário e sem prova de inferência real**. Teste de sintaxe por CI não equivale a teste de conta.

## Arquitetura proposta após aprovação do protótipo

- **Fase 1:** teste local em computador. Sem alterações do FLUX em produção.
- **Fase 2:** módulo de provedor OpenAI Subscription no Android ou host local autorizado, de forma compatível com o fluxo de login e com armazenamento seguro de tokens. Validar política/escopo antes da implementação. O Worker da Cloudflare continua atendendo voz, imagens e demais funções, **sem receber refresh token OAuth do ChatGPT**.
- **Fase 3:** roteador por modo FAST/STANDARD/DEEP e memória contextual do FLUX, com histórico enviado em `input` por conta de `store:false`, limite de mensagens e comandos locais seguros.
- **Fase 4:** FLUX Build com Codex/app-server, ramo Git isolado, testes automatizados, diff e aceite humano antes de qualquer deploy.
- **Fase 5:** avaliar custos e alternativas de voz e imagens separadamente; observar limites do plano, sem compra/upgrade prematuro.

## Restrições e segurança

- Não tentar endpoints internos `backend-api` do ChatGPT, scraping de cookies/sessões, reaproveitamento de senha ou tokens extraídos do aplicativo.
- Não usar a integração open-source para mascarar um app remoto/pago se a modalidade não for elegível.
- Não dar permissão de autoatualização do FLUX sem diff, teste e aprovação.
- Não fazer merge/deploy da branch sem avaliação do teste local.
