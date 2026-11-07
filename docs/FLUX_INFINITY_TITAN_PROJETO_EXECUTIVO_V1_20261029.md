# FLUX INFINITY — ESCOPO FINAL DE PRODUTO V1.1

**Revisado:** 06/11/2026  
**Status:** escopo definido para planejamento; implementação final NÃO concluída.  
**Orçamento correto e vinculante:** **R$ 0 de investimento inicial + no máximo R$ 300 por mês**. Sem taxa de implantação, sem reserva inicial de R$ 1.000, sem mensalidade de R$ 500. Não há autorização para contratação ou cobrança.

> Este documento substitui os valores anteriores do projeto TITAN V1. Qualquer documento antigo que apresente R$ 1.000 iniciais ou R$ 500/mês está desatualizado.

## 1. Visão

FLUX INFINITY: assistente pessoal multimodal em Android (Motorola Edge 50 Neo) e Desktop/Web, com interface premium preto/grafite/branco metálico, esfera central e **cards adaptativos**. A interação deve parecer um sistema único, mesmo quando usar várias IAs especializadas.

Preservar o repositório [flux-mobile-build](https://github.com/mauri20102023-glitch/flux-mobile-build), seus módulos Android/Cloudflare/PWA e as interfaces já aprovadas (FLUX Visual System V1 / Morph V8). Não reescrever código funcional por estética.

## 2. Arquitetura principal

```text
Android / Desktop PWA — chat, voz, anexos e workspaces
      ↓ autenticação e sessão
FLUX Core — Cloudflare Workers
      ├─ Session Manager + Auth + Spending Guard
      ├─ FLUX Intelligence Router / Capability Registry
      │    ├─ ElevenLabs FLUX ATH (agente conversacional / voz)
      │    ├─ LLM multimodal, quando conectado e disponível
      │    ├─ IA de programação, quando conectada e disponível
      │    ├─ geração de imagens, quando conectada e disponível
      │    └─ funções locais determinísticas e módulos 3D
      ├─ File Dock + Jobs + armazenamento privado
      ├─ Memory Graph + histórico + dados de projetos
      ├─ Actions/Verifier (consentimento e estado real)
      └─ Morph Renderer (componentes e layouts JSON validados)
```

**Agente ElevenLabs existente:** FLUX ATH, ID `agent_5701m38je3dweertjnjsk2fegsbk`; voice ID `0UODmc3E7WJdP8dVJWTB`. Última leitura confirmou LLM `gpt-5.4-mini`, TTS `eleven_flash_v2_5`, ASR `scribe_realtime`. **Não possui ferramentas conectadas na configuração lida**; conectar e testar antes de afirmar execução de ações.

A ElevenLabs não substitui automaticamente Claude Code/Codex, OpenAI Image, ferramentas visuais ou infraestrutura para programar e compilar o FLUX. Cada serviço externo exige integração, condições de acesso, custo e autenticação próprios. ChatGPT Plus não fornece créditos de API.

## 3. Funções planejadas

| Componente | Escopo |
|---|---|
| **Home + FLUX Morph** | Esfera, cards dinâmicos, organização adaptativa, fixação de cards, layouts responsivos |
| **Chat + Live** | Conversas persistentes, busca, voz e texto, transcrições, mute/interrupção, controle de sessões e orçamento |
| **FLUX File Dock** | Anexar PDFs, apresentações, fotos, documentos, arquivos e planilhas; validação de formato/tamanho; armazenamento e consentimento |
| **FLUX Study Generator** | Gerar aulas interativas a partir de materiais: imagens, diagramas, capítulos, áudio, questões, flashcards, revisão e acompanhamento; referência: aula de Equinodermos já criada |
| **Memory Graph** | Memória editável e pesquisável por projeto; contexto e fonte dos dados; exportação/exclusão |
| **Genesis 3D / HoloDesk** | Biblioteca de modelos interativos, cenas técnicas ilustrativas, controles de câmera, manipulação e integração com explicações |
| **Planner + Chronos** | Tarefas, prazos, cronologia e lembretes persistentes |
| **Business** | Mauright e Automobili: análise de CSVs, margem, estoque, veículos, relatórios e simulações com dados reais |
| **Vision** | Captura de tela/câmera autorizada, compreensão visual via modelo compatível e apresentação de resultados |
| **Forge** | Miniaplicativos baseados em componentes seguros: calculadora, checklist, simulador, tabela |
| **FLUX Build** | Solicitação de alterações, branches e PRs; compilação/testes e instalação mediante revisão e aprovação |
| **Link + Devices** | Integração gradual com contas e dispositivos por APIs oficiais, sincronização e Handoff |
| **System Lab** | Configurações, segurança, status, permissões, recuperação, backup e métricas de custo |

**Não prometer:** geração ilimitada de imagens/vídeos/3D, qualquer modelo premium sem cobrança, acesso irrestrito a outras contas, operação de hardware não conectado, ou clonagem premium em tempo real no celular sem teste.

## 4. FLUX Intelligence Router — seleção da ferramenta

O FLUX escolhe por tarefa, custo, disponibilidade e qualidade observada:
- conversa ao vivo → ElevenLabs, com orçamento controlado;
- PDFs/aulas → parsing/OCR e modelo multimodal habilitado;
- imagem → modelo de imagem conectado; armazenar e reutilizar resultados;
- programação → agente de código com GitHub, testes e aprovação;
- 3D → modelos de biblioteca e motor gráfico; geração pesada externa somente quando aprovada;
- operações precisas → funções determinísticas e calculadoras locais.

O usuário controla as preferências e pode revisar qual serviço foi utilizado, a operação realizada e o custo estimado.

Nunca executar código gerado pelo modelo diretamente no navegador/servidor sem validação e sandbox.

## 5. Regra financeira — zero de entrada, limite de R$ 300/mês

**R$ 0 inicialmente:** reutilizar código, modelos e bibliotecas livres, GitHub e franquias gratuitas já disponíveis. Não comprar domínio, hardware, assets, créditos de API ou licenças para começar.

**R$ 300 mensais:** teto global para os serviços, não uma obrigação de gasto. Proposta inicial de envelopes flexíveis, sujeita à conferência de preços reais:

| Recurso | Reserva mensal máxima planejada |
|---|---:|
| ElevenLabs (voz e agente) | R$ 130 |
| Modelos de texto / documentos / análise | R$ 70 |
| Imagens, código e tarefas especializadas | R$ 50 |
| Infraestrutura / armazenamento | R$ 20 |
| Reserva para variação cambial e tributos | R$ 30 |
| **Total** | **R$ 300** |

Os valores acima **não são assinaturas contratadas nem preços confirmados**. Se o plano mínimo de um serviço não couber em sua reserva, usar alternativa mais barata, modo local ou deixar o recurso indisponível. O limite em BRL deve cobrir impostos e variação cambial; nenhuma recarga automática ou pagamento por uso sem trava rígida. Cobranças de terceiros não devem ser assumidas como automaticamente bloqueadas pelo backend: configurar também limites nas plataformas.

Não contratar ElevenLabs Pro (ou qualquer plano) sem verificar o preço final; o Pro pode ultrapassar todo o teto sozinho.

## 6. Etapas de implementação e aceite

**Fase A — Base visual e local:** auditar branches/PRs; preservar home aprovada, Morph V8, chat local, Planner, backup e mobile/desktop. Aceite: telas reais e dados persistentes; nada de demonstração rotulada como produção.

**Fase B — Arquivos e Study:** File Dock seguro; produzir a partir de PDF uma aula no formato de Equinodermos, com imagens, questões e narração opcional; revisão por desempenho. Aceite: material novo criado automaticamente e salvo com identificação das fontes e progresso.

**Fase C — Inteligência e custos:** conectar FLUX ATH e pelo menos um provedor alternativo por integração legítima; sessão, voz, logs sanitizados, orçamento e erros reais. Aceite: sessões reais e custo medido sem exceder teto.

**Fase D — Morph dinâmico / Genesis / Forge:** usar schemas e componentes permitidos para transformar cards e experiências; cenas 3D conhecidas com persistência. Aceite: cenários demonstráveis sem programação fictícia.

**Fase E — Business, Vision, Link e Build:** desenvolver progressivamente, medindo custos e segurança. Aceite: ferramenta realiza ação real ou indica claramente indisponibilidade; nenhum deploy sem revisão.

## 7. Segurança e honestidade técnica

- Segredos e tokens somente em armazenamento protegido e servidor autenticado.
- Permissões visíveis para áudio, vídeo, captura de tela e serviços externos.
- Memórias e arquivos privados, com controle de eliminação/exportação.
- Ferramentas distinguem pedido, execução, confirmação e erro.
- Não enviar dados financeiros, pessoais ou escolares a serviços não autorizados.
- Nenhuma alteração de produção ou contratação sem autorização específica.
- Não confundir “projeto fechado” com “aplicativo pronto”.

**Próximo trabalho de engenharia:** auditoria da base atual, backlog de tarefas e implementação da Fase A preservando a UI aprovada.