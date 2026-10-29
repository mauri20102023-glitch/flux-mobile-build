# FLUX INFINITY TITAN — projeto executivo V1.0

**Data:** 29/10/2026  
**Estado:** ESPECIFICAÇÃO, não implementação concluída.  
**Limites autorizados para planejamento:** R$ 1.000 uma vez em despesas pontuais e R$ 300/mês na operação. **Nenhuma compra autorizada individualmente.**

## Decisão de arquitetura

Partir do FLUX existente — Web/PWA, Android nativo em Kotlin/Compose e Core Cloudflare Workers — preservando os protótipos FLUX INFINITY Visual System V1.0 / Morph V8. Evitar reescrever a interface. Aplicar UX responsiva, com home minimalista, esfera e cards contextuais. O Motorola Edge 50 Neo é o dispositivo móvel principal.

Arquitetura em camadas:

```
Android / Desktop (PWA)
   ↓ sessão autenticada
FLUX Core (Cloudflare Worker)
   ├── FLUX Session / Auth / Spending Guard
   ├── FLUX Conversation Router (ElevenLabs FLUX ATH para voz)
   ├── FLUX Text/Vision Adapter (modelo substituível)
   ├── FLUX Memory Graph (D1 + cache local)
   ├── FLUX Tools / Capability Registry / Audit
   └── FLUX Morph JSON Renderer → biblioteca de cards / Genesis Three.js
```

O agente ElevenLabs existente **FLUX ATH** deve ser lido antes de qualquer mudança (agent_id `agent_5701m38je3dweertjnjsk2fegsbk`). A IA não recebe acesso irrestrito a sistemas, credenciais ou JavaScript executável. A geração de cards se dá por **esquemas JSON validados e componentes permitidos**.

## Escopo V1 — prioridade alta

| Módulo | Recurso / definição de pronto |
|---|---|
| Home + Morph | Cards responsivos, reorganização dinâmica, fixar/restaurar, transições; dados verdadeiros ou sinalizados como demonstração |
| Chat | Histórico persistente, títulos, pesquisa, reabrir conversas, mensagens de voz transcritas, exportar |
| Live | Sessão real via ElevenLabs FLUX ATH, mute, interrupção, desconectar, estado real, tempo de sessão e limite |
| Memory | Memórias por projeto, consultar/editar/apagar, apenas contexto autorizado e rastreamento de origem |
| Planner + Chronos | Tarefas, prioridades, lembretes locais, linha do tempo e persistência |
| Business | Simulador de margens, veículos e estoque; importação CSV, gráficos baseados em dados reais |
| Study | Revisões, simulados/flashcards e explicações visuais com exercícios |
| Genesis 3D | V8 demonstrativo e biblioteca pequena de cenas navegáveis, com controles reais |
| Forge | Ferramentas construídas por templates seguros: calculadora, tabela, checklist, comparador |
| System | Configurações, perfil, permissões, orçamento, diagnóstico e backups |
| Mobile | Usar a interface já aprovada e o app Android existente; preservar a experiência no Motorola |
| Desktop | Reutilizar PWA / Command Center com workspaces persistentes |

## Expansões condicionadas à estabilidade e verba

- **Vision/Snap:** imagens e captura de tela explícitas; OCR e respostas de modelo de visão quando autorizado.
- **FLUX Link:** OAuth e APIs oficiais de Google Calendar/Gmail ou outros conectores realmente disponíveis.
- **Handoff:** continuar chat e workspace entre Android e computador.
- **Mission Control:** missões em etapas e aprovação de operações.
- **HoloDesk / X-Ray:** mesa espacial e visualização de camadas de modelos conhecidos.
- **FLUX Build:** prepara branch, PR, CI e mudanças testáveis; instalação sempre depende de aprovação.
- **Device Fabric:** conexão e comando em dispositivos com API disponível e autenticada.
- **Voice Offline:** motor local para comandos/avisos; qualidade e velocidade devem ser medidas no aparelho.

Não declarar concluídas geração 3D ilimitada, clonagem premium com funcionamento offline, autonomia irrestrita no Android, criação ilimitada de vídeos, controles sobre apps sem APIs ou 60 min/dia de ElevenAgents.

