import { Box, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { diferenciaDias, estadoCalculado, type Expediente } from '../data';
import { cargarExpedientes } from '../expedientesStore';

export function Component() {
  const [lista, setLista] = useState<Expediente[]>(cargarExpedientes);
  useEffect(() => {
    const actualizar = () => setLista(cargarExpedientes());
    window.addEventListener('expedientes-actualizados', actualizar);
    return () => window.removeEventListener('expedientes-actualizados', actualizar);
  }, []);

  const datos = useMemo(() => {
    const abiertos = lista.filter((e) => estadoCalculado(e) !== 'Realizada');
    const hoy = abiertos.filter((e) => diferenciaDias(e.fechaSolicitud) === 0);
    const proximos = abiertos.filter((e) => { const d = diferenciaDias(e.fechaSolicitud); return d >= 1 && d <= 7; });
    const vencidos = abiertos.filter((e) => diferenciaDias(e.fechaSolicitud) < 0);
    const mes = new Date().toISOString().slice(0, 7);
    const agenda = abiertos.filter((e) => e.fechaSolicitud.startsWith(mes));
    return { hoy, proximos, vencidos, agenda };
  }, [lista]);

  const tarjetas = [
    { titulo: 'Expedientes', icono: '👥', color: '#1976d2', total: lista.length, elementos: lista.map((e) => e.cliente), ruta: '/expedientes' },
    { titulo: 'Solicitudes de desempleo para hoy', icono: '📍', color: '#2563eb', total: datos.hoy.length, elementos: datos.hoy.map((e) => e.cliente), ruta: '/expedientes?filtro=hoy' },
    { titulo: 'Próximos 7 días', icono: '⌛', color: '#f59e0b', total: datos.proximos.length, elementos: datos.proximos.map((e) => `${e.cliente} · ${e.fechaSolicitud}`), ruta: '/expedientes?filtro=proximos' },
    { titulo: 'Solicitudes vencidas', icono: '⚠️', color: '#ef4444', total: datos.vencidos.length, elementos: datos.vencidos.map((e) => `${e.cliente} · ${Math.abs(diferenciaDias(e.fechaSolicitud))} días`), ruta: '/expedientes?filtro=vencidos' },
    { titulo: 'Solicitudes prioritarias', icono: '⏰', color: '#9333ea', total: datos.vencidos.length, elementos: datos.vencidos.map((e) => e.cliente), ruta: '/expedientes?filtro=vencidos' },
    { titulo: 'Agenda mensual', icono: '📅', color: '#0f9d8a', total: datos.agenda.length, elementos: datos.agenda.map((e) => `${e.fechaSolicitud} · ${e.cliente}`), ruta: '/agenda' },
  ];

  return <Stack spacing={2.5}>
    <Box>
      <Typography variant="h4" fontWeight={900}>Panel de expedientes</Typography>
      <Typography color="text.secondary">Control de solicitudes calculadas a 46 días naturales.</Typography>
    </Box>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, minmax(0,1fr))', md: 'repeat(3,1fr)' }, gap: 2 }}>
      {tarjetas.map((t) => <Card key={t.titulo} variant="outlined" sx={{ borderLeft: `7px solid ${t.color}`, borderRadius: 4, minHeight: 220 }}>
        <CardActionArea component={Link} to={t.ruta} sx={{ height: '100%' }}>
          <CardContent sx={{ height: '100%' }}>
            <Stack spacing={1.25} sx={{ height: '100%' }}>
              <Typography variant="h6" fontWeight={900}>{t.icono} {t.titulo}</Typography>
              <Typography variant="h3" fontWeight={900}>{t.total}</Typography>
              <Box sx={{ mt: 'auto' }}>
                {t.elementos.slice(0, 3).map((x, i) => <Chip key={`${x}-${i}`} label={x} size="small" sx={{ mr: .5, mb: .5, maxWidth: '100%' }} />)}
                <Typography fontWeight={800} mt={1}>{t.titulo === 'Agenda mensual' ? 'Ver detalles' : 'Ver personas'} ▾</Typography>
              </Box>
            </Stack>
          </CardContent>
        </CardActionArea>
      </Card>)}
    </Box>
  </Stack>;
}
