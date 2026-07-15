import { Delete, Save } from '@mui/icons-material';
import { Alert, Button, Card, CardContent, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { EstadoExpediente, Expediente } from '../data';
import { cargarExpedientes, guardarExpedientes } from '../expedientesStore';

export function Component() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lista, setLista] = useState<Expediente[]>(cargarExpedientes);
  const encontrado = lista.find((item) => item.id === id);
  const [expediente, setExpediente] = useState<Expediente | undefined>(encontrado);
  const [guardado, setGuardado] = useState(false);

  if (!expediente) return <Stack spacing={2}><Alert severity="warning">El expediente no existe en este navegador.</Alert><Button component={Link} to="/expedientes">Volver</Button></Stack>;

  const cambiar = (campo: keyof Expediente, valor: string) => setExpediente({ ...expediente, [campo]: valor });
  const guardar = () => {
    const actualizado = { ...expediente, cliente: expediente.cliente.trim(), curp: expediente.curp?.toUpperCase(), rfc: expediente.rfc?.toUpperCase(), ultimaActualizacion: new Date().toISOString().slice(0, 10) };
    const nuevos = lista.map((item) => item.id === actualizado.id ? actualizado : item);
    setLista(nuevos); setExpediente(actualizado); guardarExpedientes(nuevos); setGuardado(true);
  };
  const eliminar = () => {
    if (!confirm('¿Eliminar este expediente ficticio de este navegador?')) return;
    guardarExpedientes(lista.filter((item) => item.id !== expediente.id)); navigate('/expedientes');
  };

  return <Stack spacing={3}>
    <Button component={Link} to="/expedientes" sx={{ alignSelf: 'flex-start' }}>← Volver</Button>
    <Card><CardContent><Stack spacing={2}>
      <Typography variant="h4" fontWeight={700}>{expediente.cliente}</Typography>
      <Typography color="text.secondary">Folio {expediente.id}</Typography>
      <Alert severity="warning">Modo de prueba. Los cambios se guardan solo en este dispositivo y pueden perderse si se borran los datos del navegador.</Alert>
      {guardado && <Alert severity="success">Los cambios se guardaron correctamente.</Alert>}
      <TextField label="Nombre completo" value={expediente.cliente} onChange={(e) => cambiar('cliente', e.target.value)} required/>
      <TextField label="CURP" value={expediente.curp || ''} onChange={(e) => cambiar('curp', e.target.value.toUpperCase())}/>
      <TextField label="RFC" value={expediente.rfc || ''} onChange={(e) => cambiar('rfc', e.target.value.toUpperCase())}/>
      <TextField label="Teléfono" value={expediente.telefono || ''} onChange={(e) => cambiar('telefono', e.target.value)}/>
      <TextField select label="Estado" value={expediente.estado} onChange={(e) => cambiar('estado', e.target.value as EstadoExpediente)}>{['Activo','En revisión','Archivado'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField>
      <TextField label="Fecha de inicio" type="date" InputLabelProps={{ shrink: true }} value={expediente.fechaInicio || ''} onChange={(e) => cambiar('fechaInicio', e.target.value)}/>
      <TextField label="Fecha para crear solicitud" type="date" InputLabelProps={{ shrink: true }} value={expediente.fechaSolicitud || ''} onChange={(e) => cambiar('fechaSolicitud', e.target.value)}/>
      <TextField label="Fecha de culminación" type="date" InputLabelProps={{ shrink: true }} value={expediente.fechaCulminacion || ''} onChange={(e) => cambiar('fechaCulminacion', e.target.value)}/>
      <TextField label="Notas y avances" multiline minRows={5} value={expediente.notas} onChange={(e) => cambiar('notas', e.target.value)}/>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><Button variant="contained" startIcon={<Save />} onClick={guardar}>Guardar cambios</Button><Button color="error" startIcon={<Delete />} onClick={eliminar}>Eliminar ficticio</Button></Stack>
      <Typography variant="body2" color="text.secondary">Documentos registrados: {expediente.documentos}. La carga de PDF e imágenes se habilitará al conectar Firebase Storage.</Typography>
    </Stack></CardContent></Card>
  </Stack>;
}
