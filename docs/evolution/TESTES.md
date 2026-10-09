# Evidências de teste — FLUX 2.0

## Versão Android / Core

Commit do APK entregue: c6e7bf762c198d44ab94f253aefe8b299f9bf790. GitHub Actions run 37875108922 (67). APK primário ai.flux.mobile.preview.pro; versão 2.0.0-preview, nome FLUX. Instala separado de ai.flux.mobile. Preview Cloudflare separado da produção; dados e segredos do app estável preservados.

## Validação executada

- 35 testes Node passaram: autenticação; consentimento; memória/correção/exclusão/expiração; missões persistentes/cancelamento/falha; limite de corpo; multimodal; modelos; quotas; OAuth PKCE/encriptação/uso único; revogação local mesmo com erro remoto; capacidade SmartThings antes do comando; leitura Gmail limitada com exclusão de anexos; pesquisa sem fetch arbitrário. Provedores simulados nesses testes não comprovam login/execução externa.
- TypeScript com opções ES2022, bundler, DOM e allowImportingTsExtensions: sem erros. Wrangler dry-run: bundle válido. CI web aprovado.
- Android release e instrumentation APK compilaram. CI validou metadados do assistente. ZIP do artefato validado por SHA-256 e CRC; assinatura APK v2 verificada com apksigner. Assinatura de avaliação Android Debug; não é certificado de distribuição estável entre runners.
- Emulador Android 15/API35 Pixel6: recursos adaptive icon passaram em três máscaras; FLAG_SECURE inicial do cofre passou; medidor PCM limitado passou. Navegação percorreu início/ajustes/persistência ciano/workspace/memória/conexões/segurança/chat. Capturas reais conferidas visualmente revelaram e permitiram corrigir texto preto sobre fundo escuro. A falha de localização do ícone no run66 foi corrigida por sincronização/gesto apropriados. Run67: cinco testes passaram, zero falhas. Capturas reais mostram o ícone instalado na gaveta e fixado na tela inicial, com símbolo grande e fundo integrado. O ponto rosa sobre o ícone é indicador de notificação do launcher, não parte do símbolo. Testes do cofre validam proteção da janela/estado inicial, não autenticação criptográfica física.
- Router real: FAST, STANDARD e DEEP responderam HTTP200 com 391 para 17×23 após corrigir provedor que retorna número em response. Isso testa integração/formato, não qualidade de todos os raciocínios.
- Vision real: fixture com retângulo vermelho e 4827 foi identificada, HTTP200. Análise remota individual; não valida captura MediaProjection/câmera no aparelho.
- Creator real: HTTP200, JPEG1024×1024 com 273714 bytes; arquivo aberto visualmente. SHA256: 8c1d353133b8ae34900ea6d875b14f47f8c5382ff56ab1c77d35b447d63727c8. Não é prova de edição de imagens.
- Voz FLUX4 real: HTTP402, falta crédito Inworld. Configuração preservada. Nenhuma compra realizada.
- Memória real, sessão nova: criar201/ler200/recuperarCIANO4827; corrigir200/recuperarCIANO4828; excluir200/lista vazia200. Dados sintéticos removidos.
- Missão real: alarme de estudo queued→running→completed com resposta persistida. Missão Inworld com402 terminou failed, nunca completed. Missões executam geração de texto, não ações externas.
- Catálogo/status de nuvem respondem200, reportam cadastros OAuth ausentes. Pesquisa GET retorna PREPARADO e required BRAVE_API_KEY; nenhum resultado de busca simulado como verdadeiro.

## Ainda não comprovado

Aparelho físico, áudio/barge-in/wake word/tela bloqueada, consumo real de bateria, PDF/foto no seletor Android, consentimento MediaProjection em outros apps, cofre com biometria/chave autenticada, exportação/compartilhamento, contas OAuth, playback Spotify, TV, relógio, Handoff, código Builder executável e automações. Testes de UI desconectada não comprovam o fluxo pareado completo. Fluxos de cadastro/API sem credencial estão preparados, não validados.

SHA256 APK run67: 8ba7df26aa32f5099ea818d3f91b5b76b3966c04ba470db363bd23deb3ddf2d9. 91038577 bytes. Não entregar como versão definitiva ou integralmente pronta.

Teste real adicional: Gmail/start e Research POST retornaram503 com configuração ausente explicitada; Usage POST200 persistiu câmbio5,0119/reserva10%. Status novo retornou200 com28 capacidades, incluindo10 conectores. A primeira leitura imediatamente após deploy ainda serviu versão anterior; repetição posterior confirmou as novas rotas.
