import {
  CallRounded, ContentCopyRounded, Delete, EditRounded, PictureAsPdfRounded,
  Save, ShareRounded, WhatsApp,
} from '@mui/icons-material';
import {
  Alert, Box, Button, Card, CardContent, MenuItem, Snackbar, Stack, Tab, Tabs,
  TextField, Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { EstadoExpediente, Expediente } from '../data';
import { cargarExpedientes, guardarExpedientes } from '../expedientesStore';

const digits = (value = '') => value.replace(/\D/g, '');

export function Component() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lista, setLista] = useState<Expediente[]>(cargarExpedientes);
  const encontrado = lista.find((item) => item.id === id);
  const [expediente, setExpediente] = useState<Expediente | undefined>(encontrado);
  const [editando, setEditando] = useState(false);
  const [tab, setTab] = useState(0);
  const [mensaje, setMensaje] = useState('');

  if (!expediente) return <Stack spacing={2}><Alert severity="warning">El expediente no existe en este dispositivo.</Alert><Button component={Link} to="/expedientes">Volver</Button></Stack>;

  const cambiar = (campo: keyof Expediente, valor: string) => setExpediente({ ...expediente, [campo]: valor });
  const guardar = () => {
    const actualizado = { ...expediente, cliente: expediente.cliente.trim(), curp: expediente.curp?.toUpperCase(), rfc: expediente.rfc?.toUpperCase(), ultimaActualizacion: new Date().toISOString().slice(0, 10) };
    const nuevos = lista.map((item) => item.id === actualizado.id ? actualizado : item);
    setLista(nuevos); setExpediente(actualizado); guardarExpedientes(nuevos); setEditando(false); setMensaje('Cambios guardados correctamente');
  };
  const eliminar = () => {
    if (!confirm('¿Eliminar definitivamente este expediente?')) return;
    guardarExpedientes(lista.filter((item) => item.id !== expediente.id)); navigate('/expedientes');
  };
  const copiarCurp = async () => {
    if (!expediente.curp) return setMensaje('Este expediente no tiene CURP');
    await navigator.clipboard.writeText(expediente.curp); setMensaje('CURP copiada');
  };
  const llamar = () => {
    const telefono = digits(expediente.telefono);
    if (!telefono) return setMensaje('Este expediente no tiene teléfono');
    window.location.href = `tel:${telefono}`;
  };
  const whatsapp = () => {
    const telefono = digits(expediente.telefono);
    if (!telefono) return setMensaje('Este expediente no tiene teléfono');
    const texto = encodeURIComponent(`Hola ${expediente.cliente}. Me comunico respecto a tu expediente ${expediente.id}.`);
    window.open(`https://wa.me/52${telefono.replace(/^52/, '')}?text=${texto}`, '_blank');
  };
  const compartir = async () => {
    const text = `${expediente.cliente}\nFolio: ${expediente.id}\nCURP: ${expediente.curp || 'Pendiente'}\nTeléfono: ${expediente.telefono || 'Pendiente'}\nSolicitud: ${expediente.fechaSolicitud || 'Pendiente'}`;
    if (navigator.share) await navigator.share({ title: `Expediente ${expediente.id}`, text });
    else { await navigator.clipboard.writeText(text); setMensaje('Resumen copiado para compartir'); }
  };

  const field = (label: string, key: keyof Expediente, options?: { type?: string; multiline?: boolean }) => <TextField label={label} type={options?.type || 'text'} multiline={options?.multiline} minRows={options?.multiline ? 5 : undefined} InputLabelProps={options?.type === 'date' ? { shrink: true } : undefined} value={String(expediente[key] || '')} disabled={!editando} onChange={(e) => cambiar(key, e.target.value)} />;

  return <Stack spacing={3}>
    <Button component={Link} to="/expedientes" sx={{ alignSelf: 'flex-start' }}>← Volver a expedientes</Button>
    <Card><CardContent><Stack spacing={2.5}>
      <Box><Typography variant="h4" fontWeight={800}>{expediente.cliente}</Typography><Typography color="text.secondary">Folio {expediente.id} · Actualizado {expediente.ultimaActualizacion}</Typography></Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
        <Button variant="contained" startIcon={<WhatsApp />} onClick={whatsapp}>WhatsApp</Button>
        <Button variant="outlined" startIcon={<CallRounded />} onClick={llamar}>Llamar</Button>
        <Button variant="outlined" startIcon={<ContentCopyRounded />} onClick={() => void copiarCurp()}>Copiar CURP</Button>
        <Button variant="outlined" startIcon={<ShareRounded />} onClick={() => void compartir()}>Compartir</Button>
        <Button variant="outlined" startIcon={<PictureAsPdfRounded />} onClick={() => window.print()}>Generar PDF</Button>
      </Stack>
      <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="scrollable" scrollButtons="auto"><Tab label="Datos personales"/><Tab label="Trámite"/><Tab label="Documentos"/><Tab label="Notas"/></Tabs>
      {tab === 0 && <Stack spacing={2}>{field('Nombre completo', 'cliente')}{field('CURP', 'curp')}{field('RFC', 'rfc')}{field('Teléfono', 'telefono')}<TextField select label="Estado" value={expediente.estado} disabled={!editando} onChange={(e) => cambiar('estado', e.target.value as EstadoExpediente)}>{['Activo','En revisión','Archivado'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField></Stack>}
      {tab === 1 && <Stack spacing={2}>{field('Fecha de inicio', 'fechaInicio', { type: 'date' })}{field('Fecha para crear solicitud', 'fechaSolicitud', { type: 'date' })}{field('Fecha de culminación', 'fechaCulminacion', { type: 'date' })}</Stack>}
      {tab === 2 && <Stack spacing={2}><Alert severity="info">Documentos registrados: {expediente.documentos}. La migración conservará las categorías: identificación frontal, identificación trasera, pagaré, estado de cuenta y constancia fiscal.</Alert><Button variant="outlined">Agregar documento o fotografía</Button><Button variant="outlined">Abrir carpeta de documentos</Button><Button variant="outlined" startIcon={<WhatsApp />}>Enviar documento por WhatsApp</Button></Stack>}
      {tab === 3 && <Stack spacing={2}>{field('Notas y avances', 'notas', { multiline: true })}</Stack>}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
        {!editando ? <Button variant="contained" startIcon={<EditRounded />} onClick={() => setEditando(true)}>Editar expediente</Button> : <><Button variant="contained" startIcon={<Save />} onClick={guardar}>Guardar cambios</Button><Button variant="outlined" onClick={() => { setExpediente(encontrado); setEditando(false); }}>Cancelar edición</Button></>}
        <Button color="error" startIcon={<Delete />} onClick={eliminar}>Eliminar expediente</Button>
      </Stack>
    </Stack></CardContent></Card>
    <Snackbar open={Boolean(mensaje)} autoHideDuration={2500} onClose={() => setMensaje('')} message={mensaje}/>
  </Stack>;
}
