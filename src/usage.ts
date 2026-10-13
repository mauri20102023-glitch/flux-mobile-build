import { EvolutionError, readBoundedObject } from './evolution.ts';
type Store={get<T>(key:string):Promise<T|undefined>;put<T>(key:string,value:T):Promise<void>;transaction<T>(fn:(s:Store)=>Promise<T>):Promise<T>};
type Unit='textRequests'|'imageRequests'|'visionRequests'|'ttsCharacters'|'voiceOffers'|'searchRequests';
const LIMITS:Record<Unit,number>={textRequests:3000,imageRequests:300,visionRequests:1000,ttsCharacters:1200000,voiceOffers:180,searchRequests:300};
export class FluxUsage {
 constructor(private store:Store){}
 private month(){return new Date().toISOString().slice(0,7);}
 async reserve(unit:Unit,amount=1){
  if(!Number.isSafeInteger(amount)||amount<0)throw new EvolutionError(400,'Unidade de consumo inválida.');
  await this.store.transaction(async s=>{const key='usage:'+this.month();const rows=await s.get<Partial<Record<Unit,number>>>(key)||{};
   if((rows[unit]||0)+amount>LIMITS[unit])throw new EvolutionError(429,'Limite mensal de segurança desta rota atingido. Consulte Consumo e Billing antes de aumentar o uso.');
   rows[unit]=(rows[unit]||0)+amount;await s.put(key,rows);
  });
 }
 async route(request:Request):Promise<Response|null>{
  if(new URL(request.url).pathname!=='/v1/usage')return null;
  if(request.method==='POST'){
   const body=await readBoundedObject(request);const fx=Number(body.usdBrl),fees=Number(body.feesPercent);
   if(!Number.isFinite(fx)||fx<3||fx>15||!Number.isFinite(fees)||fees<0||fees>40)throw new EvolutionError(400,'Informe câmbio entre 3 e 15 e taxas totais entre 0 e 40%.');
   await this.store.put('usage:settings',{usdBrl:fx,feesPercent:fees});
  }else if(request.method!=='GET')throw new EvolutionError(405,'Método inválido.');
  const settings=await this.store.get<{usdBrl:number;feesPercent:number}>('usage:settings')||{usdBrl:5.0119,feesPercent:10};
  const counts=await this.store.get<Partial<Record<Unit,number>>>('usage:'+this.month())||{};
  // Planning scenario: half of 30h conversations is synthesized at 900 chars/min.
  const tts=1800*.5*900;const inworldEstimatedUseUsd=tts/1e6*10+30*.10+2*.15+.25*.60;
  const inworldFixedUsd=25,cloudflareReserveUsd=7,monthlyUsd=inworldFixedUsd+cloudflareReserveUsd;
  const estimateBrl=monthlyUsd*settings.usdBrl*(1+settings.feesPercent/100);
  return Response.json({month:this.month(),budgetBrl:200,settings,estimateBrl:Math.round(estimateBrl*100)/100,
   alert:estimateBrl>200?'ESTIMATIVA ACIMA DO ORÇAMENTO':estimateBrl>160?'ATENÇÃO: reserva próxima ao orçamento':'ESTIMATIVA DENTRO DO ORÇAMENTO',
   observedGatewayAttempts:counts,routeLimits:LIMITS,
   planning:{dailyConversationMinutes:60,days:30,assistantSpeakingFraction:.5,charactersPerSpeakingMinute:900,ttsCharacters:tts,inworldEstimatedUseUsd,inworldCreatorSubscriptionUsd:25,includedCreditUsd:25,cloudflareReserveUsd:7,braveSearchRequests:300,braveGrossUsd:1.5,braveIncludedMonthlyCreditUsd:5,braveNetPlanningUsd:0},
   hardCapGuaranteed:false,providerInvoiceAvailable:false,
   warning:'Contadores registram tentativas no Core, incluindo falhas. Não equivalem a fatura. WebRTC usa STT/TTS/LLM diretamente na Inworld, sem medição exata pelo Core. Os limites de rota não garantem teto de R$200. Desative auto-reload no provedor e confira Billing.',
   priceDate:'2026-10-08',sources:['https://inworld.ai/pricing','https://inworld.ai/models','https://brave.com/search/api/','https://developers.cloudflare.com/workers-ai/platform/pricing/','https://ptax.bcb.gov.br/ptax_internet/consultarTodasAsMoedas.do?method=consultaTodasMoedas']},{headers:{'cache-control':'no-store'}});
 }
}
