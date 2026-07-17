import SearchIcon from '@mui/icons-material/Search';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Chip,
  InputAdornment,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useMemo, useState } from 'react';
import { aforeErrors, type Confidence } from '../data/aforeErrors';

const confidenceColor: Record<Confidence, 'success' | 'warning' | 'default'> = {
  Alta: 'success',
  Media: 'warning',
  Baja: 'default',
};

function normalize(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

export function Component() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('Todas');
  const [platform, setPlatform] = useState('Todas');

  const categories = useMemo(
    () => ['Todas', ...Array.from(new Set(aforeErrors.map((item) => item.category))).sort()],
    [],
  );

  const results = useMemo(() => {
    const normalizedQuery = normalize(query);
    return aforeErrors.filter((item) => {
      const haystack = normalize([
        item.code,
        item.message,
        item.app,
        item.category,
        item.process,
        item.afore,
        item.cause,
        item.solution.join(' '),
        item.keywords.join(' '),
      ].join(' '));

      const matchesQuery = !normalizedQuery || haystack.includes(normalizedQuery);
      const matchesCategory = category === 'Todas' || item.category === category;
      const matchesPlatform = platform === 'Todas' || item.app === platform;
      return matchesQuery && matchesCategory && matchesPlatform;
    });
  }, [query, category, platform]);

  return (
    <Stack spacing={2.5}>
      <Box>
        <Stack direction="row" spacing={1} alignItems="center">
          <WarningAmberIcon color="warning" />
          <Typography variant="h4" component="h1">Errores AFORE</Typography>
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
          Busca por código, mensaje, trámite o palabras como CURP, selfie, token, sesión o documentos.
        </Typography>
      </Box>

      <Alert severity="info">
        Este catálogo es una guía de apoyo basada en evidencia pública. Cuando un registro está “En investigación”, el mensaje o la causa aún necesitan confirmación adicional.
      </Alert>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Stack spacing={2}>
          <TextField
            fullWidth
            label="Buscar error"
            placeholder="Ejemplo: MC G04, no detecta mi rostro, CURP, sesión..."
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              select
              fullWidth
              label="Categoría"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              {categories.map((item) => <MenuItem key={item} value={item}>{item}</MenuItem>)}
            </TextField>
            <TextField
              select
              fullWidth
              label="Plataforma"
              value={platform}
              onChange={(event) => setPlatform(event.target.value)}
            >
              {['Todas', 'AforeMóvil', 'AforeWeb', 'Sistema AFORE'].map((item) => (
                <MenuItem key={item} value={item}>{item}</MenuItem>
              ))}
            </TextField>
          </Stack>
        </Stack>
      </Paper>

      <Typography variant="subtitle1" fontWeight={700}>
        {results.length} resultado{results.length === 1 ? '' : 's'}
      </Typography>

      {results.length === 0 ? (
        <Alert severity="warning">No encontramos coincidencias. Prueba con una parte del código o con una palabra relacionada.</Alert>
      ) : (
        <Stack spacing={1.5}>
          {results.map((item) => (
            <Accordion key={item.id} disableGutters>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ width: '100%', pr: 1 }}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'flex-start', sm: 'center' }}>
                    <Typography variant="h6" sx={{ minWidth: 110 }}>{item.code}</Typography>
                    <Typography sx={{ flexGrow: 1 }}>{item.message}</Typography>
                    <Chip size="small" label={item.status} color={item.status === 'Documentado' ? 'primary' : 'default'} />
                  </Stack>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Stack spacing={2}>
                  <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                    <Chip label={item.app} />
                    <Chip label={item.category} />
                    <Chip label={`Confianza ${item.confidence}`} color={confidenceColor[item.confidence]} />
                    <Chip label={`${item.reports} reportes localizados`} variant="outlined" />
                  </Stack>

                  <Box>
                    <Typography variant="subtitle2">AFORE y proceso</Typography>
                    <Typography color="text.secondary">{item.afore} · {item.process}</Typography>
                  </Box>

                  <Box>
                    <Typography variant="subtitle2">Causa probable</Typography>
                    <Typography color="text.secondary">{item.cause}</Typography>
                  </Box>

                  <Box>
                    <Typography variant="subtitle2">Qué hacer</Typography>
                    <Box component="ol" sx={{ mt: 0.5, mb: 0, pl: 2.5 }}>
                      {item.solution.map((step) => <li key={step}><Typography color="text.secondary">{step}</Typography></li>)}
                    </Box>
                  </Box>
                </Stack>
              </AccordionDetails>
            </Accordion>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
