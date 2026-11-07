# FLUX INFINITY — DECISÕES FUNCIONAIS FECHADAS
**Data:** 06/11/2026 · **Status:** especificação aprovada para planejamento, não significa módulos implementados.

## Regras imutáveis
- Investimento inicial: **R$ 0**. Orçamento operacional máximo **R$ 300 por mês, total consolidado inclusive impostos, assinaturas, câmbio e APIs**. Não há autorização para comprar, contratar ou iniciar cobrança.
- Priorizar reutilizar o repositório e protótipo aprovado: Android Kotlin/Compose, Desktop/PWA, Cloudflare Core, Morph V8. Não migrar toda a base nem reescrever telas sem necessidade.
- O usuário delegou decisões **de especificação**, não deu autorização para efetuar transações, configurar faturamento, enviar dados confidenciais ou publicar mudanças em produção.
- Primeiro funcionalidades verificadas, depois visual definitivo. Interface: grafite/preto, branco metálico, esfera energética, cards contextuais, movimento suave e acessibilidade.
- Sempre diferenciar **planejado**, **protótipo**, **integrado**, **testado** e **produção**.

## Módulo 01 — Intelligence Router
1. Estratégia econômica e inteligente **aprovada pelo usuário**: usar o serviço mais barato que satisfaça qualidade e segurança.
2. Aprovação explícita **por operação premium** **aprovada pelo usuário**. Mostrar provedor, objetivo, dados enviados, custo estimado, limite máximo e possibilidade de cancelamento. Para voz, aprovação pode abranger uma sessão de duração/preço limitados.
3. Se o premium for recusado, **executar somente as etapas gratuitas e deixar o restante pendente** **aprovado pelo usuário**. Não contratar substituto pago e não refazer trabalho concluído.
4. **Verifier:** testar/verificar antes de entregar; duas tentativas gratuitas sensatas, sem loops caros; se insuficiente, registrar bloqueio e oferecer serviço premium com pedido de aprovação, não acioná-lo.
5. **Router:** categorizar tarefas em determinístico/local → modelo econômico → especialista pago autorizado; considerar privacidade, qualidade, latência, saldo e disponibilidade. A escolha do fornecedor é intercambiável e não se baseia em rankings absolutos.
6. **Budget Guardian:** trava de orçamento interno + limites na conta de cada fornecedor; monitorar custos efetivos e compromissos em BRL; reservar margem para câmbio/tributos; se não houver trava externa confiável, não ligar faturamento automático; nunca garantir que um alerta sozinho evita cobranças.
7. **Smart Cache:** reutilizar imagens, TTS e análises com origem/proveniência; evitar refazer processos por duplicação.
8. **Audit:** mostrar provedor/versão, justificativa de escolha, chamadas, custo estimado e sucesso/erro.
9. Sem IA disponível: continuar funcionalidades locais e deixar explícito o que está offline/pendente.
10. ElevenLabs FLUX ATH é **interface de voz/conversa**, não única dona da orquestração; o FLUX Core coordena outras APIs e ferramentas, com autorizações próprias.

## Módulo 02 — Voz, presença e personalidade
- Personalidade original, natural, firme, cordial e crítica quando necessário; sem bajulação e sem palavrões.
- ElevenLabs FLUX ATH é integração preferida para Live; manter Voice ID já existente até teste de troca. Não prometer voz premium ilimitada ou que assinaturas OpenAI pagam a ElevenLabs.
- Push-to-talk primeiro; wake word local só se compatível com o Android e com bateria/privacidade aceitáveis.
- Barge-in, mute, legenda/transcrição, pausa e encerramento de sessão; limites por sessão e aviso de consumo.
- Áudio offline simples como alternativa, sem prometer qualidade ElevenLabs.

## Módulo 03 — Chat + File Dock
- Histórico local/persistido seguro: novas conversas, títulos, busca, renomear, arquivar, exportar, continuar e apagar.
- Campo único com anexos: PDFs, slides, imagens, DOCX, planilhas, áudio e outros arquivos suportados; validar formato/tamanho, leitura e consentimento antes de enviar a provedores.
- Visualizar arquivos, transcrição e respostas no mesmo histórico; jamais prometer que o agente pode ler um formato sem parser real.

## Módulo 04 — Memory Graph
- Memórias por sessão, projeto e longo prazo, rastreáveis à origem e data; recuperar apenas contexto relevante.
- Usuário pode visualizar, corrigir, apagar, exportar e desativar; dados sensíveis com proteção reforçada.
- Não expor senhas/tokens ao LLM; armazenamento local seguro e sincronização apenas com autenticação.

