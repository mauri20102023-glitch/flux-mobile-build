# Ativação — FLUX 2.0 profissional

Preview: https://flux-evolution-preview.mauri20102023.workers.dev
Produção preservada: flux-mobile-build2. O APK 2.0 usa ai.flux.mobile.preview.pro, nome FLUX, e instala separado do app estável. Mudar apenas o endereço do Core não muda a interface instalada: é necessário instalar o APK novo. Não desinstale o app antigo antes de validar o novo.

## Pareamento

Instale o APK, abra Ajustes, confira o endereço do preview e use um convite emitido por `/v1/pair/invite` com autenticação administrativa no servidor. Convites têm 32 caracteres, uso único e validade de 15 minutos. Use Parear novamente. Se aparecer PAIRING_DENIED, confira endereço/validade; não tente inventar um token. A credencial resultante fica protegida no Android. Nunca colocar token administrativo no APK.

## Voz FLUX 4

A voz personalizada Inworld existente foi preservada. O teste real retornou HTTP 402, exigindo saldo. Não foi feita compra nem substituição da voz. Em Billing da Inworld, confira créditos e o plano desejado; qualquer contratação é decisão do proprietário. Evite auto-reload caso queira controlar gastos. Depois, em Ajustes, execute Testar voz. Validação física: três perguntas em sequência; três interrupções enquanto o FLUX fala; ruído; viva-voz e Bluetooth; abertura/fechamento de sessão. Registrar se o áudio anterior parou e a nova instrução foi capturada completa. Código WebRTC/AEC/VAD não comprova sucesso acústico.

## Google, Spotify, Canva, GitHub, Mercado Livre e Samsung

Conectar esses serviços ao Codex não autoriza automaticamente o FLUX. Faltam cadastros OAuth próprios do aplicativo. Os clientes e callbacks foram implementados; nenhum login ou resultado de conta foi simulado como real.

Configure variáveis secretas no Worker de preview, em Settings > Variables and Secrets, tipo Secret. Não envie chaves, senhas ou tokens pelo chat. `FLUX_CONNECT_KEY` já foi configurada no servidor e protege tokens; mantenha essa chave estável. Sua perda impede decriptar conexões existentes e exigirá nova autorização. Não copiar para o APK.

| Serviço | Variáveis necessárias | Callback cadastrado exatamente | Capacidade implementada |
|---|---|---|---|
| Google | GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET | /v1/connect/callback/gmail, calendar, drive, tasks e youtube, um URI completo para cada | Gmail: até cinco mensagens e corpo texto simples; Calendar: eventos; Drive: metadados de arquivos; Tasks: tarefas; YouTube: busca |
| Spotify | SPOTIFY_CLIENT_ID | /v1/connect/callback/spotify | Pesquisa/estado; play, pause, next, previous com confirmação |
| Canva | CANVA_CLIENT_ID, CANVA_CLIENT_SECRET | /v1/connect/callback/canva | Perfil/designs; não cria apresentações por API nesta versão |
| GitHub | GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET | /v1/connect/callback/github | Perfil/repositórios públicos; sem escrita/execução Builder |
| Mercado Livre | MERCADOLIVRE_CLIENT_ID, MERCADOLIVRE_CLIENT_SECRET | /v1/connect/callback/mercadolivre | Perfil; não gerencia anúncios ou pedidos nesta versão |
| SmartThings | SMARTTHINGS_CLIENT_ID, SMARTTHINGS_CLIENT_SECRET | /v1/connect/callback/smartthings | Lista dispositivos; on/off e play/pause quando capacidades do dispositivo permitirem |

Prefixo de todos os callbacks: `https://flux-evolution-preview.mauri20102023.workers.dev`. No Google crie cliente do tipo aplicativo Web, habilite as APIs usadas, configure tela de consentimento e usuário de teste; os escopos Gmail/Drive podem exigir verificação para distribuição. O modo de teste pode limitar a duração das autorizações. Não considerar pronto para outros usuários sem resolver essa etapa.

Spotify e Mercado Livre usam PKCE; Mercado Livre precisa desse recurso habilitado no cadastro do aplicativo. SmartThings utiliza OAuth confidencial documentado para OAuth-In, não os novos endpoints ainda anunciados como coming soon. Compatibilidade da TV depende de sua exposição na conta SmartThings e das capacidades retornadas. Aceitação HTTP de comando não prova que um vídeo apareceu na televisão.

Depois do cadastro: Workspace > Conexões > Autorizar abre a tela oficial; volte ao FLUX > Testar > Consultar. Confira dados conhecidos. Para Spotify, use dispositivo ativo e conta compatível. Revogar remove tokens locais; quando o provedor não oferece revogação implementada, remova também o acesso na central de aplicativos do provedor. Google tenta revogação remota e informa se ela falhou.

## Pesquisa com fontes

Cliente Brave Search preparado, falta `BRAVE_API_KEY`. Cadastre a conta e configure o segredo no servidor. O cadastro/plano pode exigir forma de pagamento; não foi contratado. Teste Workspace > Pesquisar com consulta conhecida e confira links reais. Trechos do índice não equivalem à leitura integral das páginas. Limite mensal persistente de 300 tentativas pelo Core; outros usos na conta e cobranças externas não são controlados por esse limite.

## Android / dados sensíveis

Microfone: conceder no aparelho. Assistente: selecionar FLUX nas configurações do Android para funções que dependem do papel de assistente. Wake word depende de reconhecimento local disponível e limitações de segundo plano; validar no aparelho, incluindo tela bloqueada. Vision: consentimento MediaProjection a cada sessão; captura individual, não vídeo contínuo. Telas protegidas podem não ser capturadas. Analisar: escolher imagem/PDF permitido ou foto pelo aplicativo de câmera; consentimento explícito antes de enviar. PDF analisa somente as primeiras três páginas.

Cofre: exige bloqueio seguro e autenticação forte/credencial do dispositivo. Há criptografia local e bloqueio ao sair/inatividade. Ainda não há backup, senha principal própria, autofill ou sincronização; não usar como única cópia de credenciais importantes. Testar autenticação e cancelamento no aparelho antes de uso.

WhatsApp pessoal: abertura/compartilhamento oficial exige revisão e envio pelo usuário. Não lê conversas privadas nem envia automaticamente. Instagram/Facebook: compartilhamento Android não equivale a acesso à conta ou API de publicação. Relógio/Health Connect, entrega confirmada ao Chromebook/TV, execução de código Builder, biometria vocal, automações persistentes e vídeo contínuo permanecem em desenvolvimento, não dependem somente de créditos.
