export type EstadoExpediente = 'Pendiente' | 'Activa' | 'Realizada' | 'Vencida';

export type DocumentoCliente = {
  tipo: string;
  nombre: string;
  mime?: string;
  dataUrl?: string;
  fechaCarga?: string;
};

export type Expediente = {
  id: string;
  cliente: string;
  estado: EstadoExpediente;
  categoria: string;
  ultimaActualizacion: string;
  fechaCreacion?: string;
  responsable: string;
  documentos: number;
  notas: string;
  curp?: string;
  rfc?: string;
  telefono?: string;
  contrasenaAfore?: string;
  fechaInicio?: string;
  fechaSolicitud?: string;
  fechaCulminacion?: string;
  fotoNombre?: string;
  fotoDataUrl?: string;
  archivos?: DocumentoCliente[];
  favorito?: boolean;
};

export const expedientesDemo: Expediente[] = [];