## Módulo 05 — FLUX Study Generator
- Adotar como referência funcional **FLUX_Study_Equinodermos_Voz_FLUX.html**: aulas divididas em capítulos, ilustrações, narração, flashcards, quiz, respostas explicadas, resultados, trilha de revisão.
- Fluxo: anexar material → extrair conteúdo e fontes → plano de aula → gerar conteúdo visual → exercícios → narração autorizada/cache → salvar aula e desempenho.
- Explicações verificáveis e imagens sinalizadas como ilustrações; recursos pagos recusados ficam pendentes, sem descartar aula parcial.
- Corrigir erros e gerar revisão baseada em respostas; sem atribuir à imagem conteúdo que não está presente no material.

## Módulo 06 — Morph + Genesis + Studio
- Home ociosa minimalista; cards contextualizados; permitir fixar, ocultar, editar, reorganizar e restaurar.
- Morph escolhe por esquemas JSON validados e componentes previamente autorizados, nunca por JS arbitrário de IA.
- Genesis: 3D real com bibliotecas/modelos existentes, rotação, zoom, separação, cenas preservadas; diferenciar demonstração de simulação científica válida.
- Studio: imagens/diagramas/editoração local; geração premium de imagens só após autorização e provedor habilitado; salvar resultado na conversa e nos arquivos.
- Interfaces Desktop/Mobile adaptativas, evitando efeitos pesados em hardware limitado.

## Módulo 07 — Forge, Actions, Mission Control, Build
- Forge gera mini ferramentas via templates seguros, com validação de inputs.
- Mission Control divide tarefas em etapas e estados verificáveis, aprova operações externas e registra falhas.
- Build prepara especificação, branch, PR e testes em sandbox; **nunca atualizar produção, instalar APK nem manipular segredos sem aprovação explícita**.
- Permissões granulares por ferramenta, logs sanitizados e possibilidade de reverter mudanças.

## Módulo 08 — Planner, Chronos, Business
- Planner: tarefas, lembretes locais confiáveis, prazos, calendário opcional e sincronização autorizada.
- Chronos: linhas do tempo de projetos baseadas em registros, não suposições.
- Mauright: simulação líquida de venda, tarifas/frete/tributos quando conhecidos, importação CSV e análise real sem inventar margem.
- Automobili: cadastro de veículos, custo, materiais comerciais, fotos e calendário editorial; não publicar ou gastar sem confirmação.

## Módulo 09 — Vision, Nexus e dispositivos
- Vision via imagem/câmera/captura **somente** com permissão e indicador visível; processamento local quando suficiente, serviço externo só com autorização e limites.
- Prioridade: Android Edge 50 Neo + Desktop PWA; depois Handoff e TV Samsung compatível. Óculos e relógio como extensão futura.
- Integrações de contas e dispositivos via APIs oficiais/OAuth, conforme disponibilidade; não inventar controles ou usar scraping proibido.

## Módulo 10 — Segurança, custos e operação
- Chaves em backend protegido; autenticação, criptografia, backup, auditoria, exclusão de dados, permissões e recuperação.
- Painel de gastos por modelo/serviço e por tarefa, teto global R$300, confirmação individual premium, bloqueio antes de ultrapassar limites quando suportado.
- Testes de falha de rede, consumo, rollback, privacidade, dados e compatibilidade Android/Desktop.
- Nenhuma assinatura, domínio, GPU, hardware ou despesa sem decisão específica posterior.

## Ordem de execução
**1 — Inspecionar e preservar.** Auditoria GitHub / PWA / Android, consolidar protótipos já existentes.
**2 — Fundamento de produto.** Chat persistente, File Dock, memória, Planner e salva/restaura estado.
**3 — Study Generator.** Primeiro caso-alvo: aula nova semelhante à de Equinodermos, com quiz e imagens e sem dependência premium obrigatória.
**4 — Router e voz.** Integrar provedor econômico, FLUX ATH opcional e Budget Guardian, testar custos reais.
**5 — Morph + Genesis.** Dinamismo comandado por intenção e esquemas seguros, mantendo interface aprovada.
**6 — Forge/Business/Vision.** Implementar com testes reais.
**7 — Link/Nexus/Build/Advanced.** Expandir só após segurança e orçamento confirmados.

## Aceitação
Nenhuma função é "pronta" sem teste funcional reproduzível, tratamento de erro, modo sem conexão quando aplicável e documentação da origem dos dados. O protótipo atual não equivale a todo o projeto funcionando. **Este documento apenas fecha decisões de produto.**
