import FolderSharedIcon from '@mui/icons-material/FolderShared';
import MenuIcon from '@mui/icons-material/Menu';
import SettingsIcon from '@mui/icons-material/Settings';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {
  AppBar,
  Box,
  Container,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { AuthGate } from './AuthGate';

const menuItems = [
  { label: 'Expedientes', to: '/expedientes', icon: <FolderSharedIcon /> },
  { label: 'Errores AFORE', to: '/errores-afore', icon: <WarningAmberIcon /> },
  { label: 'Firebase', to: '/configuracion', icon: <SettingsIcon /> },
];

export function App() {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <>
      <CssBaseline />
      <AuthGate>
        <AppBar position="sticky" color="primary">
          <Toolbar>
            <IconButton
              color="inherit"
              edge="start"
              aria-label="Abrir menú"
              onClick={() => setDrawerOpen(true)}
              sx={{ mr: 1 }}
            >
              <MenuIcon />
            </IconButton>
            <FolderSharedIcon sx={{ mr: 1 }} />
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              DV Control de Trámites
            </Typography>
            <Typography variant="caption" sx={{ display: { xs: 'none', sm: 'block' } }}>
              v2.1.0
            </Typography>
          </Toolbar>
        </AppBar>

        <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)}>
          <Box sx={{ width: 285 }} role="navigation">
            <Box sx={{ px: 2.5, py: 2.25 }}>
              <Typography variant="h6">DV Control de Trámites</Typography>
              <Typography variant="body2" color="text.secondary">Menú principal</Typography>
            </Box>
            <Divider />
            <List>
              {menuItems.map((item) => (
                <ListItemButton
                  key={item.to}
                  component={NavLink}
                  to={item.to}
                  onClick={() => setDrawerOpen(false)}
                  sx={{
                    '&.active': {
                      bgcolor: 'action.selected',
                      color: 'primary.main',
                      '& .MuiListItemIcon-root': { color: 'primary.main' },
                    },
                  }}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              ))}
            </List>
          </Box>
        </Drawer>

        <Box component="main" sx={{ py: 3 }}>
          <Container maxWidth="lg">
            <Outlet />
          </Container>
        </Box>
      </AuthGate>
    </>
  );
}
