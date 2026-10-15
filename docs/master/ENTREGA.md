# FLUX 2.1 — entrega de avaliação do documento mestre

14/10/2026, horário de São Paulo (15/10 UTC). Incremento verificável sobre o FLUX existente; não é certificação de conclusão integral do documento de 72 páginas.

## Acesso e revisão

- Desktop/Web: https://flux-command-preview.mauri20102023.workers.dev/ — home e ajustes conferidos no navegador após publicação de avaliação.
- Core de avaliação: https://flux-evolution-preview.mauri20102023.workers.dev — mesma instância e mesmo namespace de dados anteriores.
- Código: PR19, https://github.com/mauri20102023-glitch/flux-mobile-build/pull/19, empilhado sobre PR18 para preservar a implementação mais recente. Main e produção flux-mobile-build2 não foram alteradas.
- Android: APK 2.1.0-master-preview, versionCode42, pacote ai.flux.mobile.preview.master. Instalação de avaliação separada. Exige pareamento próprio; não importa silenciosamente credenciais de outro aplicativo.

## Implementado neste incremento

Home Mobile/Web/Desktop com preto, grafite, prata e branco metálico, esfera central, uma barra de comunicação e navegação recolhida. Conexões, Dispositivos, Vision, Study, Memória e ajustes permanecem acessíveis. A esfera reutiliza a voz atual e reflete estados observados; medição do microfone não é apresentada como medição do áudio TTS. Estados sem conexão, mute e erros são explícitos.

Advanced persiste entre sessões sem criar outra inteligência, ligar o microfone ou conectar dispositivos. A transformação respeita redução de movimento. O painel inferior Web é uma prévia local com controles fixos, recepção dos cartões solicitados e retorno ao painel. No Android, há explicação da prévia; não há controle de um monitor físico nem confirmação remota de transferência.

Microfone do FLUX pode ser silenciado e reativado manualmente. No Android o mute encerra os donos de captura do processo, incluindo sessão principal, overlay e wake word; impede reinício e persiste. Reativar não habilita automaticamente a palavra Flux. Isto não corta fisicamente o sinal nem silencia outros aplicativos. O aviso de mute foi reposicionado para preservar acesso à navegação.

Registro de capacidades com esquemas, pré-requisitos, confirmação e verificador. O runtime recebe o catálogo do servidor. Consultas textuais explícitas sobre agenda, e-mails, memória e dispositivos chegam aos adaptadores autorizados. Pedidos textuais de pausa no Spotify e volume absoluto de uma única TV identificada preparam consentimento, sem executar antecipadamente. Se há mais de um aparelho ou metadados insuficientes, a seleção manual é necessária.

Confirmações vinculadas à sessão, capacidade e parâmetros; expiram em cinco minutos, são consumidas uma vez e têm fila limitada. Web e Android mostram a revisão antes da execução. SmartThings aceita volume somente em aparelho que anuncia audioVolume; o estado posterior é comparado ao solicitado. Spotify compara is_playing. Resposta da API sem correspondência permanece “aceito, não confirmado”; não comprova efeito físico.

Build prepara patches revisáveis e possui adaptador GitHub App para branch de tarefa, commit e PR em rascunho. Sem GitHub App configurado, a escrita fica indisponível. Main precisa exigir revisão, verificações atualizadas, proteção de administradores e bloqueio de force push/exclusão. O executor aceita componentes, adaptadores e documentação de funcionalidades; rejeita caminhos de identidade, autenticação, política, testes, workflows e credenciais aparentes. Não há executor de terminal, merge ou deploy de produção. Geração de proposta textual não é anunciada como desenvolvimento já executado.

## Estado por módulo e limites

| Área | Estado desta entrega | O que falta para o aceite completo |
|---|---|---|
| Home, drawer, paleta e responsividade Web | Implementados; 28 testes Chromium aprovados | Ensaio no Chromebook do proprietário |
| Home e navegação Android | Compilados; ensaio final registrado em TESTES.md | Motorola físico, teclado/tamanhos de fonte e consumo real |
| Advanced de software | Persistência, animação e prévia local implementadas | Display/teclado físicos, companion e ACK de transporte remoto |
| Registry e orquestração | Contratos testados; publicado no Core de avaliação | Chamadas autenticadas novas e contas autorizadas no FLUX |
| Agenda/e-mail/Drive/Canva/YouTube | Adaptadores anteriores preservados | Cadastros OAuth, consentimento e uso real; criação/envio/edição incompletos conforme catálogo |
| Samsung/Spotify | Executor e verificação preparados | OAuth, TV específica, standby, aparelho ativo/Premium quando exigido e efeito físico |
| WhatsApp pessoal | Compartilhamento assistido existente preservado | Ensaio de texto/imagem/áudio no Motorola; envio final pelo usuário |
| WhatsApp Business | Planejado | Conta Business, media/messages/webhooks e adaptador dedicado |
| FLUX Build | Patch, branch/PR e verificador CI preparados | GitHub App próprio, configuração de main, teste real; leitura contextual/planejamento autônomo e sandbox completos ainda ausentes |
| Memória/Vault | Implementações existentes preservadas | Perfis separados, biometria/backup/restauração físicos e sync completa |
| Voice/Live | Provedor e identidade preservados | Crédito/conta, acústica, eco, barge-in e teste físico atual; catálogo conhecido em Live não equivale a executor remoto de todas as ferramentas |
| Study/HoloClass | Lousa e tutor anteriores preservados | Professor 3D sincronizado e validação pedagógica completa |
| Relógio/óculos, notificações e escola | Planejados/parciais conforme catálogo | SDKs, hardware, APIs/contas e implementação complementar |

