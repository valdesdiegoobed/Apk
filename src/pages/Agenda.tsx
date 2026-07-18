import { CalendarMonthRounded } from '@mui/icons-material';
import { Alert, Card, CardContent, Chip, Stack, Typography } from '@mui/material';
import { cargarExpedientes } from '../expedientesStore';

const hoy = new Date().toISOString().slice(0, 10);
const dias = (fecha?: string) => fecha ? Math.ceil((new Date(`${fecha}T12:00:00`).getTime() - new Date(`${hoy}T12:00:00`).getTime()) / 86400000) : 9999;

export function Component() {
  const expedientes = cargarExpedientes().filter((e) => e.fechaSolicitud).sort((a, b) => String(a.fechaSolicitud).localeCompare(String(b.fechaSolicitud)));
  return <Stack spacing={3}>
    <Stack direction="row" spacing={1.5} alignItems="center"><CalendarMonthRounded color="primary"/><Typography variant="h4" fontWeight={800}>Agenda de solicitudes</Typography></Stack>
    <Alert severity="info">Vista unificada de solicitudes para hoy, próximos siete días y vencidas.</Alert>
    {!expedientes.length && <Typography color="text.secondary">No hay solicitudes programadas.</Typography>}
    {expedientes.map((e) => { const d = dias(e.fechaSolicitud); const estado = d < 0 ? 'Vencida' : d === 0 ? 'Hoy' : d <= 7 ? 'Próxima' : 'Programada'; return <Card key={e.id}><CardContent><Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1}><Stack><Typography fontWeight={800}>{e.cliente}</Typography><Typography color="text.secondary">{e.id} · {e.fechaSolicitud}</Typography></Stack><Chip label={estado} color={d < 0 ? 'error' : d <= 7 ? 'warning' : 'default'}/></Stack></CardContent></Card>; })}
  </Stack>;
}
