import { AppBar, Box, Button, Container, CssBaseline, Toolbar, Typography } from '@mui/material';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import { NavLink, Outlet } from 'react-router-dom';

export function App() {
  return <><CssBaseline /><AppBar position="sticky" color="primary"><Toolbar><FolderSharedIcon sx={{mr:1}}/><Typography variant="h6" sx={{flexGrow:1}}>Control de Expedientes</Typography><Button color="inherit" component={NavLink} to="/expedientes">Expedientes</Button><Button color="inherit" component={NavLink} to="/configuracion">Firebase</Button></Toolbar></AppBar><Box component="main" sx={{py:3}}><Container maxWidth="lg"><Outlet /></Container></Box></>;
}
