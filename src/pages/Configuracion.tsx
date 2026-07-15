import { Alert, Card, CardContent, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import { firebaseReady } from '../firebase';

export function Component() {
  return <Stack spacing={3}><Typography variant="h4" sx={{ fontWeight: 700 }}>Configuración de Firebase</Typography><Alert severity={firebaseReady?'success':'warning'}>{firebaseReady?'Variables de Firebase detectadas en el entorno.':'Copia .env.example a .env.local y reemplaza los valores demo por los de tu proyecto Firebase.'}</Alert><Card><CardContent><Typography variant="h6">Pendiente de configurar manualmente</Typography><List><ListItem><ListItemText primary="Authentication" secondary="Habilita proveedor Email/Password y define usuarios autorizados."/></ListItem><ListItem><ListItemText primary="Cloud Firestore" secondary="Crea la base de datos y reglas de seguridad para expedientes por usuario."/></ListItem><ListItem><ListItemText primary="Cloud Storage" secondary="Crea el bucket y reglas para documentos privados."/></ListItem><ListItem><ListItemText primary="Hosting/PWA" secondary="Publica el build y prueba instalación desde Chrome en Android."/></ListItem></List></CardContent></Card></Stack>;
}
