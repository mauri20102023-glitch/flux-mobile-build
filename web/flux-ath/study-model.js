// Deterministic board values. No evaluated strings or fabricated AI output.
export function quadratic(x,a,b,c){return a*x*x+b*x+c;}
export function reactionEnergy(t,activation,delta,catalyst=false){const peak=activation*(catalyst?.55:1);return t<=.5?peak*Math.sin(Math.PI*t)**2:peak+(delta-peak)*Math.sin(Math.PI*(t-.5))**2;}
export function position(t,velocity,start){return start+velocity*t;}
export function samples(kind,params){return Array.from({length:81},(_,i)=>{const t=i/80;if(kind==='energy')return [t,reactionEnergy(t,params.a,params.c,params.b===1)];if(kind==='motion')return [t*10,position(t*10,params.a,params.c)];const x=-4+t*8;return [x,quadratic(x,params.a,params.b,params.c)];});}
export function safeStudyExport(history){return {schema:'flux-study-v1',exportedAt:new Date().toISOString(),records:history.map(({subject,topic,action,answer,at})=>({subject,topic,action,answer,at}))};}
