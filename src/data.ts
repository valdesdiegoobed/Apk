export type Expediente = { id: string; cliente: string; estado: 'Activo'|'En revisión'|'Archivado'; categoria: string; ultimaActualizacion: string; responsable: string; documentos: number; notas: string };
export const expedientesDemo: Expediente[] = [
 {id:'EXP-0001', cliente:'Cliente Ficticio Aurora', estado:'Activo', categoria:'Civil', ultimaActualizacion:'2026-07-10', responsable:'Equipo Legal A', documentos:4, notas:'Pendiente validar documento de identidad ficticio.'},
 {id:'EXP-0002', cliente:'Empresa Demo Norte', estado:'En revisión', categoria:'Mercantil', ultimaActualizacion:'2026-07-12', responsable:'Equipo Legal B', documentos:7, notas:'Revisión interna de contrato simulado.'},
 {id:'EXP-0003', cliente:'Persona de Prueba Central', estado:'Archivado', categoria:'Administrativo', ultimaActualizacion:'2026-06-28', responsable:'Archivo', documentos:3, notas:'Expediente cerrado con datos inventados.'},
];
