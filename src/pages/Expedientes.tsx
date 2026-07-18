import { Add, CameraAlt, ContentCopy, DeleteSweep, Restore, Search, UploadFile } from '@mui/icons-material';
import { Accordion, AccordionDetails, AccordionSummary, Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle, InputAdornment, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { diferenciaDias, estadoCalculado, sumarDiasNaturales, TIPOS_DOCUMENTO, type DocumentoCliente, type EstadoSolicitud, type Expediente } from '../data';
import { cargarExpedientes, descargarRespaldo, generarFolio, guardarExpedientes, leerRespaldo, ultimoRespaldo } from '../expedientesStore';

const vacio = { cliente: '', curp: '', telefono: '', contrasenaAfore: '', estadoSolicitud: 'Pendiente' as EstadoSolicitud, fechaInicio: '', fechaSolicitud: '', notas: '', foto: undefined as DocumentoCliente | undefined, documentos: [] as DocumentoCliente[] };
const leerArchivo = (file: File) => new Promise<string>((resolve, reject) => { const r = new FileReader(); r.onload = () => resolve(String(r.result)); r.onerror = reject; r.readAsDataURL(file); });

export function Component() {
  const [params] = useSearchParams();
  const [lista, setLista] = useState<Expediente[]>(cargarExpedientes);
  const [form, setForm] = useState(vacio);
  const [abierto, setAbierto] = useState(params.get('nuevo') === '1');
  const [busqueda, setBusqueda] = useState('');
  const [mensaje, setMensaje] = useState('');
  const restoreRef = useRef<HTMLInputElement>(null);

  useEffect(() => { const fn = () => setLista(cargarExpedientes()); window.addEventListener('expedientes-actualizados', fn); return () => window.removeEventListener('expedientes-actualizados', fn); }, []);

  const filtrados = useMemo(() => {
    const filtro = params.get('filtro');
    return lista.filter((e) => {
      const texto = [e.cliente, e.curp, e.telefono, e.id].some((v) => String(v).toLowerCase().includes(busqueda.toLowerCase()));
      const dias = diferenciaDias(e.fechaSolicitud);
      const coincide = !filtro || (filtro === 'hoy' && dias === 0 && estadoCalculado(e) !== 'Realizada') || (filtro === 'proximos' && dias >= 1 && dias <= 7 && estadoCalculado(e) !== 'Realizada') || (filtro === 'vencidos' && dias < 0 && estadoCalculado(e) !== 'Realizada');
      return texto && coincide;
    });
  }, [lista, busqueda, params]);

  const archivo = async (tipo: string, file?: File) => {
    if (!file) return;
    const nuevo: DocumentoCliente = { tipo, nombre: file.name, mime: file.type || 'application/octet-stream', dataUrl: await leerArchivo(file), fechaCarga: new Date().toISOString() };
    setForm((f) => tipo === 'Fotografía del cliente' ? { ...f, foto: nuevo } : { ...f, documentos: [...f.documentos.filter((d) => d.tipo !== tipo), nuevo] });
  };

  const copiar = async (texto: string, nombre: string) => { if (!texto) return; await navigator.clipboard.writeText(texto); setMensaje(`${nombre} copiada`); };
  const guardar = () => {
    if (!form.cliente.trim()) return setMensaje('El nombre completo es obligatorio');
    const nuevo: Expediente = { id: generarFolio(lista), cliente: form.cliente.trim().toUpperCase(), fechaCreacion: new Date().toISOString(), foto: form.foto, curp: form.curp.toUpperCase(), contrasenaAfore: form.contrasenaAfore, telefono: form.telefono, fechaInicio: form.fechaInicio, fechaSolicitud: form.fechaSolicitud, estadoSolicitud: form.estadoSolicitud, notas: form.notas, documentos: form.documentos };
    const nuevos = [nuevo, ...lista]; guardarExpedientes(nuevos); setLista(nuevos); setForm(vacio); setAbierto(false); setMensaje('Expediente guardado correctamente');
  };
  const limpiar = () => { if (confirm('¿Borrar todo lo capturado en este formulario?')) setForm(vacio); };
  const restaurar = async (file?: File) => { if (!file) return; try { const datos = await leerRespaldo(file); if (!confirm(`Se restaurarán ${datos.length} expedientes. ¿Continuar?`)) return; guardarExpedientes(datos); setLista(datos); setMensaje('Respaldo restaurado correctamente'); } catch { setMensaje('El archivo de respaldo no es válido'); } };

  return <Stack spacing={2.5}>
    <Box>
      <Typography variant="h4" fontWeight={900}>Expedientes</Typography>
      <Typography color="text.secondary">Copia automática: {ultimoRespaldo() ? new Date(ultimoRespaldo()!).toLocaleString() : 'sin respaldo todavía'}</Typography>
    </Box>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
      <Button variant="contained" size="large" startIcon={<Add />} onClick={() => setAbierto(true)}>Nuevo expediente</Button>
      <Button variant="outlined" size="large" startIcon={<Restore />} onClick={() => restoreRef.current?.click()}>Restaurar</Button>
      <Button variant="outlined" onClick={() => descargarRespaldo(lista)}>Crear respaldo</Button>
      <input ref={restoreRef} hidden type="file" accept="application/json,.json" onChange={(e) => void restaurar(e.target.files?.[0])} />
    </Stack>
    <TextField value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar por nombre, CURP, teléfono o folio" slotProps={{ input: { startAdornment: <InputAdornment position="start"><Search /></InputAdornment> } }} />
    <Typography variant="h5" fontWeight={900}>Expedientes guardados: {lista.length}</Typography>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2,1fr)' }, gap: 2 }}>
      {filtrados.map((e) => { const estado = estadoCalculado(e); return <Card key={e.id} variant="outlined"><CardContent><Stack spacing={1.2}><Stack direction="row" justifyContent="space-between"><Typography fontWeight={900}>{e.cliente}</Typography><Chip size="small" label={estado} color={estado === 'Realizada' ? 'success' : estado === 'Vencida' ? 'error' : estado === 'Activa' ? 'warning' : 'default'} /></Stack><Typography color="text.secondary">{e.id} · Creado {e.fechaCreacion.slice(0, 10)}</Typography><Typography>Solicitud: {e.fechaSolicitud || 'Sin fecha'}</Typography><Typography>Documentos: {e.documentos.length}/5</Typography><Button component={Link} to={`/expedientes/${e.id}`} variant="contained">Abrir expediente</Button></Stack></CardContent></Card>; })}
    </Box>

    <Dialog open={abierto} onClose={() => setAbierto(false)} fullScreen>
      <DialogTitle sx={{ fontWeight: 900 }}>➕ Nuevo expediente</DialogTitle>
      <DialogContent><Stack spacing={1.5} sx={{ mt: 1 }}>
        <Accordion defaultExpanded><AccordionSummary><Typography fontWeight={900}>📷 Fotografía del cliente</Typography></AccordionSummary><AccordionDetails><Stack spacing={1.2}><Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}><Button component="label" variant="outlined" startIcon={<CameraAlt />}>Abrir cámara<input hidden type="file" accept="image/*" capture="environment" onChange={(e) => void archivo('Fotografía del cliente', e.target.files?.[0])} /></Button><Button component="label" variant="outlined" startIcon={<UploadFile />}>Seleccionar archivo<input hidden type="file" accept="image/*,.pdf" onChange={(e) => void archivo('Fotografía del cliente', e.target.files?.[0])} /></Button></Stack>{form.foto?.dataUrl.startsWith('data:image') && <Box component="img" src={form.foto.dataUrl} sx={{ width: 130, height: 130, objectFit: 'cover', borderRadius: 3 }} />}<Chip label={form.foto ? 'Fotografía agregada' : 'Fotografía pendiente'} color={form.foto ? 'success' : 'default'} /></Stack></AccordionDetails></Accordion>
        <Accordion><AccordionSummary><Typography fontWeight={900}>👤 Datos personales</Typography></AccordionSummary><AccordionDetails><Stack spacing={2}><TextField required label="Nombre completo" value={form.cliente} onChange={(e) => setForm({ ...form, cliente: e.target.value.toUpperCase() })} /><Stack direction="row" spacing={1}><TextField fullWidth label="CURP" inputProps={{ maxLength: 18 }} value={form.curp} onChange={(e) => setForm({ ...form, curp: e.target.value.toUpperCase() })} /><Button variant="outlined" onClick={() => void copiar(form.curp, 'CURP')}><ContentCopy /></Button></Stack><Stack direction="row" spacing={1}><TextField fullWidth label="Contraseña AFORE" value={form.contrasenaAfore} onChange={(e) => setForm({ ...form, contrasenaAfore: e.target.value })} /><Button variant="outlined" onClick={() => void copiar(form.contrasenaAfore, 'Contraseña')}><ContentCopy /></Button></Stack><TextField label="Teléfono / WhatsApp" inputMode="numeric" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value.replace(/\D/g, '').slice(0, 10) })} /></Stack></AccordionDetails></Accordion>
        <Accordion><AccordionSummary><Typography fontWeight={900}>📅 Fechas del trámite</Typography></AccordionSummary><AccordionDetails><Stack spacing={2}><TextField label="Fecha de inicio del trámite" type="date" InputLabelProps={{ shrink: true }} value={form.fechaInicio} onChange={(e) => { const fechaInicio = e.target.value; setForm({ ...form, fechaInicio, fechaSolicitud: sumarDiasNaturales(fechaInicio, 46) }); }} /><TextField label="Fecha de solicitud de retiro por desempleo" type="date" InputLabelProps={{ shrink: true }} value={form.fechaSolicitud} disabled /><TextField select label="Estado de la solicitud" value={form.estadoSolicitud} onChange={(e) => setForm({ ...form, estadoSolicitud: e.target.value as EstadoSolicitud })}>{['Pendiente', 'Activa', 'Realizada', 'Vencida'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField></Stack></AccordionDetails></Accordion>
        <Accordion><AccordionSummary><Typography fontWeight={900}>📁 Documentos personales · {form.documentos.length}/5</Typography></AccordionSummary><AccordionDetails><Stack spacing={2}>{TIPOS_DOCUMENTO.map((tipo, i) => { const cargado = form.documentos.find((d) => d.tipo === tipo); return <Card key={tipo} variant="outlined"><CardContent><Stack spacing={1}><Typography fontWeight={800}>{i + 1}. {tipo}</Typography><Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}><Button component="label" variant="outlined" startIcon={<CameraAlt />}>Abrir cámara<input hidden type="file" accept="image/*" capture="environment" onChange={(e) => void archivo(tipo, e.target.files?.[0])} /></Button><Button component="label" variant="outlined" startIcon={<UploadFile />}>Seleccionar archivo<input hidden type="file" accept="image/*,.pdf" onChange={(e) => void archivo(tipo, e.target.files?.[0])} /></Button></Stack><Chip label={cargado ? `${tipo} cargado` : `${tipo} pendiente`} color={cargado ? 'success' : 'default'} /></Stack></CardContent></Card>; })}</Stack></AccordionDetails></Accordion>
        <Accordion><AccordionSummary><Typography fontWeight={900}>📝 Notas</Typography></AccordionSummary><AccordionDetails><TextField fullWidth multiline minRows={4} label="Observaciones adicionales" inputProps={{ maxLength: 100 }} helperText={`${form.notas.length} de 100`} value={form.notas} onChange={(e) => setForm({ ...form, notas: e.target.value })} /></AccordionDetails></Accordion>
        {mensaje && <Alert severity="info">{mensaje}</Alert>}
      </Stack></DialogContent>
      <DialogActions sx={{ p: 2 }}><Button color="error" startIcon={<DeleteSweep />} onClick={limpiar}>Borrar</Button><Button onClick={() => setAbierto(false)}>Cerrar</Button><Button variant="contained" onClick={guardar}>Guardar</Button></DialogActions>
    </Dialog>
    {mensaje && !abierto && <Alert severity="info">{mensaje}</Alert>}
  </Stack>;
}
