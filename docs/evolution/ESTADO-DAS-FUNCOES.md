# Estado das funções — FLUX Evolution

FUNCIONANDO exige evidência no ambiente indicado; implementação compilada não comprova hardware. Esta matriz inicial será complementada pelos resultados reais em TESTES.md.

| Função | Estado inicial | Falta / evidência necessária |
|---|---|---|
| Chat + identidade do criador | FUNCIONANDO no preview | Cloudflare Llama 4 Scout respondeu 200 e recuperou memória CIANO4827; Inworld atualmente HTTP 402 |
| Voz FLUX 4 MP3 | PRECISA DA SUA AÇÃO | Inworld HTTP 402 no teste de 8/10. Conferir créditos em Billing e repetir teste de áudio |
| Clima | FUNCIONANDO na produção 1.8 | API com coordenadas reais; autorizar localização no novo app |
| Agenda Android | FUNCIONANDO na produção 1.8 | Nove eventos lidos no Core; autorizar calendário no preview |
| Ativação pela palavra Flux | PRECISA DA SUA AÇÃO | Selecionar preview como assistente e testar Android em tela bloqueada/aberta |
| Conversa contínua / barge-in / eco | EM DESENVOLVIMENTO | Novo controlador WebRTC; testar interrupções repetidas no aparelho |
| Recuperação automática de áudio | EM DESENVOLVIMENTO | Erro explícito e reinício manual; reconexão contínua ainda incompleta |
| Identidade vocal / consentimento / exclusão | EM DESENVOLVIMENTO | Nenhum cadastro biométrico implementado |
| Face ID / biometria forte | EM DESENVOLVIMENTO | Implementar BiometricPrompt e fallback seguro |
| Cofre / Autofill / backup criptografado | EM DESENVOLVIMENTO | Não guardar senhas na memória |
| Análise de imagem recebida | FUNCIONANDO no backend | Teste real leu 4827, calculou 12 e identificou círculo vermelho; captura Android ainda depende do aparelho |
| MediaProjection fallback | EM DESENVOLVIMENTO | Consentimento por sessão e foreground service ainda não implementados |
| Câmera | EM DESENVOLVIMENTO | Captura única web implementada; nativa e vídeo contínuo pendentes |
| Gerar imagens | FUNCIONANDO no preview | JPEG 1024 × 1024 criado por /v1/images, salvo e visualizado; ajuste para quatro passos após erro inicial |
| Editar imagens / PDF | EM DESENVOLVIMENTO | Upload de imagem não é edição; PDF exige extração/renderização |
| Gmail / Google Calendar OAuth / Drive / Docs / Sheets / Tasks | PRECISA DA SUA AÇÃO | Conta Google + projeto OAuth com permissões mínimas; conectores FLUX ainda precisam ser implementados |
| WhatsApp | EM DESENVOLVIMENTO | Abertura de app não comprova leitura/envio; investigar conta e API oficial |
| Spotify | PRECISA DA SUA AÇÃO | Conta e OAuth de app; conector e testes de reprodução pendentes |
| YouTube / Maps | EM DESENVOLVIMENTO | Abrir URL local disponível; pesquisa/verificação remota ainda incompletas |
| GitHub / Cloudflare no FLUX Builder | EM DESENVOLVIMENTO | Codex consegue operar conectores, mas isso não os disponibiliza automaticamente ao FLUX |
| Canva / Instagram / Facebook / Mercado Livre / contatos | EM DESENVOLVIMENTO | Sem executor autenticado testado |
| ElevenLabs | PRECISA DA SUA AÇÃO | Provedor opcional; a voz preservada é Inworld e não se exige migração |
| Chromebook | PRECISA DA SUA AÇÃO | Abrir preview HTTPS e parear navegador próprio; PWA disponível |
| TV Samsung | PRECISA DA SUA AÇÃO | Informar modelo e autorizar SmartThings; transporte/ACK de vídeo ainda ausentes |
| Relógio / saúde / sono | PRECISA DA SUA AÇÃO | Informar modelo e aplicativo; conector e consentimento Health Connect pendentes |
| Glasses / Bluetooth | EM DESENVOLVIMENTO | Áudio depende do sistema; não há conector visual |
| Handoff confirmado | EM DESENVOLVIMENTO | Memórias compartilhadas não equivalem à entrega confirmada de tarefas |
| ClassApp / Geekie One | PRECISA DA SUA AÇÃO | Compartilhar material autorizado; APIs oficiais ainda precisam de investigação |
| Study / mapas / quizzes / flashcards | EM DESENVOLVIMENTO | Missões produzem estudo textual; sem avaliação adaptativa ou renderer de mapas nesta fase |
| Memória persistente / corrigir / esquecer | FUNCIONANDO no backend | Criação 201, recuperação em nova conversa 200; testes locais de correção/exclusão/expiração passaram; teste de nuvem complementado em TESTES.md |
| Time Machine / knowledge graph / separação de pessoas | EM DESENVOLVIMENTO | Sem índices/histórico completos |
| Missões textuais | FUNCIONANDO no backend | Alarme Cloudflare executou estudo real e registrou queued → running → completed; falha Inworld também registrada |
| Builder / testes / PR / rollback pelo FLUX | EM DESENVOLVIMENTO | FLUX ainda não executa código ou altera repositório |
| Creator / Dream Lab / Business | EM DESENVOLVIMENTO | Geração de textos/imagens; ações comerciais e exportações ainda incompletas |
| Coach / Focus / Night Mode / Briefing | EM DESENVOLVIMENTO | Briefing local existe; agendamento e níveis de proatividade pendentes |
| Debate / Simulation | EM DESENVOLVIMENTO | Conversa possível; rubricas e acompanhamento não implementados |
| News Radar / Research | EM DESENVOLVIMENTO | Sem pesquisa/fonte confiável conectada ao Core |
| Music DJ | EM DESENVOLVIMENTO | Depende de Spotify autenticado e executor |
| Context Fusion / Quick Actions | EM DESENVOLVIMENTO | Clima/agenda/imagem pontual; combinação e sugestões completas pendentes |
| Digital Workspace / Skills / Multi-agent Studio | EM DESENVOLVIMENTO | Estrutura parcial; sem agentes especializados executores |
| Automações / notificações persistentes | EM DESENVOLVIMENTO | Alarmes de missões não notificam compromissos |
| Command Center / Mission Control / Privacy Center | EM DESENVOLVIMENTO | Painéis parciais; logs/consumo/biometria/sessões não centralizados |
| Offline | EM DESENVOLVIMENTO | Shell web e tarefas locais; sem IA/voz offline completas |
| Segurança / autonomia quatro níveis | EM DESENVOLVIMENTO | Autenticação bearer e consentimento; sem autorização granular/biometria completa |

Nenhuma função está classificada NÃO SUPORTADO por suposição. Isso requer demonstração da limitação no dispositivo/conta concreta.

## Pendências que dependem do proprietário

- Android: instalar APK preview lado a lado, conceder microfone, selecionar como assistente se desejar wake/Vision; verificar uma conversa com três interrupções e captura real. Não instalar sobre o app estável.
- Google: futura autorização OAuth na tela oficial; não enviar senha ou token no chat. Verificação: listar dados autorizados e revogar acesso.
- Spotify: futura autorização oficial da conta; teste: pausar música no dispositivo ativo e confirmar mudança real.
- TV/relógio: informar modelos e apps já usados. Verificação: executar tarefa e receber confirmação do dispositivo, não só abrir app.
- Material escolar: compartilhar arquivo/captura autorizados. Verificação: resposta cita o exercício realmente fornecido.
- Contas/cloud: nenhum segredo deve ser enviado em mensagem. Configurar apenas campos de segredo do provedor ou fluxo seguro de autorização.
