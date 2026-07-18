import BackupRoundedIcon from '@mui/icons-material/BackupRounded';
import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import MenuIcon from '@mui/icons-material/Menu';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import SettingsIcon from '@mui/icons-material/Settings';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { AppBar, Box, Container, CssBaseline, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, Stack, Toolbar, Typography } from '@mui/material';
import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';

const menuItems = [
  { label: 'Inicio', to: '/', icon: <HomeRoundedIcon /> },
  { label: 'Nuevo expediente', to: '/expedientes?nuevo=1', icon: <PersonAddAltRoundedIcon /> },
  { label: 'Todos los expedientes', to: '/expedientes', icon: <FolderSharedIcon /> },
  { label: 'Agenda mensual', to: '/agenda', icon: <CalendarMonthRoundedIcon /> },
  { label: 'Respaldos', to: '/respaldos', icon: <BackupRoundedIcon /> },
  { label: 'Errores AFORE', to: '/errores-afore', icon: <WarningAmberIcon /> },
  { label: 'Configuración', to: '/configuracion', icon: <SettingsIcon /> },
];

export function App() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  return <>
    <CssBaseline />
    <AppBar position="sticky" elevation={1} sx={{ bgcolor: '#0d1730' }}>
      <Toolbar>
        <IconButton color="inherit" edge="start" aria-label="Abrir menú" onClick={() => setDrawerOpen(true)} sx={{ mr: 1 }}><MenuIcon /></IconButton>
        <FolderSharedIcon sx={{ mr: 1.2 }} />
        <Stack sx={{ flexGrow: 1, lineHeight: 1.05 }}>
          <Typography variant="h6" sx={{ fontWeight: 900 }}>Expedientes</Typography>
          <Typography variant="caption" sx={{ opacity: .86, fontWeight: 600 }}>Diego Obed Valdes Guerrero</Typography>
        </Stack>
      </Toolbar>
    </AppBar>
    <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)}>
      <Box sx={{ width: 300 }} role="navigation">
        <Box sx={{ px: 2.5, py: 2.25 }}><Typography variant="h6" fontWeight={900}>Expedientes</Typography><Typography variant="body2" color="text.secondary">Diego Obed Valdes Guerrero</Typography></Box>
        <Divider />
        <List>{menuItems.map((item) => <ListItemButton key={item.to} component={NavLink} to={item.to} end={item.to === '/'} onClick={() => setDrawerOpen(false)} sx={{ '&.active': { bgcolor: 'action.selected', color: 'primary.main', '& .MuiListItemIcon-root': { color: 'primary.main' } } }}><ListItemIcon>{item.icon}</ListItemIcon><ListItemText primary={item.label} /></ListItemButton>)}</List>
      </Box>
    </Drawer>
    <Box component="main" sx={{ py: 2.5, minHeight: 'calc(100vh - 64px)', bgcolor: '#f4f7fb' }}><Container maxWidth="lg"><Outlet /></Container></Box>
  </>;
}