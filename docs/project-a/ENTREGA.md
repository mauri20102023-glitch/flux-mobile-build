# Entrega de avaliação — Projeto A 2.0.1

13/10/2026. Primeiro incremento utilizável da nova especificação: interfaces Desktop e Android, base Study e preparação técnica do Advanced. Não é a conclusão integral do Projeto A ou dos módulos anteriores.

Código testado e usado no APK/Worker: `23146b8d3c37129c693ee8d5e43ff38903711551`. Branch `codex/flux-project-a-20261013`. [PR18](https://github.com/mauri20102023-glitch/flux-mobile-build/pull/18), em avaliação; main e produção preservadas. Referência de planejamento PR17 incorporada à auditoria, não tratada como implementação.

## O que mudou

- Desktop Command Center: marca vermelha/obsidiana, navegação lateral, painel de contexto recolhível com largura ajustável, modos Normal/Command/Imersivo/Study/Vision, atalhos configuráveis e movimento reduzível.
- Mobile Android: Home com núcleo compacto e símbolo F; navegação Home/Chat/Vision/Study/Control; páginas Vision e Control; ajustes secundários; cores persistentes e redução de movimento. Ícone adaptativo e nome FLUX conferidos no launcher. Sistema de voz e captura existente preservado.
- Study: lousa de funções quadráticas, perfil qualitativo de energia e movimento uniforme na Web; funções quadráticas e energia no Android. Parâmetros locais alteram gráficos calculados. Explicação/pistas/exercícios usam o Core existente. Histórico Web exige consentimento, pode ser excluído/exportado. Android mantém identificador da conversa, sem guardar localmente o texto da aula.
- Control Web: catálogo consultado do Core com Autorizar/Testar/Revogar conforme configuração e estado reais. Google, Spotify e Samsung não são apresentados como conectados sem teste.
- Desktop é somente interface, com service binding para o Core de preview. Não duplica modelos, voz, chaves nem armazenamento. Contrato inicial de capacidades/Handoff preparado; nenhum transporte remoto é declarado funcional.

## Testes e evidências

[GitHub Actions run 73](https://github.com/mauri20102023-glitch/flux-mobile-build/actions/runs/38351214988): cinco jobs aprovados.

| Verificação | Resultado | Limite da evidência |
|---|---|---|
| Node/Core e contratos | 44 aprovados, 0 falhas | Mocks de provedores não validam contas externas |
| TypeScript e dois bundles Wrangler | Aprovados | Não comprovam serviços pagos |
| Chromium Desktop 1440×1000 e Web Mobile 412×915 | 14 aprovados, 0 falhas | Navegação, painéis, preferências, lousa, rede indisponível e contratos; sem teste acústico |
| Android 15/API35 Pixel 6 | 5 aprovados, 0 falhas/erros; log de crashes vazio no ensaio | Navegação, preferências persistentes, recursos do ícone, proteção inicial do cofre e medidor PCM; UI desconectada |
| APK release e metadados de assistente | Compilados/verificados | Assinatura Android Debug de avaliação |
| SHA256 e ZIP CRC | Aprovados | Integridade dos artefatos, não garantia física |
| Assinatura APK | apksigner: Verifies, v2=true | Certificado de avaliação; sem SourceStamp |
| Core remoto, estudo | HTTP 200, explicação real sobre catalisador | Resposta pontual; não avaliação pedagógica completa |
| Interface publicada + health/status/connect | HTTP 200; study-board.js idêntico ao fonte | Core compartilhado autenticado; contas OAuth ausentes |
| FLUX4/Inworld | HTTP 402 atual, falta crédito | Voz preservada; reprodução/acústica não validadas |

Capturas reais de Chromium e do emulador estão nos artefatos do run 73. Conferidas: Home, Command, Study, Android Home/Study e ícone instalado. Corrigidos antes da entrega: modo Imersivo que se desfazia, overflow do Chat, contraste da barra do sistema, preenchimento indevido dos eixos SVG e saudação da madrugada. Study Android aparece em ciano porque o teste alterou a preferência para verificar personalização; o padrão novo é vermelho.

APK primário: `FLUX-Mobile-2.0.1-project-a.apk`, 91.104.121 bytes, versionCode 41, pacote `ai.flux.mobile.preview.projecta`. SHA256 `1aaa85061f9737cc8fbc00ccb465d3da6ba933080e2a7697f9f4c76ebc9b0f34`.

## Ativação e pendências

1. Instalar o APK novo no Android; ele fica separado das instalações antigas. Alterar apenas o endereço do aplicativo antigo não muda sua interface. Assinatura de avaliação não garante atualização entre runners diferentes.
2. Control > Ajustes: Core `https://flux-evolution-preview.mauri20102023.workers.dev`. Usar convite Android novo, de 32 caracteres, emitido pelo servidor, uma vez por 15 minutos. Convite do navegador deve ser diferente.
3. Desktop/Chromebook: abrir `https://flux-command-preview.mauri20102023.workers.dev`, botão Parear/Ajustes, usar convite próprio. Navegador exige gesto/permissão para microfone e captura. O health informa a versão 2.0.0 do Core; a interface é 2.0.1. São componentes distintos.
4. Voz: conferir saldo/ativação em Billing da Inworld, preservando FLUX4. Nenhuma contratação foi feita. Depois Ajustes > Testar voz; três perguntas contínuas e três interrupções no Android. Barge-in automático da voz existente exige esse ensaio. O áudio da página Study possui interrupção manual; não há declaração de barge-in automático nessa página.
5. Integrações: faltam clientes OAuth próprios do FLUX e autorização das contas. [ATIVACAO.md](../evolution/ATIVACAO.md) contém variáveis secretas e callbacks exatos. Cadastros são feitos no provedor; segredos ficam no Cloudflare, não no APK/chat. Após autorização, voltar ao FLUX e Testar/Consultar dados conhecidos. Callback continua no endereço do Core de preview.
6. Android: conceder microfone/assistente, consentimento MediaProjection por sessão e acesso aos materiais escolhidos. Captura é individual, não vídeo contínuo; confirmar parar captura e comportamento de telas protegidas. Biometria/cofre exigem teste físico de autenticação/cancelamento.

| Área deste incremento | Estado | Pendência |
|---|---|---|
| Layout/navegação Desktop e Mobile | FUNCIONANDO no ambiente testado | Validação no Chromebook e Android do proprietário |
| Preferências e lousa calculada | FUNCIONANDO no ambiente testado | Conteúdos adicionais e validação pedagógica ampliada |
| Explicação pelo Core | FUNCIONANDO na API | Fluxo completo pareado no aparelho ainda não ensaiado |
| Study áudio FLUX4 | PREPARADO | Crédito Inworld e reprodução física; HTTP 402 conhecido |
| Vision individual | PREPARADO | Consentimento e captura no aparelho; modelos existentes preservados |
| Google/Spotify/Canva/GitHub/ML/SmartThings | PREPARADO no escopo existente | Cadastros OAuth, autorização e execução real; não basta conectar ao Codex |
| Professor 3D, gestos e aula adaptativa | EM DESENVOLVIMENTO | Modelagem, sincronização e verificações próprias |
| Handoff, Watch, Glasses, Ambient | EM DESENVOLVIMENTO | Contrato/especificação iniciais; transporte, SDK/hardware e testes pendentes |
| TV Samsung | PREPARADO para cliente SmartThings existente | Modelo/capacidades/OAuth; exibição física e HoloClass ainda não implementados |
| Advanced/Command Room | EM DESENVOLVIMENTO como produto | [Especificação](ADVANCED.md) e teto preliminar R$ 12.000; sem aquisição/cotação |

Demais recursos anteriores mantêm o inventário completo, datado, em [ESTADO-DAS-FUNCOES.md](../evolution/ESTADO-DAS-FUNCOES.md); evidências de 8/10 não são anunciadas como testes reais novos. Builder executor, automações completas, identidade vocal, edição de imagens, grafo de memória e conectores de saúde ainda exigem engenharia. Créditos não resolvem essas implementações incompletas.

## Recuperação e próxima validação

Preview separado e PR não integrado permitem continuar usando a versão estável. Para inspecionar o código entregue: checkout do commit acima e repetir o workflow. Para recuperar a interface, publicar um bundle da referência anterior do preview; não limpar Durable Objects nem rotacionar chaves de criptografia. Renovação administrativa realizada apenas no preview não invalidou tokens existentes de aparelhos (regressão testada).

Próxima prioridade: ensaio pareado no Android/Chromebook, voz após crédito e captura com consentimento; depois ampliar a aula interativa e preparar o avatar 3D com critérios próprios. Não há bloqueio de todo o projeto por saldo, nem afirmação de controle físico de TV/relógio/óculos.
