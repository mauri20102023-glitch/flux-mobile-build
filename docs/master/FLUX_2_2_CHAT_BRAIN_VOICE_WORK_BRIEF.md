# FLUX 2.2 — CHAT + BRAIN + VOICE
## Projeto mestre e ordem de execução para ChatGPT Work
**Data:** 16/10/2026 | **Status:** especificação para implementação em branch isolada | **Prioridade máxima:** biblioteca de conversas persistentes.

> **Missão executiva para o Work:** não entregar apenas desenhos, prompts ou um protótipo inerte. Inspecionar o código atual, implementar incrementalmente, testar Android/Web/Core, preservar funções anteriores e entregar PRs revisáveis. Não implantar produção, comprar serviço ou consumir API paga sem permissão explícita do proprietário.

### 0. Contexto técnico confirmado e fontes
- Repositório principal: https://github.com/mauri20102023-glitch/flux-mobile-build
- FLUX Core em Cloudflare Workers com Durable Objects, SPA/PWA em `web/flux-ath`, Android nativo Kotlin/Compose em `apps/android`.
- `main` ainda tem chat único; Web guarda `state.messages` no `localStorage`, Android possui cache local; `/v1/chat` guarda aproximadamente 24 mensagens recentes por `conversationId` no Durable Object. **Isso NÃO é uma biblioteca persistente de conversas, nem garante sincronização completa.**
- PR #18: https://github.com/mauri20102023-glitch/flux-mobile-build/pull/18 — FLUX 2.0.1, branch `codex/flux-project-a-20261013`.
- PR #19: https://github.com/mauri20102023-glitch/flux-mobile-build/pull/19 — FLUX 2.1, branch `codex/flux-master-20261015`, empilhada sobre #18. Verificar estado, CI e diferenças antes de qualquer alteração.
- PR #20: https://github.com/mauri20102023-glitch/flux-mobile-build/pull/20 — PoC local Sign in with ChatGPT, branch `experiments/chatgpt-plus-siwc-poc`, **não implantado nem autenticado em conta real**.
- Este documento pode estar em branch de planejamento baseada no #19; evitar mesclar commits duplicados da PoC. Inspecionar PRs e origem antes de escolher base.

Documentação oficial a validar novamente ao implementar:
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://github.com/QwenLM/Qwen3-TTS
- https://github.com/resemble-ai/chatterbox
- https://github.com/black-forest-labs/flux

### 1. Objetivo de produto
Transformar o FLUX em assistente pessoal Android + desktop/PWA com personalidade JARVIS original, visual premium preto/grafite/branco metálico, chat semelhante em USABILIDADE ao ChatGPT mas **sem copiar logotipos ou material protegido**, biblioteca de conversas própria e preservada, memória controlada pelo usuário, resposta fluida, fluxo de voz, integrações reais e caminho para FLUX Build. Experiência única entre dispositivos. Preferência de custo incremental inicial zero, sem afirmar zero absoluto.

**Regras do usuário:** preservar IA/interface que já funcionam; evoluir modularmente; não refazer do zero; não comprar equipamentos; nenhuma cobrança adicional inesperada; usuário aprova merges, publicações, envio de mensagens, controles de dispositivo ou alterações sensíveis.

### 2. PRIORIDADE 1 — FLUX CHAT 2.2 com histórico permanente
Implementar PRIMEIRO, independente de autenticação da OpenAI, para que funcione com provider já disponível.

