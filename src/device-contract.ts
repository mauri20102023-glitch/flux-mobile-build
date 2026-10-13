/** Prepared protocol only; no connected hardware or transport is inferred. */
export type DeviceKind='android'|'desktop'|'tv'|'watch'|'glasses'|'ambient';
export type Capability='display'|'audio'|'camera'|'spatial-tracking'|'notifications'|'health'|'media-control';
export interface DeviceDescriptor{id:string;kind:DeviceKind;capabilities:Capability[];verifiedAt?:string;}
export interface HandoffRequest{id:string;targetId:string;capability:Capability;expiresAt:string;requiresConfirmation:boolean;}
export function validateHandoff(device:DeviceDescriptor,request:HandoffRequest,now=Date.now()):void {
 if(request.targetId!==device.id)throw new Error('Dispositivo de destino divergente.');
 if(!Number.isFinite(Date.parse(request.expiresAt))||Date.parse(request.expiresAt)<=now)throw new Error('Pedido expirado.');
 if(!device.verifiedAt||!Number.isFinite(Date.parse(device.verifiedAt)))throw new Error('Dispositivo ainda não verificado.');
 if(!device.capabilities.includes(request.capability))throw new Error('Capacidade não demonstrada pelo dispositivo.');
}
