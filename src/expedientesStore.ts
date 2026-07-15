import { expedientesDemo, type Expediente } from './data';

const STORAGE_KEY = 'control-expedientes-demo-v1';

export function cargarExpedientes(): Expediente[] {
  try {
    const guardados = localStorage.getItem(STORAGE_KEY);
    return guardados ? JSON.parse(guardados) as Expediente[] : expedientesDemo;
  } catch {
    return expedientesDemo;
  }
}

export function guardarExpedientes(expedientes: Expediente[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(expedientes));
  window.dispatchEvent(new Event('expedientes-actualizados'));
}

export function generarFolio(expedientes: Expediente[]) {
  const numero = expedientes.reduce((maximo, expediente) => {
    const valor = Number(expediente.id.replace(/\D/g, ''));
    return Number.isFinite(valor) ? Math.max(maximo, valor) : maximo;
  }, 0) + 1;
  return `EXP-${String(numero).padStart(4, '0')}`;
}