**UX Android, PWA e desktop:**
1. Entrada "Chat" fixa na navegação; mobile com drawer lateral para conversas e botão "Novo chat"; desktop com painel lateral recolhível; interface responsiva, grafite/metal e acessível.
2. Listar conversas ordenadas por atividade; agrupar Hoje/Ontem/Últimos 7 dias/Anteriores com fuso correto. Cada conversa exibe título, preview e data atualizados.
3. Criar conversa, recuperar ao fechar e abrir app, continuar conversa antiga sem perder contexto, alternar sem misturar respostas; gerar título inicial a partir do primeiro pedido sem custo de IA; oferecer título automático opcional e edição manual. Pesquisar títulos e textos, renomear, fixar, arquivar/desarquivar, excluir com confirmação. Oferecer exportar em Markdown/JSON e importar de backup; distinção entre arquivo e backup seguro.
4. Chat com visual limpo, bolhas discretas, markdown seguro, blocos de código com copiar, listas, links externos com confirmação, indicador de geração, botão parar, tentar novamente, copiar/resposta, rolagem automática respeitando leitura e tela inicial vazia elegante. Suporte a textos extensos e falhas offline.
5. Mensagens de voz devem aparecer no MESMO chat; transcrição, texto e/ou estado de áudio conforme integração; mute continua funcionando. Não exigir o TTS novo para concluir esta fase.
6. Sincronizar histórico entre Android e PWA/Desktop mediante conta/dispositivo autorizado. Mostrar estado de sincronização e conflitos. Não marcar sincronizado se só estiver em cache local. Primeiro carregar a lista, depois paginar mensagens. Nunca apagar chat por limite de contexto do LLM.

**Modelo de dados mínimo:** `Conversation {id, ownerId, title, createdAt, updatedAt, lastMessageAt, archivedAt?, pinnedAt?, deletedAt?, version}`; `Message {id, conversationId, role, parts, status, createdAt, updatedAt, requestId, provider?, error?}`; armazenar `role`/conteúdo/status com validação; partes estruturadas permitem imagens anexas no futuro sem redesenhar banco. Chaves UUID, índice por owner e data, controle de versão para conflitos. Não misturar anexos base64 gigantes em JSON/chaves locais.

**API sugerida** (ajustar ao roteamento atual e compatibilidade):
- `POST /v1/conversations`
- `GET /v1/conversations?cursor=&q=&status=`
- `GET /v1/conversations/:id`
- `PATCH /v1/conversations/:id` (título, fixação, arquivo)
- `DELETE /v1/conversations/:id` (remoção confirmada, privacidade e política de retenção clara)
- `GET /v1/conversations/:id/messages?cursor=&limit=`
- `POST /v1/conversations/:id/messages` (idempotency/requestId)
- `POST /v1/conversations/:id/messages/stream` ou rota SSE equivalente
- `GET /v1/conversations/export` escopado ao proprietário, ou exportação local segura.
Preservar `/v1/chat` legado ou fornecer compatibilidade para apps antigos. Todas as rotas usam autenticação e isolamento por proprietário, sem IDOR.

**Persistência:** usar recursos já existentes do Durable Object/SQLite (avaliar volume, limites e migração). Separar armazenamento histórico integral do **contexto limitado** enviado ao modelo. Paginação por cursor, pesquisa em escala viável, limitação de tamanho, deduplicação de tentativas, concorrência duas janelas, sem sobrescrever mensagens devido a ID novo. Guardar a mensagem do usuário antes da geração e atualizar a resposta por estados `pending/streaming/completed/failed`. Não inventar respostas perdidas se a rede cair.

**Migração:** inspecionar `state.messages` PWA, Android `LocalConversationCache.kt` e DO `conversation:` existente. Migrar sem apagar dados, com IDs estáveis e backup; dados locais antigos requerem importação opt-in quando não puderem ser correlacionados com segurança. Não prometer recuperação de mensagens já descartadas ou que não existam.

**Aceite P1:** cinco chats independentes ficam salvos após fechar/reabrir; renomear/fixar/pesquisar/arquivar/deletar funcionam; mensagens de chats diferentes não misturam; Android e desktop mostram o mesmo histórico autorizado; offline/sync não duplica; chat com mais de 24 mensagens preserva todo o histórico armazenado sem enviar tudo ao modelo; testes da migração; acessibilidade básica.

