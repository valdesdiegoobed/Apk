import { BackupRounded, DownloadRounded, RestoreRounded } from '@mui/icons-material';
import { Alert, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useRef, useState } from 'react';
import { cargarExpedientes, guardarExpedientes } from '../expedientesStore';

export function Component() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [mensaje, setMensaje] = useState('');
  const respaldar = () => {
    const data = JSON.stringify({ version: 1, createdAt: new Date().toISOString(), expedientes: cargarExpedientes() }, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob); const a = document.createElement('a');
    a.href = url; a.download = `Nuevo-Proyecto-Android-Respaldo-${new Date().toISOString().slice(0, 10)}.json`; a.click(); URL.revokeObjectURL(url);
    localStorage.setItem('nuevoProyectoAutoBackup', data); setMensaje('Respaldo creado correctamente');
  };
  const restaurarArchivo = async (file?: File) => {
    if (!file) return;
    try { const json = JSON.parse(await file.text()); if (!Array.isArray(json.expedientes)) throw new Error(); guardarExpedientes(json.expedientes); localStorage.setItem('nuevoProyectoAutoBackup', JSON.stringify(json)); setMensaje(`Se restauraron ${json.expedientes.length} expedientes`); }
    catch { setMensaje('El archivo de respaldo no es válido'); }
  };
  const restaurarAutomatico = () => {
    try { const raw = localStorage.getItem('nuevoProyectoAutoBackup'); if (!raw) return setMensaje('No existe una copia automática'); const json = JSON.parse(raw); guardarExpedientes(json.expedientes || []); setMensaje('Copia automática restaurada'); }
    catch { setMensaje('No se pudo restaurar la copia automática'); }
  };
  return <Stack spacing={3}>
    <Stack direction="row" spacing={1.5} alignItems="center"><BackupRounded color="primary"/><Typography variant="h4" fontWeight={800}>Respaldos</Typography></Stack>
    <Alert severity="info">El respaldo incluye la información de los expedientes. Los archivos adjuntos se incorporarán en la siguiente fase de almacenamiento.</Alert>
    {mensaje && <Alert severity={mensaje.includes('no ') || mensaje.includes('válido') ? 'warning' : 'success'}>{mensaje}</Alert>}
    <Card><CardContent><Stack spacing={2}><Typography variant="h6" fontWeight={750}>Administrar copias</Typography><Button variant="contained" startIcon={<DownloadRounded/>} onClick={respaldar}>Respaldar ahora</Button><Button variant="outlined" startIcon={<RestoreRounded/>} onClick={() => inputRef.current?.click()}>Restaurar respaldo</Button><Button variant="outlined" startIcon={<RestoreRounded/>} onClick={restaurarAutomatico}>Restaurar automáticamente</Button><input ref={inputRef} hidden type="file" accept="application/json,.json" onChange={(e) => void restaurarArchivo(e.target.files?.[0])}/></Stack></CardContent></Card>
  </Stack>;
}
