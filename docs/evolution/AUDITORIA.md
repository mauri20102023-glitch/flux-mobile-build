# FLUX Evolution — auditoria de 8 de outubro de 2026

Criador: Maurício Manzolli Palhares. Planejamento e desenvolvimento assistidos por ChatGPT/Codex (OpenAI).

Base preservada: main 815ca9297328fe46d01e993c320ac785632640de. Desenvolvimento em codex/flux-evolution-20261008. A produção 1.8.0 não é substituída por esta entrega; preview tem Worker, armazenamento e applicationId separados. Nenhuma migração de dados da produção.

## Achados comprovados no código

- Voz Android ativa usava SpeechRecognizer -> chat -> MP3 -> reabertura do microfone. Não havia escuta durante a resposta, portanto barge-in não estava implementado nesse caminho.
- VoiceInteractionSession já recebia captura oficial do Android, mas FluxConversationalVoice.sendScreenFrame apenas emitia mensagem de indisponibilidade.
- Site possuía WebRTC Inworld; compartilhamento de tela ainda dependia do caminho legado Gemini.
- Builder guardava uma proposta local. Não executava testes, branches ou alterações de produção.
- Agenda recebia próximos eventos do Android autorizado; não era OAuth Google Calendar completo.
- TV abria SmartThings, sem executor remoto ou confirmação de recepção. Relógio não tinha conector.
- Pareamento aceitou o primeiro resgate e negou repetições. ViewModel não bloqueava clique duplicado. Agora estabelece isConnecting antes de iniciar coroutine e ignora cliques enquanto conecta; código é limpo após sucesso.
- Memórias existentes no site eram notas locais, com PIN nas privadas; não se confundem com o novo armazenamento de memórias explicitamente autorizadas no Core.

## Incremento implementado

1. POST/GET/PATCH/DELETE /v1/memories: consentimento explícito, categorias, limite 200, validade, recuperação contextual limitada e exclusão. Escopo: workspace do proprietário; não é identidade vocal ou segregação multiusuário completa. Todos dispositivos pareados têm acesso. NÃO usar como cofre.
2. POST/GET /v1/missions e cancelamento: fila persistente, alarmes de Durable Object, registro de estados reais, limite de cinco pendentes, falhas explícitas. Executores produzem TEXTO para estudo, negócio, rascunho e propostas de código. Não pesquisam fontes, executam código, publicam ou enviam mensagens.
3. POST /v1/vision: imagem inline JPEG/PNG/WebP enviada à Inworld multimodal; sem URLs arbitrárias, limite de corpo, timeout e nenhuma persistência da imagem pelo Core. Provedor processa imagem conforme sua própria política. Não existe análise contínua de vídeo nesta entrega.
4. Site: memória cloud, correção/exclusão, missões com resultado real e análise de arquivo/captura/câmera de uma imagem por vez no FLUX Lab. Saídas renderizadas como texto para evitar injeção HTML.
5. Android preview: controlador WebRTC Inworld, áudio simultâneo, cancelamento de eco e ruído, VAD com interrupt_response, resposta visual via API e ferramentas para contexto local autorizado de clima/agenda. Controlador antigo preservado no código. Validação acústica requer aparelho real.
6. Leitura JSON com limite, erro sem segredo, resposta OkHttp tardia fechada após cancelamento.

## Limitações e riscos ainda abertos

Sem cofre forte, BiometricPrompt, cadastro vocal com consentimento, OAuth Gmail/Spotify, Health Connect, controle TV, MediaProjection fallback, câmera nativa, edição de imagens, pipeline Builder autônomo, handoff confirmado ou automações de notificações persistentes. Alarmes de missões NÃO são lembretes ao usuário. Memórias são dados, não instruções, mas resistência a prompt injection exige avaliações adicionais. Detecção por regex de credenciais não é garantia de prevenção; proibido inserir credenciais nessa memória.

A integração usa uma conta de proprietário, não identifica automaticamente quem fala. Voz não concede autenticação. Não fazer biometria facial por comparação de fotos. Não publicar alterações ou mandar conteúdo sensível sem revisão.

## Revisão e recuperação

Preview não deve substituir instalação estável. Para parar avaliação, fechar/desinstalar FLUX Evolution; app original e Core original continuam disponíveis. Para rollback do preview, redeploy do commit anterior com os mesmos bindings e secrets; não apagar namespaces. Merge e atualização da produção dependem de revisão explícita do resultado e validação no aparelho.