### 3. PRIORIDADE 2 — FLUX BRAIN com ChatGPT Plus, sem despesa adicional por padrão
Executar após P1 ou em branch experimental isolada, podendo desenvolver adapters paralelamente. O cérebro é um modelo OpenAI pela integração oficialmente permitida, NÃO uma conexão direta a esta conversa do ChatGPT. Não sincronizar histórico/memórias da conta ChatGPT. O histórico do FLUX é próprio.

- A PoC #20 usa login por fluxo OAuth oficial com PKCE, OIDC e sessão local; conferir rigorosamente com documentação; **testar com usuário antes de aprovar produção**.
- Modalidade `Sign in with ChatGPT` é voltada a aplicativos locais e open-source elegíveis. O Core do FLUX está hospedado na Cloudflare: **não repassar tokens Plus a Worker remoto nem assumir elegibilidade de app hospedado sem aprovação específica da OpenAI**.
- Estudar adaptador executado localmente no dispositivo/desktop autorizado, com token guardado em Keystore/armazenamento protegido e consentimento visível; nunca incluir segredos no APK, GitHub, frontend, logs, cookies ou analytics. Se Android direto não for elegível/suportado, documentar bloqueio e manter provedor atual com aviso claro de eventual cobrança. Não usar scraping, `backend-api` privada, senhas nem sessão do app ChatGPT.
- `GET /v1/models` da conta, modelo disponível selecionado, `POST /v1/responses` compatível com preview (`store:false`, `stream:true`, enviar contexto em `input`), respeitando campos vedados. API plan-sharing não disponibiliza automaticamente imagem gerada, transcrição ou áudio.
- Criar `AIProvider` interface e `FLUX Brain Router`: `FAST` para comando simples, `STANDARD` para chat, `DEEP` para análise, sem obrigar modelo mais caro. O mesmo sistema de conversas persiste com qualquer provider. Mostrar provider real selecionado e erro claro de limite do plano. **Não fazer fallback silencioso para API paga**.
- Resposta por streaming com primeira atualização de UI imediata, cancelamento, retry, mensagens parciais marcadas corretamente e histórico consistente. Medir TTFB e latência percebida em rede real; reportar medianas/p95 em testes, nunca prometer segundos garantidos.
- Pesquisa atual, GitHub, e-mail e dispositivos exigem ferramentas reais e consentimentos; não afirmar integração porque existe atalho.

**Aceite P2:** login oficial autorizado, catálogo da conta consultado, dois chats respondem em streaming, limite/revogação seguros, zero uso silencioso de API paga, histórico de cada chat permanece íntegro; status rotulado `experimental` enquanto estiver só no protótipo.

### 4. PRIORIDADE 3 — FLUX VOICE 01 original
Voz masculina em **português brasileiro**, grave moderada, firme, elegante, calor humano discreto, dicção nítida, pausas naturais, sem parecer locutor, Google TTS ou imitação exata de ator/JARVIS.

- Avaliar Qwen3-TTS VoiceDesign para criar amostra original e fluxo VoiceDesign→Clone para consistência; comparar com Chatterbox Multilingual `pt-br` em frases curtas, longas e nomes técnicos. Conferir licença/modelo e recursos locais.
- Se o hardware do usuário não suportar geração rápida, preservar a voz atual como fallback **explicitamente identificado** ou modo texto; estudar motor leve sem assinatura. Não comprar GPU, serviço de hospedagem ou ElevenLabs sem aprovação.
- Dividir STT, texto LLM e TTS em módulos independentes. Medir tempo até o primeiro áudio e latência; manter barge-in, mute, wake word, tratamento de Android background e economia de bateria; validar em aparelho real.
- Salvar amostra/descrição/versão de identidade `FLUX_VOICE_01`; usar apenas amostras originais ou de pessoas com consentimento.

**Aceite P3:** cinco frases brasileiras avaliadas em naturalidade, pronúncia e consistência; streaming/cancelamento/mute preservados; documentação de recursos do hardware e custo adicional efetivo.

