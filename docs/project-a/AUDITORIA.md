# Auditoria do Projeto A — 13/10/2026

Base main: 815ca9297328fe46d01e993c320ac785632640de (1.8). PR16: e92b8a34d473b04dee5efabea705b3074f1e631f (preview2.0); não integrado. PR17: 17d84e7796feee18b5987a8ae0cf1bc2e6118f0e; dois documentos de planejamento, não funcionalidades. Este incremento parte do PR16 em branch própria para não perder seus avanços.

## Verificação atual

- 35 testes de regressão do Core passaram antes das alterações. Posteriormente, 42 incluindo cálculos e preservação de autenticação da lousa/exportação passaram. Testes com respostas simuladas são contratos locais, não prova de API externa.
- Cloudflare: produção e preview existentes responderam200 à consulta administrativa de configuração. Provedores e segredos permanecem no servidor; valores não lidos/impressos. O preview mantém Workers AI para texto/visão e Inworld para voz. A voz FLUX4 não foi substituída.
- A credencial administrativa gerada anteriormente se perdeu após limpeza dos temporários. Renovada apenas no preview; autenticação dos aparelhos usa hashes independentes e não é invalidada por essa rotação. Nenhuma chave de provedor/produção foi alterada. Estudo real respondeu200 depois da recuperação; teste de voz atual consta na entrega. O402 antigo não é usado como prova de saldo atual.
- Não há acesso ao Android físico, Samsung ou relógio do proprietário. Testes em emulador/Chromium serão registrados com escopo próprio.

| Área | Evidência anterior / atual | Limite |
|---|---|---|
| Chat/router/memória/missões/imagem/visão | Evidências reais do preview registradas em docs/evolution; 35 regressões atuais | Disponibilidade real atual exige pareamento e nova chamada |
| Android voz/wake/biometria/captura | Código e builds anteriores; 5 testes anteriores de UI/recursos | Sem validação acústica, captura ou chave biométrica em aparelho físico |
| OAuth Google/Spotify/Canva/GitHub/ML/TV | Clientes preparados, tokens cifrados e confirmação | Sem cadastro/configuração/conta validada no preview |
| Interface Web | Versão mostrava1.8; script1.9; tema azul com grade e efeitos redundantes | Mudança principal deste incremento; preservar DOM/ações/voz |
| Android Home | Núcleo270dp, botões Home/Chat/Voz/Workspace/Ajustes | Compactar núcleo, trocar navegação sem retirar funções secundárias |
| Study | Missões textuais, sem lousa | Lousa calculada + perguntas ao Core + Independence; avatar3D separado |
| Dispositivos | Placeholders planejados | Remover Hub dedicado conforme nova decisão; nunca presumir pareamento |

## Riscos tratados

Identificadores HTML preservados para não quebrar funções existentes. Novos módulos usam a API atual. Desktop será um Worker somente de interface com service binding ao Core de preview; sem cópia da IA, tokens ou Durable Objects. Nova versão Android instala separada do app atual. HTTP do Core, key/voice/modelos e produção permanecem sem alteração de implementação nesta etapa.

Animação reduzida e controles manuais disponíveis. Histórico Study Web opt-in local; exportação exclui token/PIN/notas privadas. Aulas e áudio são pedidos reais, com erros visíveis. Cálculos da lousa são determinísticos e identificados como representações, nunca como dados de sensores.
