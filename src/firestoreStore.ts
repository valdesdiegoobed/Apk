import { collection, deleteDoc, doc, getDocs, orderBy, query, serverTimestamp, setDoc } from 'firebase/firestore';
import { auth, db } from './firebase';
import type { Expediente } from './data';

function coleccionUsuario() {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error('Usuario no autenticado');
  return collection(db, 'usuarios', uid, 'expedientes');
}

export async function listarExpedientes(): Promise<Expediente[]> {
  const snapshot = await getDocs(query(coleccionUsuario(), orderBy('ultimaActualizacion', 'desc')));
  return snapshot.docs.map((item) => item.data() as Expediente);
}

export async function guardarExpediente(expediente: Expediente) {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error('Usuario no autenticado');
  await setDoc(doc(db, 'usuarios', uid, 'expedientes', expediente.id), { ...expediente, actualizadoServidor: serverTimestamp() });
}

export async function eliminarExpediente(id: string) {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error('Usuario no autenticado');
  await deleteDoc(doc(db, 'usuarios', uid, 'expedientes', id));
}

export function generarFolio(expedientes: Expediente[]) {
  const numero = expedientes.reduce((maximo, expediente) => {
    const valor = Number(expediente.id.replace(/\D/g, ''));
    return Number.isFinite(valor) ? Math.max(maximo, valor) : maximo;
  }, 0) + 1;
  return `EXP-${String(numero).padStart(4, '0')}`;
}
