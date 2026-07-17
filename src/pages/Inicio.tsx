import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CloudDoneOutlinedIcon from '@mui/icons-material/CloudDoneOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import FolderSharedOutlinedIcon from '@mui/icons-material/FolderSharedOutlined';
import SearchIcon from '@mui/icons-material/Search';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { Box, Button, Card, CardActionArea, CardContent, Chip, Grid, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

const modules = [
  {
    title: 'Expedientes',
    description: 'Consulta, registra y administra la información de tus clientes.',
    icon: <FolderSharedOutlinedIcon fontSize="large" />,
    to: '/expedientes',
    status: 'Disponible',
  },
  {
    title: 'Documentos',
    description: 'Base preparada para fotografías, imágenes y archivos PDF.',
    icon: <DescriptionOutlinedIcon fontSize="large" />,
    to: '/expedientes',
    status: 'Preparado',
  },
  {
    title: 'Errores AFORE',
    description: 'Consulta el catálogo de errores y sus posibles soluciones.',
    icon: <WarningAmberOutlinedIcon fontSize="large" />,
    to: '/errores-afore',
    status: 'Disponible',
  },
  {
    title: 'Sincronización',
    description: 'Configuración de Firebase y respaldo de información.',
    icon: <CloudDoneOutlinedIcon fontSize="large" />,
    to: '/configuracion',
    status: 'Configurable',
  },
];

export function Component() {
  return (
    <Stack spacing={3}>
      <Box
        sx={{
          p: { xs: 2.5, sm: 4 },
          borderRadius: 4,
          background: 'linear-gradient(135deg, #12355b 0%, #2563a6 100%)',
          color: 'white',
          boxShadow: 3,
        }}
      >
        <Typography variant="overline" sx={{ opacity: 0.85 }}>
          Aplicación Android · Versión inicial
        </Typography>
        <Typography variant="h4" fontWeight={800} gutterBottom>
          Nuevo Proyecto Android
        </Typography>
        <Typography sx={{ maxWidth: 680, opacity: 0.9, mb: 2.5 }}>
          Base de trabajo para administrar clientes, expedientes, documentos y trámites desde tu teléfono.
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
          <Button component={RouterLink} to="/expedientes" variant="contained" color="secondary" startIcon={<SearchIcon />}>
            Ver expedientes
          </Button>
          <Button component={RouterLink} to="/expedientes" variant="outlined" startIcon={<AddCircleOutlineIcon />} sx={{ color: 'white', borderColor: 'rgba(255,255,255,.7)' }}>
            Registrar cliente
          </Button>
        </Stack>
      </Box>

      <Box>
        <Typography variant="h5" fontWeight={800}>Módulos principales</Typography>
        <Typography color="text.secondary">Selecciona una opción para comenzar.</Typography>
      </Box>

      <Grid container spacing={2}>
        {modules.map((module) => (
          <Grid key={module.title} size={{ xs: 12, sm: 6 }}>
            <Card sx={{ height: '100%', borderRadius: 3 }} variant="outlined">
              <CardActionArea component={RouterLink} to={module.to} sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                    <Box sx={{ color: 'primary.main' }}>{module.icon}</Box>
                    <Chip size="small" label={module.status} />
                  </Stack>
                  <Typography variant="h6" fontWeight={750} mt={2}>{module.title}</Typography>
                  <Typography color="text.secondary" mt={0.5}>{module.description}</Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Stack>
  );
}
