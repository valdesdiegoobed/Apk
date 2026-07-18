export type EstadoSolicitud = 'Pendiente' | 'Activa' | 'Realizada' | 'Vencida';

export type DocumentoCliente = {
  tipo: string;
  nombre: string;
  mime: string;
  dataUrl: string;
  fechaCarga: string;
};

export type Expediente = {
  id: string;
  cliente: string;
  fechaCreacion: string;
  foto?: DocumentoCliente;
  curp: string;
  contrasenaAfore: string;
  telefono: string;
  fechaInicio: string;
  fechaSolicitud: string;
  estadoSolicitud: EstadoSolicitud;
  notas: string;
  documentos: DocumentoCliente[];
  favorito?: boolean;
};

export const TIPOS_DOCUMENTO = [
  'Identificación frontal',
  'Identificación trasera',
  'Pagaré',
  'Estado de cuenta',
  'Constancia de situación fiscal',
] as const;

export function sumarDiasNaturales(fecha: string, dias: number) {
  if (!fecha) return '';
  const [ano, mes, dia] = fecha.split('-').map(Number);
  const valor = new Date(ano, mes - 1, dia);
  valor.setDate(valor.getDate() + dias);
  return `${valor.getFullYear()}-${String(valor.getMonth() + 1).padStart(2, '0')}-${String(valor.getDate()).padStart(2, '0')}`;
}

export function fechaHoy() {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

export function diferenciaDias(fecha: string, referencia = fechaHoy()) {
  if (!fecha) return Number.POSITIVE_INFINITY;
  const a = new Date(`${referencia}T00:00:00`);
  const b = new Date(`${fecha}T00:00:00`);
  return Math.round((b.getTime() - a.getTime()) / 86400000);
}

export function diaProceso(expediente: Expediente) {
  if (!expediente.fechaInicio) return 0;
  return Math.max(1, 1 - diferenciaDias(expediente.fechaInicio));
}

export function estadoCalculado(expediente: Expediente): EstadoSolicitud {
  if (expediente.estadoSolicitud === 'Realizada') return 'Realizada';
  const dias = diferenciaDias(expediente.fechaSolicitud);
  if (dias < 0) return 'Vencida';
  if (dias === 0) return 'Activa';
  return 'Pendiente';
}

export const expedientesDemo: Expediente[] = [];
