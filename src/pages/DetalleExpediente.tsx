import { CloudUpload, Lock } from '@mui/icons-material';
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Stack, Typography } from '@mui/material';
import { Link, useParams } from 'react-router-dom';
import { expedientesDemo } from '../data';

export function Component() {
  const { id } = useParams();
  const expediente = expedientesDemo.find((item) => item.id === id) ?? expedientesDemo[0];
  return <Stack spacing={3}><Button component={Link} to="/expedientes" sx={{alignSelf:'flex-start'}}>← Volver</Button><Card><CardContent><Stack spacing={2}><Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}><Typography variant="h4" sx={{ fontWeight: 700 }}>{expediente.cliente}</Typography><Chip label={expediente.estado}/></Stack><Typography color="text.secondary">Folio {expediente.id} · Categoría {expediente.categoria}</Typography><Alert severity="info" icon={<Lock />}>La pantalla está preparada para Firebase Authentication, Firestore y Storage. Los registros visibles son ficticios.</Alert><Divider/><Box><Typography variant="h6">Resumen</Typography><Typography>{expediente.notas}</Typography></Box><Box><Typography variant="h6">Documentos</Typography><Typography color="text.secondary">{expediente.documentos} archivos simulados. La carga real se habilitará al configurar Cloud Storage.</Typography><Button variant="contained" startIcon={<CloudUpload />} sx={{mt:1}}>Subir documento ficticio</Button></Box></Stack></CardContent></Card></Stack>;
}
