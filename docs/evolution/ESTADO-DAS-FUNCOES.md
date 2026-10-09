# Estado das funções — FLUX 2.0 profissional

8/10/2026, horário de São Paulo. FUNCIONANDO só vale para o escopo e ambiente descritos. PREPARADO significa código compilado/testado localmente aguardando configuração ou validação real; não equivale a integração externa validada. EM DESENVOLVIMENTO contém trabalho técnico restante. Não há declaração de entrega integral.

| Requisito | Estado | Escopo e limite |
|---|---|---|
| Identidade, criador, empresas, personalidade | FUNCIONANDO no Core | Instruções preservadas; Maurício/ChatGPT/Mauright/Automobili. Humor contextual por instrução, não motor de humor independente |
| Chat contextual / rápido / padrão / profundo | FUNCIONANDO no Core | Três modelos Workers AI responderam HTTP 200; modo nativo persistente |
| Voz FLUX 4 | PREPARADO | Configuração preservada; teste real Inworld HTTP 402 por créditos |
| Wake word Flux | PREPARADO | Serviço Android com reconhecimento local quando disponível; validação física pendente |
| Conversação contínua / barge-in / eco / streaming | PREPARADO | WebRTC full duplex, VAD/AEC/NS; não validado acusticamente no aparelho |
| Reconexão automática / retomada de sessão de voz | EM DESENVOLVIMENTO | Falhas visíveis e reinício; backoff/retomada contínua incompletos |
| Horários de silêncio / níveis de proatividade | EM DESENVOLVIMENTO | Controles persistentes e gatilhos contextuais ainda incompletos |
| Voice Identity / consentimento / perfis | EM DESENVOLVIMENTO | Sem cadastro biométrico vocal; voz não autoriza segredos |
| Face ID / biometria Android | PREPARADO | Cofre usa Keystore autenticado; modalidade depende de biometria forte do aparelho; teste físico pendente |
| Vault local / geração de senha / bloqueio | PREPARADO | AES-256-GCM, autenticação por operação, FLAG_SECURE, bloqueio ao sair/60s; teste cripto físico pendente |
| Vault senha principal / backup / sync / autofill | EM DESENVOLVIMENTO | Não implementados; não usar como única cópia |
| Vision análise de imagem real | FUNCIONANDO no Core | Fixture real com retângulo vermelho e 4827 identificada pelo modelo |
| MediaProjection / aba inferior | PREPARADO | Captura individual autorizada, encerramento/expiração; validar no Android físico |
| Vision PDF / câmera | PREPARADO | Foto individual pelo app de câmera, PDFs primeiras três páginas renderizadas; consentimento antes do envio |
| Vision Live vídeo contínuo | EM DESENVOLVIMENTO | Não há streaming contínuo de câmera/tela |
| Esfera / estados / cores / navegação | FUNCIONANDO no emulador | Filamentos, navegação e cor persistente validados; reação acústica ao áudio ainda exige aparelho |
| Ícone adaptive / foreground / background / monochrome | FUNCIONANDO nos testes de recurso | Máscaras círculo/quadrado arredondado/squircle aprovadas; capturas reais do ícone instalado na gaveta e tela inicial conferidas |
| Mood / Easter Eggs | EM DESENVOLVIMENTO | Sem motor de reações/easter eggs configurável |
| Chat texto / histórico / contexto | FUNCIONANDO no Core | API e memória validadas; teste nativo pareado completo pendente |
| Anexos imagem / PDF | PREPARADO | Tela Analisar; sem continuidade de documentos completos no histórico do Chat |
| Creator gerar imagem | FUNCIONANDO no Core | JPEG real 1024 × 1024 gerado, salvo e aberto |
| Creator salvar / compartilhar | PREPARADO | CreateDocument e FileProvider implementados; seleção/compartilhamento físicos pendentes |
| Creator editar / remover fundo / substituir objetos | EM DESENVOLVIMENTO | Modelo/API de edição ainda não integrados |
| Gmail | PREPARADO | OAuth + teste + até 5 mensagens/corpo texto simples + resumo autorizado; falta cadastro e teste da conta |
| Gmail enviar / organizar / responder | EM DESENVOLVIMENTO | Escopo atual somente leitura; nenhum e-mail enviado |
| Calendar / Drive / Tasks / YouTube | PREPARADO | OAuth/leitura; falta cadastro Google e validação real; escrita de docs/eventos não implementada |
| WhatsApp pessoal | PREPARADO para abertura/compartilhamento | Envio exige usuário; sem leitura privada, resumo automático ou API pessoal de mensagens |
| Spotify / Music DJ | PREPARADO para pesquisa/controle | OAuth PKCE e comandos com confirmação; conta/dispositivo e teste real pendentes; playlists próprias incompletas |
| Canva | PREPARADO para consulta | OAuth e designs; criação/edição de apresentações ainda não implementadas |
| GitHub | PREPARADO para consulta | OAuth e repositórios públicos; sem executor/PR autônomo do FLUX |
| Cloudflare pelo FLUX | EM DESENVOLVIMENTO | Backend hospedado não equivale a integração administrativa do assistente |
| Mercado Livre | PREPARADO para perfil | OAuth PKCE; sem anúncios, vendas ou pedidos implementados |
| Instagram / Facebook | EM DESENVOLVIMENTO | Abrir/compartilhar no Android não é integração autenticada Meta |
| TV Samsung / SmartThings | PREPARADO para capacidades permitidas | OAuth, lista e switch/mediaPlayback; sem abertura de vídeos/ACK físico; modelo/conta pendentes |
| Chromebook / PWA / Handoff | EM DESENVOLVIMENTO | Interface web existe; recebimento confirmado/extension companion não implementados |
| Relógio / Health Connect / sono | EM DESENVOLVIMENTO | Sem conector; nenhum dado de saúde consultado |
| Glasses / Bluetooth | EM DESENVOLVIMENTO | Áudio do sistema possível; sem protocolo visual/glasses validado |
| ClassApp / Geekie One | PREPARADO para material compartilhado | Analisar material autorizado; sem acesso automático/login/API escolar |
| Study explicação / resumo / flashcards / simulado | FUNCIONANDO para geração textual no Core | Missão de estudo executada e persistida; sem acompanhamento adaptativo completo |
| Mapas mentais visuais / gráficos / animações | EM DESENVOLVIMENTO | Mapa textual; renderer educativo e exercícios interativos ausentes |
| Learning / Study Battle / Challenge | EM DESENVOLVIMENTO | Sem pontuação/avaliação persistente/adaptação de aprendizagem |
| Pesquisa de videoaulas / envio para TV | PREPARADO para busca YouTube | OAuth/busca preparada; avaliação/reprodução remota/handoff incompletos |
| Memory CRUD / consentimento / recuperação | FUNCIONANDO no Core | Nuvem e testes: criar, recuperar, corrigir, excluir e expirar |
| Memory múltiplas pessoas / todas camadas / graph / Time Machine | EM DESENVOLVIMENTO | Workspace do proprietário; sem grafo/histórico completo/perfis separados |
| Missions textuais | FUNCIONANDO no Core | Fila/alarme persistente, erro/cancelamento, geração verdadeira; sem executor de ações externas |
| Builder / Test Lab / branch / PR / build pelo FLUX | EM DESENVOLVIMENTO | Propostas textuais de código, sem acesso executável ao repositório |
| Detective / Recovery / Quality / Security Sentinel | EM DESENVOLVIMENTO | Diagnóstico parcial; sem rollback automático ou verificador independente completo |
| Creator / Dream Lab / Business | FUNCIONANDO para propostas textuais | Não publica anúncios, movimenta dinheiro ou executa negócio; dashboards específicos incompletos |
| Coach / Focus / Night / Daily Briefing | EM DESENVOLVIMENTO | Tarefas/briefing local parcial; agenda persistente e cronômetro completos pendentes |
| Debate / Simulation | EM DESENVOLVIMENTO como módulos | Pedidos textuais possíveis; sem rubricas e progresso especializado |
| Research com fontes | PREPARADO | Brave Search, limite e links seguros; falta chave e teste real |
| News Radar | EM DESENVOLVIMENTO | Sem monitoramento/agendamento persistente |
| Context Fusion / Quick Actions | EM DESENVOLVIMENTO | Contexto pontual; fusão autorizada ampla/sugestões ainda incompletas |
| Digital Workspace | PREPARADO | Novas telas memória/missões/análise/conexões/consumo/sistema; organização de todos arquivos incompleta |
| Multi-Agent Studio / Skills | EM DESENVOLVIMENTO | Sem coordenador de executores/agentes especializados |
| Automations / notificações persistentes | EM DESENVOLVIMENTO | Alarmes de missões não equivalem a lembretes/automações |
| Command Center / Mission Control / consumo | PREPARADO | Estados reais, quotas/estimativas; sem telemetria completa de fatura/hardware |
| Privacy Center / revogação | PREPARADO parcial | Memórias e conexões revogáveis; registro central de sessões sensíveis incompleto |
| Offline | EM DESENVOLVIMENTO | Tarefas/configurações locais; sem IA/voz offline completas |
| Autonomia quatro níveis / permissões granulares | EM DESENVOLVIMENTO | Confirmação de controles, auth, consentimento; política transversal ainda incompleta |
| Orçamento R$200 | PREPARADO como planejamento | Cenário R$176,42, alertas e contadores; teto rígido não garantido |

Não há classificação NÃO SUPORTADO por suposição de hardware/conta. Limitações oficiais são explicadas no escopo; o modelo/conta concretos precisam de validação. Sem compra ou publicação comercial realizada. ATIVACAO.md descreve ações do proprietário; linhas EM DESENVOLVIMENTO exigem engenharia adicional, não apenas pagamento.