Uma chamada configurada ou mock aprovado não é prova de funcionamento externo. Nesta execução não houve novas consultas autenticadas de chat/voz/OAuth em contas reais. O navegador de avaliação abriu sem pareamento; consulta pública direta a /health ficou bloqueada pelo ambiente de navegação. Não foi lido ou rotacionado nenhum segredo para contornar isso.

## Publicação de avaliação e reversão

Core: upload aceito pela API Cloudflare com HTTP 200, deployment_id `4fe4b4da6df243c7a689a79deb947059`. Preservados os 12 bindings anteriores, inclusive AI, Inworld, chave de criptografia, autenticação, prompt e FLUX_STATE. Namespace preservado: 4b2fa902e8f14f5d96ea8787cff88ddf. Não houve migração destrutiva.

Interface: upload de avaliação aceito com HTTP 200, deployment_id `79a779b0dc9a46cabb9ed6cb89dafa8e`. Service binding FLUX_CORE continua apontando para flux-evolution-preview; não cria outro backend, memória ou credenciais. A home, o drawer e a página de ajustes foram observados após a atualização; o estado sem pareamento foi mantido honesto.

Rollback de avaliação: Core anterior 60bd3ef2-d249-43d5-82ef-60d434be2a83; interface anterior cdc11dfc-b9e3-446b-a727-0e721693a23a. Reimplantar as versões/bundles anteriores preservando bindings e Durable Objects. Não apagar dados, recriar namespaces ou trocar chaves de criptografia. Produção permanece fora desta publicação.

## Ativação e pendências concretas

Instale o APK de avaliação no Android e use um convite próprio de pareamento com o Core de avaliação. Convites são emitidos pelo administrador autenticado do Core, duram 15 minutos e são de uso único. O Desktop precisa de convite distinto, salvo quando já possui um token válido no mesmo navegador. Não envie tokens ou chaves em chat.

As autorizações de Gmail, Cloudflare, GitHub e outros aplicativos neste ChatGPT não são transferidas ao FLUX. Os clientes OAuth devem ser registrados para este produto. A lista de variáveis e callbacks anteriores continua em docs/evolution/ATIVACAO.md.

Build requer FLUX_BUILD_APP_ID, FLUX_BUILD_INSTALLATION_ID e FLUX_BUILD_PRIVATE_KEY em PKCS8 no servidor. Instalação limitada ao repositório mauri20102023-glitch/flux-mobile-build; permissões contents:write, pull_requests:write, checks:read e administration:read. Não conceder administração de escrita. Checks obrigatórios: web, web-ui, android e android-ui; revisão humana antes de qualquer integração.

O APK usa certificado Android Debug de avaliação. A assinatura v2 e SHA256 foram verificadas; isso não garante atualização sobre uma instalação assinada por outro runner. Nenhum APK foi instalado no aparelho físico do proprietário nesta execução.

Próximo portão: ensaio pareado no Motorola/Chromebook, credenciais próprias das integrações e teste real do Build. Merge e produção exigem aprovação explícita do proprietário, como solicitado no documento mestre. Nenhum hardware ou plano pago foi adquirido.

## Artefato Android verificado

APK avaliado: FLUX-2.1.0-Avaliacao.apk, 91,153,273 bytes. Fonte nativa testada: `0b63f697a86f86c20060c83886a28592d401ba44`; [Actions run78](https://github.com/mauri20102023-glitch/flux-mobile-build/actions/runs/38667824300). SHA256 `002d604933459d2bea5ee2450d863cc80ba3302caf601ae361e695d6a00d8ff7`. Assinatura v2 verificada com apksigner; certificado de avaliação.

A auditoria npm inicial encontrou quatro alertas nas ferramentas de desenvolvimento (Wrangler/Miniflare, Sharp e Undici). Atualização compatível do lockfile aplicada; auditoria repetida retornou zero vulnerabilidades conhecidas. Testes Node, TypeScript e ambos os bundles foram repetidos com as dependências corrigidas. Esses pacotes de desenvolvimento não são embarcados no APK. Isto não equivale a uma auditoria completa de segurança do produto.
