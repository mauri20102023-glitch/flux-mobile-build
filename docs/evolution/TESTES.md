# Evidências de teste

8 de outubro de 2026. Backend local: 21 testes automatizados passaram (`npm test`). Casos: autenticação de novas rotas; consentimento; persistência entre instâncias; correção/exclusão de memória; validade; bloqueio de padrões óbvios de credenciais; missão persistente; executor com falha; cancelamento de resultado tardio; fila limitada; leitura de corpo limitada; envio multimodal; bloqueio de URL remota; limite de plano Inworld; regressões de chat/TTS/realtime/interface.

Mock de provedor comprova contrato e segurança local, não funcionamento real da API. Testes de nuvem, imagem criada/visualizada, compilação Android, interface e dispositivos devem constar aqui quando realizados. Não há aparelho Android, TV ou relógio acessível neste ambiente.


## Testes reais do preview

- Cloudflare: Worker flux-evolution-preview publicado em ambiente e namespace separados. Produção flux-mobile-build2 não foi modificada.
- Vision em 23:25:32 UTC: imagem de teste com texto "Código: 4827", conta "7 + 5 = ?" e círculo vermelho. Cloudflare Llama 4 Scout retornou HTTP 200, número 4827, resultado 12 e círculo vermelho. A interpretação textual não foi perfeita (chamou "código" de número), mas os três dados visuais foram identificados. Imagem não armazenada no Core.
- Memória: POST 201, GET 200; chat novo HTTP 200 recuperou CIANO4827 sem o identificador na pergunta. Em 23:36 UTC, PATCH 200 alterou para CIANO4828, chat novo 200 recuperou CIANO4828, DELETE 200 apagou e GET 200 confirmou lista vazia. Nenhuma memória sintética restou no workspace.
- Missão 812160b9-8658-4fc4-9298-3ce023fae265: criada 23:25:49 UTC, running 23:25:50, completed 23:25:59. Resultado real sobre fotossíntese + duas perguntas persistido. Log de alarme confirmou executor Cloudflare. Estado completed significa texto gerado, não validação científica por uma ferramenta externa.
- Primeira missão Inworld terminou failed após 402; nunca recebeu status completed. Inworld chat, Vision e TTS retornaram HTTP 402. Erro agora mantém HTTP 402 e explica ação de créditos, evitando retentativas automáticas como se fosse falha de rede.
- Gerador: primeiro teste /v1/images falhou 500; API direta confirmou disponibilidade. Quatro passos no preview: /v1/images HTTP 200 às 23:30:03 UTC, JPEG 1024 × 1024 salvo e aberto visualmente. Sem alegação de edição de imagem.
- Interface: navegador real abriu FLUX Lab e exibiu novos formulários, captura única, memória/missões e Mission Control. Teste visual público, sem login automático; não equivale a teste ponta a ponta de todos os formulários.
- Android: GitHub Actions runs 37858905787, 37859768291 e 37860218313 compilaram ambos APKs e verificaram metadados do assistente. APK preview usa ai.flux.mobile.preview; a instalação ai.flux.mobile permanece disponível. Nenhum teste acústico/físico foi realizado.
- TypeScript: tsc --noEmit passou. Bundle: Wrangler dry-run passou. CI web executou testes e validação de scripts/bundle.

## Não comprovado

Barge-in real, echo em viva-voz/Bluetooth, wake word com tela bloqueada, captura Android em diferentes apps, permissões do dispositivo, câmera nativa, biometria, cofre, contas OAuth, envio WhatsApp, controle TV/relógio, handoff e execução de código. São pendências, não resultados positivos.

SHA-256 do JPEG de teste: 8ebc7e4cc95729be5bcfc9eb599d95781e69c1268747d0ba142cd999b07f9d21

Versão final de código compilada: f444de9d9e8bc94f4e807686b7d1bd82f6d79753; run 37860218313 com web, Android e ferramentas de assinatura aprovados. APKs separados preservam ai.flux.mobile.
