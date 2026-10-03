# FLUX — auditoria técnica e de produto (2 de outubro de 2026)

## Escopo e critério

Inspeção do código Android, do site, do Worker e do fluxo de build na branch principal após a versão 1.7.6. Os estados abaixo distinguem implementação em código, compilação, validação em aparelho e operação publicada. Um build ou um campo `configured` não prova que uma conversa ou integração funcionou.

## Inventário

| Área | Implementação encontrada | Estado comprovado |
| --- | --- | --- |
| Android | Compose, Activity, papel de assistente, sessão sobre outros apps, serviço de reconhecimento local, Gemini Live, chat, armazenamento local | Fonte inspecionada. O APK 1.7.7 precisa de CI e teste em aparelho. |
| FLUX Core | Worker com Durable Object, autenticação por aparelho, código de pareamento, chat, imagem e emissão de credencial Live | Bundle gerado em `wrangler deploy --dry-run`. A implantação e os segredos da Cloudflare não foram verificados. |
| Site | PWA estático servido como assets do Worker, chat, voz, notas locais, projetos e diagnóstico | Sintaxe JavaScript e bundle verificados. A versão publicada ainda precisa ser comparada com o repositório. |
| Distribuição | GitHub Actions produz um APK `release` | Sem keystore estável configurada no código, o build usa assinatura de debug; atualização sobre instalações antigas pode falhar. |

## Problemas encontrados e correções desta etapa

| Área | Evidência no código anterior | Correção em 1.7.7 | Validação pendente |
| --- | --- | --- | --- |
| Voz e wake word | A detecção pausava o reconhecimento antes de abrir a Activity e nem todos os caminhos reativavam o serviço. A UI marcava a voz como ativa antes do `setupComplete`. | Pausa enquanto a conversa possui o microfone, retoma ao encerrar ou falhar, distingue conexão de sessão aberta; palavra “Flux” exige correspondência isolada. | Uso em segundo plano, tela bloqueada, troca de rede, Bluetooth e fabricante do Android. |
| Compatibilidade da wake word | O serviço exige `SpeechRecognizer` local no Android 12 ou superior. A opção podia permanecer marcada sem suporte. | A Activity detecta indisponibilidade e desmarca a opção. | Conferir suporte e consumo de bateria no aparelho real. |
| Áudio | Falhas de inicialização, leitura e reprodução podiam deixar a sessão aparente ativa. | Falhas são tratadas com encerramento e mensagem compreensível. | Captura, latência, interrupção e reprodução reais. |
| Home | Núcleo estático e grande, indicadores baseados em configuração, contato fictício. | Núcleo com respiração e reação aos estados observados; conteúdo compacto e último contato vazio quando não há conversa. | Revisão visual, acessibilidade, animação reduzida e telas pequenas. |
| Chat e memória Android | Conversa do Core recebia identificador novo ao recriar o controlador; opção de memória não mudava o contexto de texto. | ID estável quando histórico está ligado; novo contexto por mensagem quando desligado; limpar conversa gira o ID. | Fluxo de longa duração e limpeza no servidor; memórias categorizadas ainda não existem no Android. |
| Conteúdo inicial | Tarefas e projetos de demonstração apareciam como se fossem do usuário. | Instalações novas começam vazias; dados já salvos não são apagados. | Migração de instalações antigas com exemplos já gravados. |
| Laboratório Android | Cards afirmavam instalar, testar e recuperar sem ação real. | Exibe apenas gerador de imagens e diagnósticos existentes, com instalação/recuperação declaradas indisponíveis. | Builder, Test Lab isolado, Update e Recovery nativos ainda não implementados. |
| Integrações | Apps externos eram rotulados como controles ou pareamento, mas só abrem intents/URLs. | Identificados como atalhos; TV e relógio não aparecem pareados. | OAuth, API de terceiros e controle de TV continuam pendentes. |
| Site e segurança | Notas privadas reveladas entravam no estado que `save()` podia gravar em texto aberto. PIN novo sobrescrevia o antigo, deixando notas cifradas inacessíveis. | Texto revelado fica em memória temporária; gravação remove plaintext; novo PIN usa verificador PBKDF2 e recifra notas com o PIN atual. | Inspeção de armazenamento em navegador real, migração de dados antigos e política de recuperação. |
| Site e estados | Integrações eram marcadas conectadas ao abrir um link; busca de dispositivos informava sucesso sem busca; `/health` podia ser servido pelo service worker fora de rede. | Rótulos de atalho, diagnóstico honesto e cache restrito aos assets estáticos. | Publicação e teste offline no navegador. |

## Capacidade atual e lacunas

- **Implementado em código:** chat via Core ou chave pessoal no Android, sessão Live com tentativas de retomada, reconhecimento local opcional, assistente do sistema, tarefas, projetos, imagem via Core, site PWA e notas locais cifradas com PIN.
- **Parcial:** a memória Android é histórico recente de chat; a memória do site é um caderno local e não alimenta automaticamente o modelo. O reconhecimento por “Flux” depende do serviço local do Android e de permissões; não há garantia de escuta permanente em todos os fabricantes.
- **Não implementado:** sincronização de memória categorizada entre aparelhos, contas OAuth dos atalhos, controle da TV, Builder que gera código, suíte isolada de agentes, atualização assinada e rollback no aplicativo.
- **Não validado:** chave Gemini e segredo no Worker publicado, resposta de áudio real, wake word com tela bloqueada, compatibilidade de certificado com APK instalado, desempenho e bateria.

## Portões de entrega

1. Compilar a branch na CI Android e verificar o APK e o checksum.
2. Configurar uma chave de assinatura estável; comparar certificado com o APK já instalado antes de prometer atualização sem reinstalação.
3. Publicar o Worker e os assets no projeto Cloudflare correto, sem colocar chaves no repositório; confirmar `/health`, `/v1/diagnostics`, chat, imagem e credencial Live com autorização.
4. Testar no telefone: primeira instalação, permissões, “Flux” com app aberto/fechado/tela bloqueada, conversa longa, silêncio, interrupção, reconexão, Bluetooth e gesto do assistente.
5. Testar o site publicado no Chromebook: pareamento, PIN, nota privada, recarga, restauração, microfone e PWA offline. Confirmar que o service worker não mascara falha do Core.
6. Revisar visualmente tamanhos de tela, contraste, foco, leitor de tela e movimento reduzido. Só depois declarar a versão pronta para produção.

**Classificação nesta data:** implementação em revisão; bundle do Worker compilado localmente; Android, implantação e aparelho pendentes. Não é uma entrega de produção validada.
