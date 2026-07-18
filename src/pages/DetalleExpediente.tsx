import { CallRounded, Close, ContentCopyRounded, Delete, Download, EditRounded, PictureAsPdfRounded, Save, Visibility, WhatsApp } from '@mui/icons-material';
import { Accordion, AccordionDetails, AccordionSummary, Alert, Avatar, Box, Button, Card, CardContent, Chip, LinearProgress, MenuItem, Snackbar, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { DocumentoCliente, EstadoExpediente, Expediente } from '../data';
import { cargarExpedientes, guardarExpedientes } from '../expedientesStore';

const digits=(v='')=>v.replace(/\D/g,'');
const tipos=['Identificación frontal','Identificación trasera','Pagaré','Estado de cuenta','Constancia de situación fiscal'];
function diasProceso(fecha?:string){if(!fecha)return 0;const i=new Date(`${fecha}T00:00:00`);const h=new Date();h.setHours(0,0,0,0);return Math.max(1,Math.floor((h.getTime()-i.getTime())/86400000)+1)}
function descargar(a:DocumentoCliente){if(!a.dataUrl)return;const x=document.createElement('a');x.href=a.dataUrl;x.download=a.nombre;x.click()}

export function Component(){
 const {id}=useParams(); const nav=useNavigate(); const [lista,setLista]=useState<Expediente[]>(cargarExpedientes); const original=lista.find(x=>x.id===id); const [e,setE]=useState<Expediente|undefined>(original); const [editando,setEditando]=useState(false); const [mensaje,setMensaje]=useState('');
 if(!e)return <Stack><Alert severity="warning">El expediente no existe.</Alert><Button component={Link} to="/expedientes">Volver</Button></Stack>;
 const dia=diasProceso(e.fechaInicio); const progreso=Math.min(100,(dia/46)*100); const docs=e.archivos||[]; const estadoVisual=e.estado==='Realizada'?'Realizada':dia>46?'Vencida':dia===46?'Activa':'Pendiente';
 const cambiar=(k:keyof Expediente,v:any)=>setE({...e,[k]:v});
 const guardar=()=>{const actualizado={...e,cliente:e.cliente.toUpperCase().trim(),curp:e.curp?.toUpperCase(),documentos:docs.length,ultimaActualizacion:new Date().toISOString().slice(0,10)};const nueva=lista.map(x=>x.id===e.id?actualizado:x);guardarExpedientes(nueva);setLista(nueva);setE(actualizado);setEditando(false);setMensaje('Cambios guardados');};
 const eliminar=()=>{if(confirm('¿Estás seguro de eliminar este expediente?')){guardarExpedientes(lista.filter(x=>x.id!==e.id));nav('/expedientes')}};
 const copiar=async()=>{if(e.curp){await navigator.clipboard.writeText(e.curp);setMensaje('CURP copiada')}};
 const whatsapp=()=>{const t=digits(e.telefono);if(!t)return setMensaje('No hay teléfono guardado');window.open(`https://wa.me/52${t.replace(/^52/,'')}?text=${encodeURIComponent(`Hola ${e.cliente}, me comunico respecto a tu trámite.`)}`,'_blank')};
 const compartirArchivo=async(a:DocumentoCliente)=>{if(!a.dataUrl)return;const r=await fetch(a.dataUrl);const f=new File([await r.blob()],a.nombre,{type:a.mime});if(navigator.share)await navigator.share({files:[f],title:a.tipo});else setMensaje('El dispositivo no permite compartir este archivo')};
 const ver=(a:DocumentoCliente)=>{if(a.dataUrl)window.open(a.dataUrl,'_blank')};
 const pdf=()=>window.print();
 return <Stack spacing={2}>
  <Button component={Link} to="/expedientes" sx={{alignSelf:'flex-start'}}>← Volver</Button>
  <Card variant="outlined" sx={{borderRadius:4}}><CardContent><Stack spacing={2}>
   <Stack direction="row" spacing={2} alignItems="center"><Avatar src={e.fotoDataUrl} sx={{width:96,height:96}}>{e.cliente[0]}</Avatar><Box flex={1}><Typography variant="h4" fontWeight={900}>{e.cliente}</Typography><Typography color="text.secondary">Creado: {e.fechaCreacion?.slice(0,10)||e.ultimaActualizacion}</Typography><Typography variant="h6" fontWeight={800}>Día {Math.min(dia,46)} de 46 · {estadoVisual}</Typography><LinearProgress variant="determinate" value={progreso} color={estadoVisual==='Vencida'?'error':'primary'} sx={{height:12,borderRadius:8,mt:1}}/></Box></Stack>
   <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap"><Chip label={estadoVisual} color={estadoVisual==='Realizada'?'success':estadoVisual==='Vencida'?'error':estadoVisual==='Activa'?'primary':'default'}/><Chip label={`📁 ${docs.length}/5 documentos`} color={docs.length===5?'success':'default'}/>{dia>46&&e.estado!=='Realizada'&&<Chip label={`⚠️ Día ${dia} del proceso · solicitud vencida`} color="error"/>}</Stack>
   <Typography>🪪 {e.curp||'CURP pendiente'}<br/>📞 {e.telefono||'Teléfono pendiente'}<br/>🚩 Inicio: {e.fechaInicio||'Sin fecha'} · ⏰ Solicitud: {e.fechaSolicitud||'Sin fecha'}</Typography>
   <LinearProgress variant="determinate" value={(docs.length/5)*100} color={docs.length===5?'success':'primary'} sx={{height:10,borderRadius:8}}/>
   {docs.length<5&&<Typography color="text.secondary">Faltan: {tipos.filter(t=>!docs.some(a=>a.tipo===t)).join(', ')}</Typography>}
   <Accordion><AccordionSummary><Typography fontWeight={900}>📁 Documentos del cliente · {docs.length}/5</Typography></AccordionSummary><AccordionDetails><Stack spacing={2}>{tipos.map(tipo=>{const a=docs.find(x=>x.tipo===tipo);return <Card key={tipo} variant="outlined"><CardContent><Typography fontWeight={800} mb={1}>{tipo}</Typography>{a?<Box sx={{display:'grid',gridTemplateColumns:'repeat(2,1fr)',gap:1}}><Button size="small" variant="contained" startIcon={<Visibility/>} onClick={()=>ver(a)}>Ver</Button><Button size="small" color="success" variant="contained" startIcon={<WhatsApp/>} onClick={()=>void compartirArchivo(a)}>WhatsApp</Button><Button size="small" variant="outlined" startIcon={<Download/>} onClick={()=>descargar(a)}>Guardar</Button><Button size="small" variant="outlined" startIcon={<Close/>}>Cerrar</Button></Box>:<Chip label="Pendiente"/>}</CardContent></Card>})}</Stack></AccordionDetails></Accordion>
   <TextField select label="Estado de la solicitud de retiro" value={e.estado} onChange={x=>cambiar('estado',x.target.value as EstadoExpediente)}>{['Pendiente','Activa','Realizada','Vencida'].map(v=><MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField>
   {editando&&<Stack spacing={1.5}><TextField label="Nombre completo" value={e.cliente} onChange={x=>cambiar('cliente',x.target.value.toUpperCase())}/><TextField label="CURP" value={e.curp||''} onChange={x=>cambiar('curp',x.target.value.toUpperCase())}/><TextField label="Contraseña AFORE" value={e.contrasenaAfore||''} onChange={x=>cambiar('contrasenaAfore',x.target.value)}/><TextField label="Teléfono / WhatsApp" value={e.telefono||''} onChange={x=>cambiar('telefono',x.target.value.replace(/\D/g,'').slice(0,10))}/><TextField multiline minRows={3} label="Notas" inputProps={{maxLength:100}} value={e.notas} onChange={x=>cambiar('notas',x.target.value)}/></Stack>}
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'repeat(2,1fr)'},gap:1.2}}><Button variant="outlined" startIcon={<PictureAsPdfRounded/>} onClick={pdf}>Generar PDF</Button><Button color="success" variant="contained" startIcon={<WhatsApp/>} onClick={whatsapp}>Enviar WhatsApp</Button><Button variant="outlined" startIcon={<CallRounded/>} href={`tel:${digits(e.telefono)}`}>Llamar al cliente</Button><Button variant="outlined" startIcon={<ContentCopyRounded/>} onClick={()=>void copiar()}>Copiar CURP</Button>{!editando?<Button variant="contained" startIcon={<EditRounded/>} onClick={()=>setEditando(true)}>Editar expediente</Button>:<Button variant="contained" startIcon={<Save/>} onClick={guardar}>Guardar cambios</Button>}<Button color="error" variant="outlined" startIcon={<Delete/>} onClick={eliminar}>Eliminar expediente</Button></Box>
  </Stack></CardContent></Card>
  <Snackbar open={!!mensaje} autoHideDuration={2500} onClose={()=>setMensaje('')} message={mensaje}/>
 </Stack>;
}
