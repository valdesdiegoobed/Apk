import { Add, Search, UploadFile, WhatsApp } from '@mui/icons-material';
import { Accordion, AccordionDetails, AccordionSummary, Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, InputAdornment, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { DocumentoCliente, EstadoExpediente, Expediente } from '../data';
import { generarFolio, guardarExpediente, listarExpedientes } from '../firestoreStore';

type Filtro = 'Todos' | 'Próximos' | 'Vencidos' | 'Incompletos';

const vacio = { cliente: '', curp: '', rfc: '', telefono: '', contrasenaAfore: '', estado: 'Activo' as EstadoExpediente, fechaInicio: '', fechaSolicitud: '', fechaCulminacion: '', notas: '', fotoNombre: '', archivos: [] as DocumentoCliente[] };
const tiposDocumento = ['Identificación frontal', 'Identificación trasera', 'Pagaré', 'Estado de cuenta', 'Constancia fiscal'];

function sumarDias(fecha: string, dias: number) {
  if (!fecha) return '';
  const [ano, mes, dia] = fecha.split('-').map(Number);
  const valor = new Date(Date.UTC(ano, mes - 1, dia));
  valor.setUTCDate(valor.getUTCDate() + dias);
  return valor.toISOString().slice(0, 10);
}

function diferenciaDias(fecha?: string) {
  if (!fecha) return Number.POSITIVE_INFINITY;
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const destino = new Date(`${fecha}T00:00:00`);
  return Math.round((destino.getTime() - hoy.getTime()) / 86400000);
}

export function Component() {
  const [expedientes, setExpedientes] = useState<Expediente[]>([]);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState<Filtro>('Todos');
  const [abierto, setAbierto] = useState(false);
  const [formulario, setFormulario] = useState(vacio);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    void listarExpedientes().then(setExpedientes).catch(() => setError('No fue posible cargar los expedientes. Revisa tu conexión y las reglas de Firestore.')).finally(() => setCargando(false));
  }, []);

  const filtrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return expedientes.filter((e) => {
      const coincide = !texto || [e.cliente, e.id, e.curp, e.rfc, e.telefono, e.estado].some((v) => v?.toLowerCase().includes(texto));
      const dias = diferenciaDias(e.fechaSolicitud);
      const incompleto = (e.documentos ?? 0) < tiposDocumento.length;
      const categoria = filtro === 'Todos' || (filtro === 'Próximos' && dias >= 0 && dias <= 7) || (filtro === 'Vencidos' && dias < 0) || (filtro === 'Incompletos' && incompleto);
      return coincide && categoria;
    });
  }, [busqueda, expedientes, filtro]);

  const crear = async () => {
    if (!formulario.cliente.trim()) return;
    setGuardando(true); setError('');
    const nuevo: Expediente = {
      id: generarFolio(expedientes), cliente: formulario.cliente.trim(), estado: formulario.estado,
      categoria: 'Ayuda por desempleo', ultimaActualizacion: new Date().toISOString().slice(0, 10),
      responsable: 'Asesoría', documentos: formulario.archivos.length, notas: formulario.notas, curp: formulario.curp.toUpperCase(),
      rfc: formulario.rfc.toUpperCase(), telefono: formulario.telefono, contrasenaAfore: formulario.contrasenaAfore,
      fechaInicio: formulario.fechaInicio, fechaSolicitud: formulario.fechaSolicitud, fechaCulminacion: formulario.fechaCulminacion,
      fotoNombre: formulario.fotoNombre, archivos: formulario.archivos,
    };
    try {
      await guardarExpediente(nuevo);
      setExpedientes((actuales) => [nuevo, ...actuales]);
      setFormulario(vacio); setAbierto(false);
    } catch { setError('No fue posible guardar el expediente en Firebase.'); }
    finally { setGuardando(false); }
  };

  const seleccionarDocumento = (tipo: string, archivo?: File) => {
    if (!archivo) return;
    const restantes = formulario.archivos.filter((item) => item.tipo !== tipo);
    setFormulario({ ...formulario, archivos: [...restantes, { tipo, nombre: archivo.name, mime: archivo.type }] });
  };

  return <Stack spacing={3}>
    <Box><Typography variant="h4" sx={{ fontWeight: 700 }}>Expedientes de clientes</Typography><Typography color="text.secondary">Migración funcional de DV Control v1.8.3: búsqueda, filtros, fotografía, documentos y cálculo automático de fechas.</Typography></Box>
    <Alert severity="info">La fecha para realizar la solicitud se calcula automáticamente 46 días después de la fecha de inicio.</Alert>
    {error && <Alert severity="error">{error}</Alert>}
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><TextField fullWidth value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar por nombre, folio, CURP, RFC o teléfono" slotProps={{ input: { startAdornment: <InputAdornment position="start"><Search /></InputAdornment> } }}/><Button variant="contained" startIcon={<Add />} onClick={() => setAbierto(true)}>Nuevo expediente</Button></Stack>
    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">{(['Todos','Próximos','Vencidos','Incompletos'] as Filtro[]).map((item) => <Chip key={item} label={item} clickable color={filtro === item ? 'primary' : 'default'} onClick={() => setFiltro(item)}/>)}</Stack>
    {cargando ? <Box display="grid" sx={{ placeItems: 'center', py: 6 }}><CircularProgress /></Box> : <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>{filtrados.map((e) => { const dias = diferenciaDias(e.fechaSolicitud); return <Card key={e.id}><CardContent><Stack spacing={1.5}><Stack direction="row" justifyContent="space-between"><Typography fontWeight={700}>{e.id}</Typography><Chip label={dias < 0 ? 'Vencido' : dias <= 7 ? 'Próximo' : e.estado} color={dias < 0 ? 'error' : dias <= 7 ? 'warning' : e.estado === 'Activo' ? 'success' : 'default'} size="small"/></Stack><Typography variant="h6">{e.cliente}</Typography><Typography color="text.secondary">{e.curp || 'CURP pendiente'}</Typography><Typography variant="body2">Solicitud: {e.fechaSolicitud || 'Sin fecha'}</Typography><Typography variant="body2">Documentos: {e.documentos ?? 0} de {tiposDocumento.length}</Typography><Stack direction="row" spacing={1}><Button component={Link} to={`/expedientes/${e.id}`} variant="outlined">Abrir</Button>{e.telefono && <Button href={`https://wa.me/52${e.telefono.replace(/\D/g, '')}`} target="_blank" startIcon={<WhatsApp/>}>WhatsApp</Button>}</Stack></Stack></CardContent></Card>; })}</Box>}
    {!cargando && !filtrados.length && <Typography color="text.secondary">No se encontraron expedientes.</Typography>}

    <Dialog open={abierto} onClose={() => !guardando && setAbierto(false)} fullWidth maxWidth="md"><DialogTitle>Nuevo expediente</DialogTitle><DialogContent><Stack spacing={1.5} sx={{ mt: 1 }}>
      <Accordion defaultExpanded><AccordionSummary><Typography fontWeight={700}>📷 Fotografía</Typography></AccordionSummary><AccordionDetails><Button component="label" variant="outlined" startIcon={<UploadFile/>}>Seleccionar o tomar fotografía<input hidden type="file" accept="image/*" capture="environment" onChange={(e) => setFormulario({ ...formulario, fotoNombre: e.target.files?.[0]?.name ?? '' })}/></Button>{formulario.fotoNombre && <Typography variant="body2" sx={{ mt: 1 }}>{formulario.fotoNombre}</Typography>}</AccordionDetails></Accordion>
      <Accordion defaultExpanded><AccordionSummary><Typography fontWeight={700}>👤 Datos personales</Typography></AccordionSummary><AccordionDetails><Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)' }, gap: 2 }}><TextField label="Nombre completo" required value={formulario.cliente} onChange={(e) => setFormulario({ ...formulario, cliente: e.target.value })}/><TextField label="CURP" inputProps={{ maxLength: 18 }} value={formulario.curp} onChange={(e) => setFormulario({ ...formulario, curp: e.target.value.toUpperCase() })}/><TextField label="RFC" value={formulario.rfc} onChange={(e) => setFormulario({ ...formulario, rfc: e.target.value.toUpperCase() })}/><TextField label="Teléfono / WhatsApp" value={formulario.telefono} onChange={(e) => setFormulario({ ...formulario, telefono: e.target.value })}/><TextField label="Contraseña AFORE" type="password" value={formulario.contrasenaAfore} onChange={(e) => setFormulario({ ...formulario, contrasenaAfore: e.target.value })}/><TextField select label="Estado" value={formulario.estado} onChange={(e) => setFormulario({ ...formulario, estado: e.target.value as EstadoExpediente })}>{['Activo','En revisión','Archivado'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField></Box></AccordionDetails></Accordion>
      <Accordion defaultExpanded><AccordionSummary><Typography fontWeight={700}>📅 Información del trámite</Typography></AccordionSummary><AccordionDetails><Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3,1fr)' }, gap: 2 }}><TextField label="Fecha de inicio" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaInicio} onChange={(e) => { const fechaInicio = e.target.value; setFormulario({ ...formulario, fechaInicio, fechaSolicitud: sumarDias(fechaInicio, 46), fechaCulminacion: sumarDias(fechaInicio, 49) }); }}/><TextField label="Fecha para solicitud" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaSolicitud} onChange={(e) => setFormulario({ ...formulario, fechaSolicitud: e.target.value })}/><TextField label="Fecha de culminación" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaCulminacion} onChange={(e) => setFormulario({ ...formulario, fechaCulminacion: e.target.value })}/></Box></AccordionDetails></Accordion>
      <Accordion><AccordionSummary><Typography fontWeight={700}>📁 Documentos ({formulario.archivos.length}/{tiposDocumento.length})</Typography></AccordionSummary><AccordionDetails><Stack spacing={1.5}>{tiposDocumento.map((tipo) => <Button key={tipo} component="label" variant="outlined" startIcon={<UploadFile/>} sx={{ justifyContent: 'flex-start' }}>{tipo}{formulario.archivos.find((a) => a.tipo === tipo)?.nombre ? `: ${formulario.archivos.find((a) => a.tipo === tipo)?.nombre}` : ''}<input hidden type="file" accept="image/*,.pdf" onChange={(e) => seleccionarDocumento(tipo, e.target.files?.[0])}/></Button>)}</Stack></AccordionDetails></Accordion>
      <Accordion><AccordionSummary><Typography fontWeight={700}>🗒️ Notas</Typography></AccordionSummary><AccordionDetails><TextField fullWidth label="Nota" multiline minRows={3} inputProps={{ maxLength: 100 }} helperText={`${formulario.notas.length} de 100`} value={formulario.notas} onChange={(e) => setFormulario({ ...formulario, notas: e.target.value })}/></AccordionDetails></Accordion>
    </Stack></DialogContent><DialogActions><Button disabled={guardando} onClick={() => setAbierto(false)}>Cancelar</Button><Button disabled={guardando || !formulario.cliente.trim()} variant="contained" onClick={() => void crear()}>{guardando ? 'Guardando…' : 'Guardar expediente'}</Button></DialogActions></Dialog>
  </Stack>;
}
