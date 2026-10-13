# FLUX — Projeto A: Mobile e Command Center

Clientes **2.0.1-project-a**, desenvolvidos sobre o preview 2.0 do PR #16. O Core existente é preservado. `main` e produção continuam em 1.8 até revisão/aprovação. O Projeto B Advanced é futuro, sem compras.

## Experiências

- Android: Home compacta, Chat, Vision, Study e Control. Creator, memória, cofre, projetos, integrações e ajustes permanecem em Control/Workspace.
- Desktop/Chromebook: Web/PWA Command Center, navegação lateral, contexto recolhível/redimensionável, modos Normal/Command/Imersivo/Study/Vision, atalhos e redução de movimento.
- Study: lousa determinística, explicações pela API existente, incentivo ao raciocínio, histórico Web local mediante consentimento e exportação. Professor 3D e avaliação adaptativa permanecem em desenvolvimento.

## Arquitetura e proteção

`wrangler.command.jsonc` hospeda somente a interface Desktop e utiliza service binding para `flux-evolution-preview`. Nenhuma cópia de inteligência, banco ou chave de serviço é criada para o Desktop. O Android continua usando esse Core remoto, mesmo com o PC desligado. A produção `flux-mobile-build2` não é substituída.

A voz personalizada FLUX 4 e os clientes existentes são preservados. Disponibilidade exige teste real; configuração não é evidência de operação. Tokens de aparelhos ficam separados da credencial administrativa. API keys permanecem no servidor. Não incluir segredos no APK, scripts, logs, commits ou exportações.

Navegador exige gesto/permissão para microfone/captura. Vision analisa capturas individuais; não equivale a vídeo contínuo. Wake word Android depende de suporte/permissões e teste físico. OAuth exige cadastro e autorização próprios do FLUX; conexão dos serviços ao Codex não autoriza o aplicativo automaticamente.

## Desenvolvimento

```sh
npm ci
npm run embed:web
npm test
npm run check:types
npx wrangler deploy --config wrangler.command.jsonc --dry-run
npm run test:web
```

Os testes Web usam Chromium no CI com fixture explicitamente simulada, sem chamadas pagas. Não comprovam a execução de provedores/contas. CI compila APKs e executa emulador Android. Build Android em `apps/android` requer Java 17; o workflow gera um pacote separado para preservar a instalação atual. Não usar `npm run deploy` em produção sem validação e aprovação.

Para parear, emita convite administrativo pelo Core de avaliação, uso único por 15 minutos. Cada cliente usa convite próprio. A interface recebe apenas token do aparelho, nunca a chave administrativa. Segredos do serviço devem ser configurados no painel seguro do Cloudflare.

## Documentação

- [Auditoria atual](docs/project-a/AUDITORIA.md)
- [Design System e limites do Study](docs/project-a/DESIGN-SYSTEM.md)
- [Advanced: compatibilidade, orçamento e critérios](docs/project-a/ADVANCED.md)
- Evidências históricas e configuração do preview anterior em `docs/evolution`; essas evidências não equivalem a testes físicos atuais.

Não houve compra, assinatura, recarga ou aquisição de equipamento. O teto de R$ 12.000 é planejamento de hardware/montagem; não garante preços nem inclui automaticamente APIs/licenças recorrentes.
