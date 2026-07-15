import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material';
import { onAuthStateChanged, signInWithEmailAndPassword, signOut, type User } from 'firebase/auth';
import { useEffect, useState, type ReactNode } from 'react';
import { auth } from './firebase';

export function AuthGate({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<User | null>(null);
  const [cargando, setCargando] = useState(true);
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => onAuthStateChanged(auth, (actual) => { setUsuario(actual); setCargando(false); }), []);

  const entrar = async () => {
    setError(''); setEnviando(true);
    try { await signInWithEmailAndPassword(auth, correo.trim(), contrasena); }
    catch { setError('No fue posible iniciar sesión. Revisa el correo y la contraseña.'); }
    finally { setEnviando(false); }
  };

  if (cargando) return <Box minHeight="70vh" display="grid" sx={{ placeItems: 'center' }}><CircularProgress /></Box>;

  if (!usuario) return <Box minHeight="75vh" display="grid" sx={{ placeItems: 'center', px: 2 }}><Paper sx={{ p: 3, width: '100%', maxWidth: 430 }}><Stack spacing={2}><Typography variant="h4" fontWeight={700}>Acceso privado</Typography><Typography color="text.secondary">Inicia sesión con el usuario que creaste en Firebase.</Typography>{error && <Alert severity="error">{error}</Alert>}<TextField label="Correo electrónico" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} /><TextField label="Contraseña" type="password" value={contrasena} onChange={(e) => setContrasena(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && void entrar()} /><Button variant="contained" disabled={enviando || !correo || !contrasena} onClick={() => void entrar()}>{enviando ? 'Ingresando…' : 'Iniciar sesión'}</Button><Alert severity="info">No existe registro público. Solo pueden entrar usuarios creados desde tu consola de Firebase.</Alert></Stack></Paper></Box>;

  return <>{children}<Button size="small" onClick={() => void signOut(auth)} sx={{ position: 'fixed', right: 12, bottom: 12, zIndex: 1300 }} variant="outlined">Cerrar sesión</Button></>;
}
