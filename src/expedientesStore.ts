import { expedientesDemo, type Expediente } from './data';

const STORAGE_KEY = 'expedientes-diego-obed-v1';

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
  localStorage.setItem(`${STORAGE_KEY}-ultimo-respaldo`, new Date().toISOString());
  window.dispatchEvent(new Event('expedientes-actualizados'));
}

export function generarFolio(expedientes: Expediente[]) {
  const numero = expedientes.reduce((maximo, expediente) => {
    const valor = Number(expediente.id.replace(/\D/g, ''));
    return Number.isFinite(valor) ? Math.max(maximo, valor) : maximo;
  }, 0) + 1;
  return `EXP-${String(numero).padStart(4, '0')}`;
}

export function ultimoRespaldo() {
  return localStorage.getItem(`${STORAGE_KEY}-ultimo-respaldo`);
}

export function descargarRespaldo(expedientes: Expediente[]) {
  const blob = new Blob([JSON.stringify({ version: 1, fecha: new Date().toISOString(), expedientes }, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `respaldo-expedientes-${new Date().toISOString().slice(0, 10)}.json`;
  enlace.click();
  URL.revokeObjectURL(url);
}

export async function leerRespaldo(archivo: File): Promise<Expediente[]> {
  const contenido = await archivo.text();
  const datos = JSON.parse(contenido) as { expedientes?: Expediente[] };
  if (!Array.isArray(datos.expedientes)) throw new Error('Respaldo incompatible');
  return datos.expedientes;
}
