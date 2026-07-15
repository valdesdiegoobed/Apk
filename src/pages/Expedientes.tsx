import { Add, Search } from '@mui/icons-material';
import { Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle, InputAdornment, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { EstadoExpediente, Expediente } from '../data';
import { cargarExpedientes, generarFolio, guardarExpedientes } from '../expedientesStore';

const vacio = { cliente: '', curp: '', rfc: '', telefono: '', estado: 'Activo' as EstadoExpediente, fechaInicio: '', fechaSolicitud: '', fechaCulminacion: '', notas: '' };

export function Component() {
  const [expedientes, setExpedientes] = useState<Expediente[]>(cargarExpedientes);
  const [busqueda, setBusqueda] = useState('');
  const [abierto, setAbierto] = useState(false);
  const [formulario, setFormulario] = useState(vacio);

  const filtrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    if (!texto) return expedientes;
    return expedientes.filter((e) => [e.cliente, e.id, e.curp, e.rfc, e.telefono, e.estado].some((v) => v?.toLowerCase().includes(texto)));
  }, [busqueda, expedientes]);

  const crear = () => {
    if (!formulario.cliente.trim()) return;
    const nuevo: Expediente = {
      id: generarFolio(expedientes), cliente: formulario.cliente.trim(), estado: formulario.estado,
      categoria: 'Ayuda por desempleo', ultimaActualizacion: new Date().toISOString().slice(0, 10),
      responsable: 'Asesoría', documentos: 0, notas: formulario.notas, curp: formulario.curp.toUpperCase(),
      rfc: formulario.rfc.toUpperCase(), telefono: formulario.telefono, fechaInicio: formulario.fechaInicio,
      fechaSolicitud: formulario.fechaSolicitud, fechaCulminacion: formulario.fechaCulminacion,
    };
    const actualizados = [nuevo, ...expedientes];
    setExpedientes(actualizados); guardarExpedientes(actualizados); setFormulario(vacio); setAbierto(false);
  };

  return <Stack spacing={3}>
    <Box><Typography variant="h4" sx={{ fontWeight: 700 }}>Expedientes de clientes</Typography><Typography color="text.secondary">Versión de prueba: la información se guarda únicamente en este navegador.</Typography></Box>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><TextField fullWidth value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar por nombre, folio, CURP, RFC o teléfono" slotProps={{ input: { startAdornment: <InputAdornment position="start"><Search /></InputAdornment> } }}/><Button variant="contained" startIcon={<Add />} onClick={() => setAbierto(true)}>Nuevo cliente</Button></Stack>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>{filtrados.map((e) => <Card key={e.id}><CardContent><Stack spacing={1.5}><Stack direction="row" justifyContent="space-between"><Typography fontWeight={700}>{e.id}</Typography><Chip label={e.estado} color={e.estado === 'Activo' ? 'success' : e.estado === 'En revisión' ? 'warning' : 'default'} size="small"/></Stack><Typography variant="h6">{e.cliente}</Typography><Typography color="text.secondary">{e.curp || 'CURP pendiente'}</Typography><Typography variant="body2">Actualizado: {e.ultimaActualizacion}</Typography><Button component={Link} to={`/expedientes/${e.id}`} variant="outlined">Abrir expediente</Button></Stack></CardContent></Card>)}</Box>
    {!filtrados.length && <Typography color="text.secondary">No se encontraron expedientes.</Typography>}
    <Dialog open={abierto} onClose={() => setAbierto(false)} fullWidth maxWidth="sm"><DialogTitle>Registrar cliente ficticio</DialogTitle><DialogContent><Stack spacing={2} sx={{ mt: 1 }}><TextField label="Nombre completo" required value={formulario.cliente} onChange={(e) => setFormulario({ ...formulario, cliente: e.target.value })}/><TextField label="CURP" value={formulario.curp} onChange={(e) => setFormulario({ ...formulario, curp: e.target.value.toUpperCase() })}/><TextField label="RFC" value={formulario.rfc} onChange={(e) => setFormulario({ ...formulario, rfc: e.target.value.toUpperCase() })}/><TextField label="Teléfono" value={formulario.telefono} onChange={(e) => setFormulario({ ...formulario, telefono: e.target.value })}/><TextField select label="Estado" value={formulario.estado} onChange={(e) => setFormulario({ ...formulario, estado: e.target.value as EstadoExpediente })}>{['Activo','En revisión','Archivado'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField><TextField label="Fecha de inicio" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaInicio} onChange={(e) => setFormulario({ ...formulario, fechaInicio: e.target.value })}/><TextField label="Fecha para crear solicitud" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaSolicitud} onChange={(e) => setFormulario({ ...formulario, fechaSolicitud: e.target.value })}/><TextField label="Fecha de culminación" type="date" InputLabelProps={{ shrink: true }} value={formulario.fechaCulminacion} onChange={(e) => setFormulario({ ...formulario, fechaCulminacion: e.target.value })}/><TextField label="Notas" multiline minRows={3} value={formulario.notas} onChange={(e) => setFormulario({ ...formulario, notas: e.target.value })}/></Stack></DialogContent><DialogActions><Button onClick={() => setAbierto(false)}>Cancelar</Button><Button variant="contained" onClick={crear}>Guardar</Button></DialogActions></Dialog>
  </Stack>;
}
