# Auditoria do documento mestre — 15/10/2026 UTC

Documento recebido: FLUX_MESTRE_CORE_ADVANCED_BUILD_2026-1.pdf, 72 páginas, edição técnica de 13/10/2026. A parte técnica inicial prevalece sobre o anexo antigo quanto à implementação do Advanced de software.

Main inspecionada: `815ca9297328fe46d01e993c320ac785632640de`. Implementação mais recente reutilizada: PR18, `861e57c75ddba375b30effd061a884c4cc6e4a23`. Branch de trabalho isolada: `codex/flux-master-20261015`. Nenhuma alteração em main, tokens, dados ou produção.

| Módulo | Evidência atual antes das mudanças | Estado / prioridade |
|---|---|---|
| Core | 44 testes Node aprovados; TypeScript aprovado | Implementado, contratos locais testados; manter |
| Android | Compose, RoleManager com verificação posterior, MediaProjection individual, Keystore, cofre e voz WebRTC | Implementado; teste físico pendente |
| Desktop | PWA e Worker de interface com service binding para o Core existente | Implementado; visual anterior vermelho, dashboard e navegação permanente diferem do PDF |
| Memória | Memórias consentidas no Durable Object; notas privadas locais cifradas; estudos opt-in | Preservar armazenamento e identificadores; não implica múltiplos perfis |
| Conectores | Clientes OAuth, leitura; comandos Spotify/SmartThings com confirmação | Implementados sem contas externas validadas nesta execução |
| Advanced | Especificação anterior; não havia ativação persistente nem painel inferior | Implementar software/preview; hardware pendente |
| Build | Missões geram texto; não havia executor de branch/PR | Não confundir geração de código com execução de desenvolvimento |

Cloudflare consultado em modo leitura: Workers `flux-mobile-build2`, `flux-evolution-preview`, `flux-command-preview` existem. Produção tem binding FLUX_STATE e nomes de segredos Inworld/ElevenLabs; valores não foram lidos. A configuração local padrão ainda aponta para `flux-core-12`, enquanto o pipeline Android aponta para `flux-mobile-build2`: implantação exige escolher explicitamente o ambiente correto. Consultas HTTP públicas de health deste ambiente retornaram 403; não houve teste autenticado novo nem rotação de credenciais. Versão instalada no Motorola, TV/firmware, saldo de voz e contas OAuth não foram acessados.

| Plataforma | Suporte demonstrado no código | Limite |
|---|---|---|
| Android nativo | Assistente, intents, captura autorizada, biometria e áudio | Permissões e testes no Motorola necessários |
| Web/PWA | Texto, microfone por gesto, captura suportada pelo navegador, armazenamento local | Não herda privilégios do Android; sem wake word permanente |
| Desktop/navegador | Mesma inteligência por service binding; janelas e painéis locais | Sem controle global de janelas, teclado físico ou monitores |
| TV Samsung | Adaptador SmartThings condicionado a OAuth/capabilities | Sem TV descoberta/controle físico comprovado |
| Touch/relógio/óculos | Contratos/previews de software | Hardware e transporte remoto não comprovados |

Reversão: não integrar a branch mantém as versões atuais. Nenhuma migração remove dados; novas preferências têm chaves versionadas. Reverter commits/bundle de interface não deve apagar Durable Objects nem trocar chaves. CI existente compila e produz APKs de avaliação; nenhuma etapa faz deploy automático de produção.
