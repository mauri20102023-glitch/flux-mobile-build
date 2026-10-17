# FLUX VOICE 01 — Instrução ao Work

Objetivo: identidade vocal masculina original brasileira, estilo JARVIS Elegante, sem aluguel de voz. O usuário aprovou o **estilo Algieba** e sua edição Elegante, **não adquiriu os pesos Google Chirp nem os direitos sobre um timbre específico**.

- Não utilizar gravações da Algieba, trechos de filmes ou voz de Paul Bettany para treinar/clone. Selecionar locutor adulto brasileiro próprio/autorizado expressamente; guardar consentimento.
- Kit experimental: `tools/flux-voice-01/studio.py`, `test_studio.py` e `README.md`. O código cadastra a referência e tenta usar o modelo regional `ResembleAI/Chatterbox-Multilingual-pt-br` (MIT) com carregamento estrito. Primeiro testar pesos reais na infraestrutura compatível, depois 10 frases novas com sotaque pt-BR.
- **Não afirmar motor pronto:** CI cobre testes sem GPU, não inferência real com pesos multi-GB. Verificar compatibilidade entre versão do pacote e checkpoints T3/S3Gen.
- Requisito final Android: funcionar com computador desligado, baixa latência, sem mensalidade. Modelo regional de vários GB **não foi portado ao Android**. Medir antes de prometer. Serviço na nuvem ou API comercial requer aprovação separada, inclusive custo e dados.
- Orçamento inicial **R$0**, teto futuro de **R$50 em despesa única aprovada**, sem assinatura.
- Preparar amostras usando somente referência autorizada, testes e PR sem deploy/merge em produção.

Critério de aceite: locutor/consentimento válidos; voz pt-BR de 10 frases avaliadas pelo usuário; reprodução em novos textos; consumo e latência medidos no aparelho; sem fallback pago nem alterações em produção.