## Orçamento — investimento único (reservas máximas)

| Grupo | Valor |
|---|---:|
| Ensaios de voz, visão e modelos | R$ 150 |
| Processamento pontual para testes e compilações | R$ 200 |
| Assets 3D/ícones licenciados (se necessários) | R$ 100 |
| Backup/testes/distribuição pontuais | R$ 150 |
| Reserva ainda não comprometida | R$ 400 |
| **Máximo** | **R$ 1.000** |

Nenhuma categoria implica compra automática, e não se trata de preço de contratação de desenvolvedores.

## Orçamento — operação mensal (teto rígido)

| Grupo | Teto mensal |
|---|---:|
| ElevenLabs — assinatura, se aprovada após auditoria | R$ 140 |
| Minutos extras de voz pré-pagos manualmente | R$ 40 |
| Modelo de texto e Vision | R$ 55 |
| Infraestrutura e backups | R$ 15 |
| Contingência | R$ 50 |
| **Máximo** | **R$ 300** |

Tarifas publicadas na ElevenLabs em outubro/2026: Creator US$ 22/mês, com 275 minutos de chamada incluídos; US$ 0,08/min adicional; modelo de linguagem cobrado à parte. 275 minutos equivalem a aproximadamente 9 minutos/dia em 30 dias. Para 1 hora/dia, seriam 1.800 minutos no mês, além do teto financeiro proposto. Conferir câmbio, IOF, tributos e tela de checkout antes de qualquer assinatura.

**Política financeira:** gasto em reais por mês não pode exceder R$ 300; alerta preventivo e bloqueio técnico por serviço; PAYG somente manual, sem auto-top-up; encerrar chamadas inativas; texto como alternativa. Se o plano atual ou o câmbio não couberem no limite, não ativar.

Fontes:
- https://elevenlabs.io/pricing/agents
- https://elevenlabs.io/docs/overview/administration/pay-as-you-go
- https://developers.cloudflare.com/workers/platform/limits/
- https://developers.cloudflare.com/d1/platform/pricing/
- https://developers.cloudflare.com/workers-ai/platform/pricing/
- https://ai.google.dev/gemini-api/docs/pricing

## Fases e aceitação

**F0 — Auditoria:** mapear main, PRs 16–23, assets e funcionalidades, com backup e relatório verificável.

**F1 — Base persistente:** chat, memória, contas, planner, D1/cache. **Aceitar** quando fecha/reabre sem perda, restaura dados e trata falhas.

**F2 — Inteligência:** FLUX ATH, credenciais efêmeras, transcrição, LLM de texto, custo. **Aceitar** após sessão real de voz, interrupção, logs sanitizados e medição de gastos.

**F3 — Morph + Genesis:** layouts por JSON validado e cenas reutilizáveis. **Aceitar** quando cards/cenas mudam, salvam e restauram sem executar código gerado arbitrariamente.

**F4 — Ferramentas:** Study, Business e Forge seguros, com dados e contas verificáveis.

**F5 — Expansão:** Vision, integrações, Handoff, automações, apenas após estabilidade e avaliação financeira.

**F6 — Avançado:** FLUX Build, Device Fabric e HoloDesk conforme infraestrutura e limites.

Não informar prazos fictícios. Não afirmar entrega sem testes.

## Segurança e limites

- Nunca incluir chaves em GitHub/cliente; backend faz proxy autenticado.
- Sessões por dispositivo, proteção dos dados, backup e exclusão.
- Microfone/câmera sempre com permissão explícita.
- Toda integração exige capability registry com permissões e confirmação.
- Registro de ações separa "pedido", "em execução", "confirmado" e "falhou".
- Sem deploy nem merge em produção sem revisão e aprovação.
- Distinguir dados reais de demos; cenas técnicas 3D ilustrativas não equivalem a simulações físicas certificadas.

## Próxima entrega de engenharia

Auditar o código e os PRs; elaborar migração de chats/memória e o roteador de sessões; criar branch de implementação a partir de base estável, executar testes no Android e desktop e medir custos antes de comprar qualquer serviço.

**Repositório:** https://github.com/mauri20102023-glitch/flux-mobile-build
