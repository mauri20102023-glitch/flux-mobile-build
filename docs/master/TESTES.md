# FLUX 2.1 — registro de validação

Execução em 14/10/2026, São Paulo; 15/10/2026 UTC. Responsável técnico: Codex nesta entrega. Aceite no aparelho e nas contas reais: Maurício, ainda pendente.

## Resultado automatizado

| Verificação | Resultado observado | Fonte e ambiente |
|---|---|---|
| Contratos Core, segurança, memória, conexões e Build | 60 aprovados, zero falhas | Node, Actions run78 e run79 |
| Interface Web/Desktop | 28 aprovados, zero falhas | Chromium; 1440×1000 e 412×915; run78 e run79 |
| Interface nativa Android | 7 aprovados, zero falhas | Emulador Pixel6, Android15/API35; run78 e run79 |
| TypeScript e bundles | Aprovados | Core e interface; dry-run, sem deploy de produção |
| APK release de avaliação | Compilado e assinatura v2 verificada | run78; certificado Android Debug |
| Dependências npm após correção do lockfile | Zero vulnerabilidades conhecidas | npm audit; não equivale a auditoria integral do produto |

[Run78 completo](https://github.com/mauri20102023-glitch/flux-mobile-build/actions/runs/38667824300), fonte `0b63f697a86f86c20060c83886a28592d401ba44`. Seus cinco jobs passaram. O APK entregue vem desta execução. [Run79 completo](https://github.com/mauri20102023-glitch/flux-mobile-build/actions/runs/38668906694), fonte `8d068b7b4fd6ccfe30275905fc6ba9468c4620da`, também passou nos cinco jobs após atualização apenas de ferramentas npm no lockfile. Não houve alteração do código nativo entre esses dois commits.

Os testes de provedores e GitHub App utilizam respostas simuladas e credenciais fictícias. Os ensaios de UI usam ambiente controlado, sem chamadas pagas. Aprovação nessas condições verifica comportamento e contratos; não comprova acesso OAuth, reprodução de áudio ou efeito físico.

## Cenários observados

| Cenário | Pré-requisito e passos | Observado / evidência | Estado |
|---|---|---|---|
| Home Web mínima e navegação | Abrir em desktop/mobile; abrir drawer; Escape; navegar | Esfera, uma barra, cards iniciais ausentes, foco retorna ao menu; screenshots Web do run78 | Aprovado automatizado |
| Advanced Web | Conversa existente; alternar, recarregar e desativar | Preferência persiste, conversa preservada, zero solicitações de getUserMedia | Aprovado automatizado |
| Mute Web | Silenciar; recarregar; tocar esfera; reativar em Ajustes | Estado muted persiste e início de voz bloqueado até reativação manual | Aprovado automatizado |
| Painel inferior Web | Abrir prévia; receber cards; voltar ao painel | Rótulo local explícito; sem cards fictícios; controles retornam | Aprovado automatizado |
| Conversa e ações Web | Core simulado; pergunta simples e pedido Spotify; cancelar | Texto usa contrato do Core; sem card inventado; nenhum comando antes da decisão; cancelamento enviado como approve:false | Aprovado com mock |
| Build Web | Preparar arquivo permitido; revisar; publicar sem App | Patch visível; erro de App ausente; nenhum link de PR fabricado | Aprovado com mock |
| Rotas e catálogo | Solicitar sem autenticação; consultar com sessão fictícia | Rotas protegidas; pré-requisitos e indisponibilidade explícitos | Aprovado automatizado |
| Leitura por linguagem natural | Pedidos explícitos; conta ausente e adaptador simulado autorizado | Ausência de conta não vira resultado; parâmetros limitados; agenda vazia permanece vazia | Aprovado com mock |
| Consentimento | Preparar comando; tentar outro ator, replay, expiração e cancelamento | Vínculo exato, consumo único, prazo e limite de fila respeitados; modelo não injeta confirm | Aprovado automatizado |
| Controle de dispositivos | Payloads simulados; volume inválido, capacidade ausente e verificação divergente | Comando rejeitado quando inválido; aceite não vira verificação; estado observado comparado ao solicitado | Aprovado com mock |
| Build protegido | Tentar identidade, auth, política, workflow, teste, traversal, credencial e outro repo | Alterações recusadas; App limitado; só branch de tarefa e PR draft; CI exige todos os checks do mesmo head | Aprovado com mock |
| Regressões anteriores | Exercitar memória, missões, OAuth/PKCE, revogação, visão e limites | 60 testes totais cobrem contratos anteriores e novos; sem remoção de testes para obter aprovação | Aprovado automatizado |
| Android: navegação e preferências | Instalação limpa no emulador; drawer, Study, Memória, Conexões, Privacidade | Rotas acessíveis; preferências sobrevivem reinício; estados sem pareamento explícitos | Aprovado no emulador |
| Android: Advanced | Alternar; voltar Home; recriar Activity; desativar | Estado persistente, sem iniciar captura | Aprovado no emulador |
| Android: mute | Silenciar; tentar voz principal; recriar Activity; reativar manualmente | Gate fecha/bloqueia captura; preferências persistem; aviso deixa engrenagem e menu acessíveis | Aprovado no emulador |
| Android: ícone, medição e cofre | Launcher, máscaras adaptativas, limites RMS e armazenamento privado | Sete testes nativos totais; 7 concluídos, 0 falhas | Aprovado no emulador |
| Interface publicada | Abrir preview no navegador cloud; Home, drawer, Ajustes e retorno | Página publicada observada; sem pareamento; screenshot FLUX-Preview.jpg | Aprovado visual |
| Preservação de staging | Comparar settings Cloudflare antes/depois de upload | 12 bindings e mesmo namespace Core; service binding da interface preservado | Aprovado por leitura da configuração |

## Evidências e repetição

Os artifacts `flux-web-evidence`, `flux-android-evidence` e os APKs estão associados ao run78 no GitHub Actions. As capturas entregues identificam Desktop, Mobile Web e Android. O screenshot do navegador cloud mostra a interface efetivamente publicada; os screenshots do CI mostram as fixtures. O aviso de mute foi conferido visualmente após correção do posicionamento.

Comandos usados: `npm ci`, `npm run embed:web`, `npm test`, `npm run check:types`, dois `wrangler deploy --dry-run`, `npm run test:web`, Gradle assembleRelease e connectedDebugAndroidTest. Gradle/Chromium locais não puderam baixar os respectivos binários neste ambiente; as execuções correspondentes foram realizadas no GitHub Actions. A auditoria npm foi repetida depois da correção do lockfile.

## Ensaios externos ainda não executados

| Ensaio | Pré-requisito | Passos e critério de aceite | Estado / responsável |
|---|---|---|---|
| Chat e voz no Motorola | APK, pareamento e provedor disponível | Conversar; verificar áudio, RMS, eco, interrupção e mute em principal/overlay; evidenciar resultado | Pendente — Maurício |
| Desktop/Chromebook | Navegador pareado com convite próprio | Conversa compartilhada, teclado, fontes, reconexão e Advanced; verificar consumo | Pendente — Maurício |
| Gmail, Agenda e outros serviços | Cliente OAuth do FLUX e consentimento da conta | Ler dado conhecido; comparar com origem; revogar e testar ausência de acesso | Bloqueado por configuração — proprietário das contas |
| Samsung/Spotify | OAuth, aparelho compatível e ativo | Confirmar volume/pausa; comparar resposta API, estado observado e efeito físico | Bloqueado por conta/hardware — Maurício |
| WhatsApp pessoal | Aplicativo no Motorola | Compartilhar texto, imagem e áudio; revisar antes do envio final | Pendente físico — Maurício |
| FLUX Build real | GitHub App restrito e proteção de main | Preparar patch permitido; aprovar; inspecionar branch/PR; exigir CI no head; revisão humana | Bloqueado por configuração — administrador do repo |
| Painel inferior e wearables físicos | SDKs, dispositivos e companion | Parear; transportar card; receber ACK; retornar ao painel; desconexão visível | Não implementado integralmente — próxima etapa |

Não houve merge, deploy de produção, alteração de segredo ou compra. O aceite completo do documento mestre depende da implementação restante e desses ensaios; esta entrega não declara o ecossistema inteiro concluído.