### 5. PRIORIDADE 4 — IMAGEM sem novo custo
Separar capacidades:
A. **Handoff assistido ChatGPT:** ao pedir imagem, FLUX gera prompt detalhado, apresenta botão "Abrir ChatGPT" e permite copiar/compartilhar; usuário envia manualmente no ChatGPT. Implementar no Android recebimento de imagem via Android Share Intent (`ACTION_SEND image/*`) e upload/import local autorizado ao chat. Não dizer que ChatGPT gerou automaticamente ou que o FLUX recebeu sem compartilhamento.
B. Manter FLUX Studio existente e testar alternativas livres/locais se houver recursos disponíveis. A geração automática OpenAI GPT Image **não está incluída** na modalidade de uso do plano Plus; não usar API de imagens paga sem consentimento.
**Aceite P4:** prompt copiado e app oficial aberto, imagem compartilhada/importada entra na conversa correta, falha/sem app tratado, nenhuma chamada paga clandestina.

### 6. PRIORIDADE 5 — FLUX Build e integrações reais
Preservar as implementações do PR #19: capabilities/consentimento, Advanced, Build protegido. Pedidos de desenvolvimento criam planos/prompt, branch e PR draft por ferramentas GitHub/Codex quando permitidos. CI, revisão de diff, autorização humana, rollback. Nunca autoatualizar APK ou produção sem confirmação. Vincular trabalhos do Build ao chat que os originou, mostrando status real.

### 7. Segurança, custo e controle
- Segurança de sessão e por-dispositivo, rate limits, proteção contra abuso, isolamento proprietário, exportação/remoção de dados, armazenamento cifrado quando apropriado, proteção contra XSS/CSRF e logs sem payload pessoal; não quebrar PIN/notas privadas, Keystore, emparelhamento nem Android permissions.
- Políticas de retenção e exclusão claras; excluir chat não deve inventar exclusão no provedor externo; `store:false` nas chamadas permitidas.
- Iniciar com serviços gratuitos e infraestrutura atual, respeitando franquias e possíveis cobranças existentes da Cloudflare e provedores. **Zero custo incremental é meta, não promessa absoluta**. Qualquer novo serviço pago exige aprovação.
- Não usar tokens ChatGPT capturados do navegador, endpoints internos, scraping ou mecanismos não documentados.

### 8. Padrão visual
Mobile: home minimalista com esfera central, aba Chat na navegação, chat full-screen premium, drawer que lista conversas e barra de envio fixa inferior. Desktop: navegação lateral e biblioteca de chats com sidebar redimensionável/recolhível; cards dinâmicos ficam fora do conteúdo de mensagens salvo quando contexto realmente exigir. Tema preto/grafite/branco metálico; nada de vermelho/roxo decorativo. Reutilizar componentes do FLUX 2.1. Foco visível, tamanhos de toque adequados, suporte leitor de tela, teclado, rolagem e animação reduzida.

### 9. Entregáveis obrigatórios do Work
1. Auditar `main`, PRs #18/#19/#20 e recursos publicados; eleger branch-base mais completa e estável; relatar estado real antes de mudanças invasivas.
2. Implementar P1 **de verdade** em backend + Android + PWA. Não apenas mockup ou API sem interface. Novos recursos atrás de feature flags se necessário, mantendo funcionalidades anteriores.
3. Testar `npm ci && npm test && npm run check`, testes unitários, integração Core, navegador/Chromium, builds/testes Android pela CI disponível. Fazer testes de chat CRUD, 25+ mensagens, sync, segurança IDOR, reconexão e migração. Informar o que não foi testado em aparelho real.
4. Criar PR draft(s) independentes com código, diff, screenshots dos modos mobile/desktop, checks e relatório de custo. Não dar merge/deploy em produção sem aprovação explícita.
5. Evoluir PoC #20 apenas pelo fluxo oficial, sem tokens no Worker remoto. Se barrado, entregar interface de seleção de provider e documentação honesta, mantendo chat existente funcional.
6. Preparar proposta/executável de avaliação de voz e handoff de imagens; não chamar de pronto sem áudio/imagem e testes reais.
7. Atualizar documentação técnica, checklist do proprietário, limitações e próximos passos.

