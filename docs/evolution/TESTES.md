# Evidências de teste — FLUX 2.0

## Versão Android / Core

Commit de implementação: b1e79c91485b3ea119b423ea68236e89f1efc5ee. GitHub Actions run 37874393908. APK primário ai.flux.mobile.preview.pro; versão 2.0.0-preview, nome FLUX. Instala separado de ai.flux.mobile. Preview Cloudflare separado da produção; dados e segredos do app estável preservados.

## Validação executada

- 35 testes Node passaram: autenticação; consentimento; memória/correção/exclusão/expiração; missões persistentes/cancelamento/falha; limite de corpo; multimodal; modelos; quotas; OAuth PKCE/encriptação/uso único; revogação local mesmo com erro remoto; capacidade SmartThings antes do comando; leitura Gmail limitada com exclusão de anexos; pesquisa sem fetch arbitrário. Provedores simulados nesses testes não comprovam login/execução externa.
- TypeScript com opções ES2022, bundler, DOM e allowImportingTsExtensions: sem erros. Wrangler dry-run: bundle válido. CI web aprovado.
- Android release e instrumentation APK compilaram. CI validou metadados do assistente. ZIP do artefato validado por SHA-256 e CRC; assinatura APK v2 verificada com apksigner. Assinatura de avaliação Android Debug; não é certificado de distribuição estável entre runners.
- Emulador Android 15/API35 Pixel6: recursos adaptive icon passaram em três máscaras; FLAG_SECURE inicial do cofre passou; medidor PCM limitado passou. Navegação percorreu início/ajustes/persistência ciano/workspace/memória/conexões/segurança/chat. Capturas reais conferidas visualmente revelaram e permitiram corrigir texto preto sobre fundo escuro. Teste run66 ainda falhou ao localizar o ícone na gaveta do launcher; suíte não está integralmente aprovada. Correção da sincronização/gesto e separação do teste launcher preparadas para repetição.
- Router real: FAST, STANDARD e DEEP responderam HTTP200 com 391 para 17×23 após corrigir provedor que retorna número em response. Isso testa integração/formato, não qualidade de todos os raciocínios.
- Vision real: fixture com retângulo vermelho e 4827 foi identificada, HTTP200. Análise remota individual; não valida captura MediaProjection/câmera no aparelho.
- Creator real: HTTP200, JPEG1024×1024 com 273714 bytes; arquivo aberto visualmente. SHA256: 8c1d353133b8ae34900ea6d875b14f47f8c5382ff56ab1c77d35b447d63727c8. Não é prova de edição de imagens.
- Voz FLUX4 real: HTTP402, falta crédito Inworld. Configuração preservada. Nenhuma compra realizada.
- Memória real, sessão nova: criar201/ler200/recuperarCIANO4827; corrigir200/recuperarCIANO4828; excluir200/lista vazia200. Dados sintéticos removidos.
- Missão real: alarme de estudo queued→running→completed com resposta persistida. Missão Inworld com402 terminou failed, nunca completed. Missões executam geração de texto, não ações externas.
- Catálogo/status de nuvem respondem200, reportam cadastros OAuth ausentes. Pesquisa GET retorna PREPARADO e required BRAVE_API_KEY; nenhum resultado de busca simulado como verdadeiro.

## Ainda não comprovado

Aparelho físico, áudio/barge-in/wake word/tela bloqueada, consumo real de bateria, PDF/foto no seletor Android, consentimento MediaProjection em outros apps, cofre com biometria/chave autenticada, exportação/compartilhamento, contas OAuth, playback Spotify, TV, relógio, Handoff, código Builder executável e automações. Testes de UI desconectada não comprovam o fluxo pareado completo. Fluxos de cadastro/API sem credencial estão preparados, não validados.

SHA256 APK run66: f5412a9c297ce530f543627c39395a632a55ee1b44f55b6a9f7fa7431390f75b. 91038577 bytes. Não entregar como versão definitiva ou integralmente pronta.
