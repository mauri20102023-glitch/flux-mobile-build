# Custos recorrentes — verificação de 13/10/2026

O teto pessoal planejado continua R$ 200/mês. O orçamento Advanced de R$ 12.000 é separado e não foi comprometido. Não houve compra, recarga ou assinatura nesta entrega. Cobranças anunciadas abaixo são em dólares americanos, antes dos tributos/câmbio efetivos do proprietário.

| Serviço | Preço público confirmado | Aplicação no FLUX |
|---|---|---|
| Inworld On-Demand | TTS2 Flash US$ 15/milhão de caracteres; STT1 US$ 0,15/h | Possível uso por créditos sem assumir assinatura Creator; saldo/voz precisam ser confirmados na conta |
| Inworld Creator | US$ 25/mês, US$ 25 em créditos; Flash US$ 10/milhão de caracteres; STT1 US$ 0,10/h | Alternativa com mensalidade; créditos também usados por outros produtos. Não contratado |
| Cloudflare Workers AI | 10.000 neurônios/dia gratuitos; Paid excedente US$ 0,011/1.000 neurônios | Texto, visão e imagem existentes. Modelos e frequência alteram consumo |
| Cloudflare Workers | Paid mínimo US$ 5/mês por conta, com excedentes por uso | O novo Desktop não exige comprar um computador/plano novo; usa conta/Core existentes |
| Brave Search | US$ 5/1.000 requisições, anúncio de US$ 5 em crédito mensal | 300 consultas seriam US$1,50 bruto; crédito líquido depende de cadastro e disponibilidade na conta |

Projeção ilustrativa para 1h/dia por 30 dias: assistente falando metade da sessão a 1.000 caracteres/min gera 900 mil caracteres. Flash: US$ 13,50 On-Demand ou US$ 9 nos créditos Creator. STT por 30h: US$ 4,50 On-Demand ou US$ 3 Creator. Total medido desses dois produtos: US$ 18 On-Demand ou US$ 12 em créditos Creator. LLM, visão, imagens e outros usos são adicionais; a assinatura Creator cobra US$ 25 mesmo usando sóUS$ 12. Android pode usar reconhecimento do sistema em alguns fluxos; não foi presumida gratuidade de todo áudio.

Cenário Creator 25 + reserva Cloudflare 7 = US$ 32. Com câmbio hipotético do cartão de R$ 5,20/USD e margem hipotética de 10%, resulta R$ 183,04. **R$ 5,20 e 10% são hipóteses de planejamento, não PTAX atual nem alíquota de imposto.** A US$ 32, esse cenário só fica abaixo de R$ 200 se câmbio×(1+margem) for inferior a 6,25. Reserva Cloudflare 7 não é pacote ilimitado e pode ser excedida. On-Demand pode ser mais econômico em uso menor, mas depende dos modelos e das políticas de créditos da conta.

O painel Consumo existente preserva câmbio/reserva, contadores e alertas. Não é conciliação da fatura nem trava financeira transversal: WebRTC/Inworld e consumo fora do FLUX não são integralmente medidos pelo Core. Auto-reload, excedentes, outras aplicações, impostos e câmbio do cartão podem ultrapassar R$ 200. Conferir Billing de cada conta antes de autorizar qualquer contratação. O teste atual de FLUX 4 retornou HTTP 402; nenhuma franquia gratuita foi presumida ainda disponível.

Fontes primárias, consultadas13/10/2026:
- https://inworld.ai/pricing
- https://developers.cloudflare.com/workers-ai/platform/pricing/
- https://developers.cloudflare.com/workers/platform/pricing/
- https://brave.com/search/api/