### 10. Critério final de pronto
A frase "o FLUX agora tem chat igual ao ChatGPT" SÓ poderá ser usada quando: nova conversa, histórico navegável/persistido, busca, renomeação, exclusão, sincronização entre dispositivos reais, resposta de modelo real, streaming, e retenção para além da janela contextual estiverem demonstrados. Voz própria e login Plus devem ser reportados separadamente como disponíveis/testados/em desenvolvimento, nunca misturados a telas demonstrativas.

**COMECE pela PRIORIDADE 1 agora; trabalhe com autonomia de implementação, não de custos/deploy, e finalize com links de PR, testes, screenshots, pendências e instruções de instalação para avaliação.**


### 4A. DECISÃO POSTERIOR DO PROPRIETÁRIO (16/10/2026): voz mesmo com computador desligado, teto de R$ 50 em gasto único
Este requisito **complementa e prevalece** sobre qualquer hipótese de motor de voz hospedado no notebook/PC. A voz do FLUX deve estar disponível no Android independente de computador ligado. O proprietário aceita no máximo R$ 50 em eventual gasto único, mas **não quer assinatura recorrente de TTS**.

**Executar antes de construir serviço TTS novo:**
1. Implementar PoC Android com motor ONNX local, sem servidor de voz: avaliar **Kokoro-82M multilingue** (voz masculina portuguesa `pm_alex`) via `sherpa-onnx`, com modelo/download sob demanda, cancelamento, mute e segurança de arquivos. Referência pública de bundle pt-BR: https://huggingface.co/cristianoaredes/kokoro-pt-br (~330 MB comprimido, modelo fp32 ~349 MB). Verificar integridade, licença e suporte Android antes de incorporar; não embutir no APK sem avaliar peso.
2. Comparar desempenho e qualidade com **Piper pt_BR Jeff medium** e alternativas pt_BR em `sherpa-onnx`: https://k2-fsa.github.io/sherpa/onnx/tts/all/Portuguese/vits-piper-pt_BR-jeff-medium.html ; medir primeira fala, tempo total, RAM, temperatura/bateria, pronúncia e naturalidade em aparelho do usuário.
3. Testar **no Android real** antes de declarar pronto; Android funcionando não implica qualidade ElevenLabs. Falhas de modelo/ausência de download exigem fallback explícito e possibilidade de usar FLUX em texto.
4. Separar conceitos: **Kokoro/Piper local têm vozes predefinidas**, não podem simplesmente importar uma amostra Qwen3-TTS e manter identidade exata. A identidade realmente inédita exigirá outro modelo capaz de cloning/tuning, testes e hardware apropriado. Não prometer que sample Qwen será voz que roda no Android.
5. Para segunda versão, pesquisar TTS local com clonagem e pt-BR comprovado, por exemplo comparar Qwen3-TTS e Chatterbox **somente se rodarem no celular a velocidade aceitável**. GPU alugada uma vez pode auxiliar a criação, mas não é hospedagem permanente.
6. Não configurar serviço premium da Cloudflare/Deepgram/OpenAI nem fazer fallback automático pago para TTS; Workers AI tem franquias diárias e alguns modelos TTS cobram por caracteres. Vale só como teste posterior mediante consentimento. Reserva de até R$ 50 não precisa ser consumida.
7. Distinguir voz local (pode funcionar sem internet) de inteligência pelo ChatGPT Plus (depende de internet e da autorização do modelo). O telefone deve responder por áudio mesmo com notebook desligado, **mas não afirmar que o cérebro ChatGPT funciona offline**.

**Novos critérios de aceite:** modelo TTS carregado e gera em português brasileiro localmente no celular sem PC; qualidade da voz avaliada pelo usuário em dez frases; sem assinaturas de voz; memória e travamentos medidos; áudio interrompe ao apertar mute; nenhuma alegação de clonagem original sem prova real.
