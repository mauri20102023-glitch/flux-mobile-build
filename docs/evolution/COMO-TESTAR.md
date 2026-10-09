# FLUX Evolution — primeira entrega de avaliação

1. Instale o APK FLUX Evolution lado a lado com o FLUX atual. Não desinstale o aplicativo estável.
2. O endereço do preview já vem no APK: https://flux-evolution-preview.mauri20102023.workers.dev . Em Configurações, pareie com um código novo de 32 caracteres; cada código vale uma vez por 15 minutos. Código do Core antigo não vale neste preview. Navegador e Android exigem códigos distintos.
3. No Chromebook, abra o site preview HTTPS e use Ajustes para parear. Os novos recursos ficam em Laboratório. Não altere o endereço do aplicativo estável para avaliar.
4. No Laboratório, autorize uma memória, faça uma pergunta sobre ela no Chat, corrija e esqueça. Inicie uma missão de estudo; atualize resultados para ver andamento registrado e texto final. Uma missão completed não comprova execução de ações externas.
5. Vision: envie uma imagem ou autorize captura/foto no navegador compatível. A resposta deve refletir a imagem real; se não houver captura/permissão, deve aparecer erro, nunca percepção inventada.
6. Android Vision: selecione FLUX Evolution como assistente, conceda microfone quando solicitado e ative Vision. Android pode negar capturas de telas protegidas. MediaProjection fallback ainda não está implementado.
7. Voz FLUX 4: está bloqueada por créditos Inworld (HTTP 402). Abra https://platform.inworld.ai/ , confira Billing/créditos da mesma conta e resolva a pendência por conta própria. Não envie chave ou senha no chat. Verifique pelo botão testar voz e por uma conversa real.
8. Após voz liberada: peça uma resposta longa; interrompa com "Flux, pare e explique mais curto"; repita três vezes. Confira se áudio anterior para, sua frase inteira é capturada e o contexto permanece. Teste sem fone e com Bluetooth. Essa confirmação é necessária antes de promover a atualização.

Google/Spotify/TV/relógio/ClassApp/Geekie One ainda precisam de conectores, modelos/contas e autorizações. Não são serviços prontos nesta entrega. Cofre e biometria ainda não existem. Não inserir credenciais em memórias.

Para parar a avaliação, feche/desinstale FLUX Evolution e selecione novamente seu assistente anterior. Seu FLUX estável e Core original não foram substituídos. Revisão está em PR #16; não fazer merge/deploy de produção sem validar os itens pendentes.


## Incremento Android 1.9.1-preview — captura autorizada

No botão Vision, escolha “Capturar tela com autorização do Android”. Autorize a tela ou o aplicativo no diálogo oficial. Abra o conteúdo desejado e, na notificação FLUX Vision, toque em “Analisar agora”. Uma captura JPEG é enviada ao Core e ao provedor configurado; a projeção é encerrada antes da requisição de análise. Abra o resultado pela notificação. “Encerrar” cancela a sessão e qualquer análise pendente. A espera pela captura expira após 60 segundos.

Esta opção não depende da conexão de voz Inworld. Não grava vídeo, áudio ou capturas em arquivos; mantém o resultado apenas na memória do processo até sua leitura. Se o Android encerrar o processo, o resultado pode ser perdido. Telas protegidas podem retornar vazias; não há tentativa de contornar a proteção. Não é Vision Live contínuo.

Validação física pendente: aceitar e negar consentimento; selecionar app e tela inteira; analisar texto e imagem conhecidos; cancelar pela notificação e pelo Android; aguardar expiração; testar orientação/tela protegida; confirmar que o indicador de compartilhamento encerra antes do envio. Só após estes testes a captura nativa poderá receber estado FUNCIONANDO.

Também foram isolados resultados tardios de ferramentas/visão entre sessões de voz, evitando que uma consulta anterior responda em uma sessão nova.
