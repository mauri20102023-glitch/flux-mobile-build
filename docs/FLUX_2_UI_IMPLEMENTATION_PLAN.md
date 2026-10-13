# FLUX 2.0 — Mobile + Desktop (proposta de implementação)
> Status: PLANEJAMENTO. Não representa funcionalidade implementada ou testada.

## Decisão de produto
Um único FLUX Core existente e duas experiências de interface: app Android nativo e Command Center Web/PWA para desktop/Chromebook. Não refazer chat, roteador de IA, voz, credenciais ou pareamento sem uma falha comprovada.

## Base revisada no GitHub (out/2026)
- `src/index.ts`: Cloudflare Worker e FLUX Core com instruções da IA, modalidades e provedores.
- `wrangler.jsonc`: Assets do Web/PWA e Worker no mesmo deploy.
- `web/flux-ath/index.html`: navegação Home, Chat, Planner, Studio, Memory, Integrations, Devices, Lab, Settings.
- `web/flux-ath/app.js`: rotas de UI, estado local no navegador, funções de voz e interface; consta versão 1.8.1.
- `web/flux-ath/styles.css`: múltiplos temas; tema inicial Ion roxo. O conceito aprovado é grafite/vermelho, então precisa ser reconciliado com as preferências atuais.
- `apps/android/app/src/main/java/ai/flux/mobile/MainActivity.kt`: fluxo Android de assistente, permissões, voz e Vision.
- `apps/android/app/build.gradle.kts`: Android Compose, versionName 1.8.0.
- O README exibe FLUX 1.7, desatualizado em relação a parte do código.
- A área de memória Web declara notas locais que o chat não usa automaticamente; não prometer memória sincronizada sem implementar e testar.

## Identidade visual FLUX / v2
- Uso: sistema de trabalho real, não mockup de IA.
- Cores de referência (a validar com protótipos acessíveis): fundo #0B0D12, superfície #171A21, texto #F3F4F6, secundário #A4ABB8, identidade #E84761; ciano apenas para informação técnica contextual.
- Evitar: humanoide azul genérico, grades decorativas, hologramas falsos, telemetria inventada, animação infinita que consome recursos, excesso de glow.
- Linguagem: tipografia limpa, hierarquia nítida, formas características inspiradas no F do FLUX, ícones coerentes, estados e ações verificáveis.
- Movimento: despertar e mudança de modo podem ser cinematográficos; navegação e comandos precisam ser rápidos. Respeitar Reduce Motion e aparelhos de menor desempenho.
- Manter modo escuro como padrão; permitir contraste acessível.

## FLUX Mobile — entregas do Projeto 1
1. Home compacta: comando por voz, resumo do dia fundamentado em dados disponíveis, ações recentes e módulos prioritários.
2. Conversa: leitura confortável, arquivos e ações mostrados apenas quando suportados, indicador de chamada de voz real.
3. Vision: permissão explícita e indicação clara de captura de tela/câmera.
4. Study: aulas por texto/voz e exercícios guiados (HoloClass 3D fica como evolução, não simular um produto entregue).
5. Control: status real dos dispositivos, estados 'não conectado' e 'requer configuração'.
6. Settings: permissões, diagnóstico, privacidade, provedor de voz, identidade visual.
7. Testes obrigatórios: wake word somente onde Android permitir, interrupção de voz, rotação/estado de app, permissões negadas, offline, aparelho lento.

## FLUX Desktop Command Center — entregas do Projeto 1
1. Aplicação responsiva sobre a Web/PWA existente; não duplicar o backend.
2. Layout: navegação lateral concisa; área principal contextual; painéis laterais recolhíveis apenas com informação útil.
3. Estados: operacional, aguardando configuração, offline, ação em curso, autorização e falha — sem dados fictícios.
4. Modos: Normal (trabalho), Imersivo (voz/estudo), Study (lousa e conteúdo), Vision (compartilhamento autorizado).
5. Navegação por teclado, zoom, telas pequenas e responsividade; performance antes de efeitos especiais.
6. Compatibilidade TV: exibição básica por navegador/espelhamento; integração de controle só após suporte real do dispositivo.

## Arquitetura e limites
- Um único Core API e contratos versionados; clientes Mobile e Desktop usam mesma identidade quando autenticados.
- Não mover chaves para o navegador ou APK. Proteger endpoints, armazenamento local e pareamento; comandos sensíveis sempre confirmados.
- Navegadores exigem gesto de usuário para microfone/compartilhamento; promessa de wake word sempre ativo é somente para plataforma Android quando suportada.
- Autonomia: modo de estudo que ensina sem resolver automaticamente tudo; dados exportáveis e controles manuais; IA sugere, usuário decide.

## Execução em incrementos revisáveis
- Marco 0: rodar testes já existentes, confirmar build Android, inventariar componentes e capturar imagens da UI real.
- Marco 1: design tokens + tela Home desktop e estados, sem modificar endpoints.
- Marco 2: tela Mobile em Compose alinhada aos tokens, preservando navegação e permissões.
- Marco 3: Chat/Voice/Vision e componentes contextuais nas duas interfaces.
- Marco 4: Study inicial, integração entre clientes, acessibilidade e regressões.
- Marco 5: testes em dispositivos reais; atualizar README conforme comportamento verificado.

## Critérios de aceite
- Nenhuma regressão comprovada em chat, voz, pareamento ou Vision.
- Nenhum botão sem ação verdadeira: desativar ou exibir status 'em desenvolvimento'.
- Nenhuma informação de sensor, agenda ou dispositivo inventada.
- Comparação antes/depois por screenshots reais.
- Testes automatizados + manual mínimo para Android e Chrome/Chromebook.
- Alterações em branch/PR, não direto em produção.

## Próximo passo de engenharia
Começar pela auditoria real de execução e screenshots; depois implementar o novo layout Web em uma branch própria e submeter para revisão. Este documento é a especificação inicial, não a implementação.
