import { Box, Card, CardActionArea, CardContent, Chip, LinearProgress, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import type { Expediente } from '../data';
import { cargarExpedientes } from '../expedientesStore';

function diasHasta(fecha?: string) { if (!fecha) return 9999; const h = new Date(); h.setHours(0,0,0,0); return Math.round((new Date(`${fecha}T00:00:00`).getTime()-h.getTime())/86400000); }

export function Component() {
  const [lista,setLista] = useState<Expediente[]>(cargarExpedientes);
  useEffect(() => { const fn=()=>setLista(cargarExpedientes()); window.addEventListener('expedientes-actualizados',fn); return()=>window.removeEventListener('expedientes-actualizados',fn); },[]);
  const datos = useMemo(() => {
    const pendientes = lista.filter(e=>e.estado!=='Realizada');
    const hoy = pendientes.filter(e=>diasHasta(e.fechaSolicitud)===0);
    const proximos = pendientes.filter(e=>{const d=diasHasta(e.fechaSolicitud);return d>=1&&d<=7});
    const vencidos = pendientes.filter(e=>diasHasta(e.fechaSolicitud)<0);
    const mes = new Date().toISOString().slice(0,7);
    const agenda = pendientes.filter(e=>e.fechaSolicitud?.startsWith(mes));
    return {hoy,proximos,vencidos,agenda};
  },[lista]);
  const cards = [
    {title:'Expedientes',icon:'👥',count:lista.length,color:'#1976d2',items:lista.map(e=>e.cliente)},
    {title:'Solicitudes de desempleo para hoy',icon:'📍',count:datos.hoy.length,color:'#2563eb',items:datos.hoy.map(e=>e.cliente)},
    {title:'Próximos 7 días',icon:'⌛',count:datos.proximos.length,color:'#f59e0b',items:datos.proximos.map(e=>`${e.cliente} · ${e.fechaSolicitud}`)},
    {title:'Solicitudes vencidas',icon:'⚠️',count:datos.vencidos.length,color:'#ef4444',items:datos.vencidos.map(e=>`${e.cliente} · ${Math.abs(diasHasta(e.fechaSolicitud))} días`)},
    {title:'Solicitudes prioritarias',icon:'⏰',count:datos.vencidos.length,color:'#9333ea',items:datos.vencidos.map(e=>e.cliente)},
    {title:'Agenda mensual',icon:'📅',count:datos.agenda.length,color:'#0f9d8a',items:datos.agenda.map(e=>`${e.fechaSolicitud} · ${e.cliente}`)},
  ];
  return <Stack spacing={2.5}>
    <Box><Typography variant="h4" fontWeight={900}>Panel de control</Typography><Typography color="text.secondary">Seguimiento automático de expedientes y solicitudes de retiro por desempleo.</Typography></Box>
    <Box sx={{display:'grid',gridTemplateColumns:{xs:'repeat(2,1fr)',md:'repeat(3,1fr)'},gap:2}}>{cards.map(c=><Card key={c.title} variant="outlined" sx={{borderLeft:`7px solid ${c.color}`,borderRadius:4,minHeight:210}}><CardActionArea component={Link} to={c.title==='Agenda mensual'?'/agenda':'/expedientes'} sx={{height:'100%'}}><CardContent sx={{height:'100%'}}><Stack spacing={1.5} sx={{height:'100%'}}><Typography variant="h6" fontWeight={900}>{c.icon} {c.title}</Typography><Typography variant="h3" fontWeight={900}>{c.count}</Typography><LinearProgress variant="determinate" value={c.count?100:0}/><Box sx={{mt:'auto'}}>{c.items.slice(0,3).map((x,i)=><Chip key={i} label={x} size="small" sx={{mr:.5,mb:.5,maxWidth:'100%'}}/>)}<Typography fontWeight={700} mt={1}>{c.title==='Agenda mensual'?'Ver detalles':'Ver personas'} ▾</Typography></Box></Stack></CardContent></CardActionArea></Card>)}</Box>
  </Stack>;
}
