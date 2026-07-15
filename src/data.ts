export type EstadoExpediente = 'Activo' | 'En revisión' | 'Archivado';

export type Expediente = {
  id: string;
  cliente: string;
  estado: EstadoExpediente;
  categoria: string;
  ultimaActualizacion: string;
  responsable: string;
  documentos: number;
  notas: string;
  curp?: string;
  rfc?: string;
  telefono?: string;
  fechaInicio?: string;
  fechaSolicitud?: string;
  fechaCulminacion?: string;
};

export const expedientesDemo: Expediente[] = [
  { id: 'EXP-0001', cliente: 'Cliente Ficticio Aurora', estado: 'Activo', categoria: 'Ayuda por desempleo', ultimaActualizacion: '2026-07-10', responsable: 'Asesoría', documentos: 4, notas: 'Pendiente validar documento de identidad ficticio.', curp: 'AAAA000101HCLXXX01', rfc: 'AAAA000101AA1', telefono: '8660000001', fechaInicio: '2026-07-01', fechaSolicitud: '2026-07-20', fechaCulminacion: '2026-07-23' },
  { id: 'EXP-0002', cliente: 'Persona Demo Norte', estado: 'En revisión', categoria: 'Ayuda por desempleo', ultimaActualizacion: '2026-07-12', responsable: 'Asesoría', documentos: 7, notas: 'Revisión interna con información simulada.', curp: 'BBBB000202MCLXXX02', rfc: 'BBBB000202BB2', telefono: '8660000002' },
  { id: 'EXP-0003', cliente: 'Persona de Prueba Central', estado: 'Archivado', categoria: 'Ayuda por desempleo', ultimaActualizacion: '2026-06-28', responsable: 'Archivo', documentos: 3, notas: 'Expediente cerrado con datos inventados.', curp: 'CCCC000303HCLXXX03', rfc: 'CCCC000303CC3', telefono: '8660000003' },
];
