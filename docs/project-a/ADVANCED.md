# FLUX Advanced — preparação técnica, sem aquisições

Projeto B permanece futuro. A Mauright tem prioridade; nenhuma verba foi tratada como disponível nem houve compra/assinatura. O PC é sede visual, não servidor obrigatório: Core remoto funciona com o PC desligado. Android concentra permissões locais; não haverá Hub dedicado.

## Arquitetura e sequência

1. Agora: clientes Web/Android, conta e Core existentes. Módulos de interface não duplicam inteligência.
2. TV existente: obter código do modelo/ano/região. Testar navegador/espelhamento e capacidades reais SmartThings. HTTP de comando não comprova vídeo exibido. Tizen exige ferramentas/permissões do modelo e teste no televisor; não anunciado compatível antes disso.
3. Wear OS: companion Kotlin/Compose for Wear OS, superfícies rápidas (tile/notificação/comando), Data Layer com Android quando compatível e cliente HTTPS pareado. Não copiar chaves de serviço para o relógio. Confirmar versão do SDK, distribuição, bateria, reconexão e modelo antes da aquisição. Saúde via APIs autorizadas/Health Connect quando houver fonte; não presumir que qualquer relógio sincroniza sono/atividade.
4. Glasses: contrato de capacidades independente de marca. Display virtual usa vídeo/áudio e não implica câmera, câmera não implica rastreamento6DoF. Para visão contextual, exigir API de câmera, consentimento e teste; para AR espacial exigir sensores/SDK espacial. Sem SDK demonstrado, limitar a espelhamento/áudio permitido.
5. Ambient: adaptadores autorizados, cenas explícitas, confirmação de operações sensíveis, botão físico de parada/privacidade e controle manual sempre disponível. Não transformar descoberta de sensor em permissão para agir.
6. Handoff futuro: pedido identificável com prazo, consentimento, ACK de recebimento, resultado e falha/timeout. Um link aberto ou mensagem enviada não é confirmação de execução. `src/device-contract.ts` prepara o contrato, não o transporte.

## Command Room

Mesa190×85cm é referência; medir quarto, passagem, cadeira e montagem antes de encomendar. Computador existente pode iniciar. Monitor principal/auxiliar independentes; touchscreen pode exigir vídeo+USB para toque, drivers e alimentação separados. Suportes exigem peso e VESA; proteger cabos e ventilação. Microfone/câmera com desligamento físico; controles programáveis com ações revisáveis; presença por sensor com consentimento. Proteção elétrica dimensionada por profissional conforme carga/rede, sem prometer estabilidade só por usar filtro.

| Categoria | Teto planejado (não cotação) |
|---|---:|
| Computador | R$ 4.600 |
| Monitor principal | R$ 1.500 |
| Painel touch | R$600 |
| Smartwatch | R$700 |
| Óculos | R$ 1.600 |
| Mesa/suportes | R$700 |
| Voz/câmera | R$450 |
| Luz/sensores | R$250 |
| Cabos/energia/adaptadores | R$700 |
| Reserva | R$900 |
| Total | R$ 12.000 |

Sem produtos/modelos recomendados nesta etapa: preços, compatibilidade, frete, tributos e garantia precisam ser confirmados quando houver decisão de compra. Reserva não comprometida; APIs/licenças/energia/internet recorrentes ficam fora do envelope de equipamentos. Se valores reais excederem o teto, escalonar recursos em vez de prometer todo hardware.

## Gates de teste

Watch: comando recebe ACK no Android/Core, notificações revogáveis, timer sem rede, ensaio de autonomia. Glasses: demonstrar display/áudio antes de câmera/AR, proteção de dados e parada imediata. TV: abrir aula conhecida, confirmar vídeo/áudio e reprodução fisicamente. Ambient: comando manual, autorização negada, perda de rede e retorno seguro. Nenhum desses testes foi feito no hardware do usuário.

Fontes oficiais consultadas13/10/2026:
- https://developer.android.com/training/wearables/apps
- https://developer.android.com/health-and-fitness/health-connect
- https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html
