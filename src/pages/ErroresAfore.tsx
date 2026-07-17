import SearchIcon from '@mui/icons-material/Search';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Chip,
  Divider,
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

const frequentSolutions = [
  'Verificar CURP, NSS, RFC, nombre completo y fecha de nacimiento.',
  'Confirmar que no exista otra solicitud o trámite activo.',
  'Actualizar o completar el expediente biométrico directamente con la AFORE.',
  'Repetir la captura facial con buena iluminación, cámara limpia y rostro sin accesorios.',
  'Revisar que los documentos estén completos, vigentes, legibles y en el formato permitido.',
  'Cerrar sesión, actualizar la aplicación y volver a intentar sin duplicar solicitudes.',
  'Conservar el folio y escalar el caso con la AFORE cuando el error persista.',
];

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

  const documentedCount = useMemo(
    () => aforeErrors.filter((item) => item.status === 'Documentado').length,
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
          Investigación y catálogo de apoyo para identificar errores, causas probables y acciones recomendadas.
        </Typography>
      </Box>

      <Paper variant="outlined" sx={{ p: 2.5 }}>
        <Stack spacing={2}>
          <Box>
            <Typography variant="h6">Objetivo de la investigación</Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              Reunir en un solo lugar los códigos y mensajes observados en AforeMóvil, AforeWeb y sistemas AFORE,
              explicando su posible origen y ofreciendo pasos prácticos de atención sin inventar información ni
              presentar como confirmada una causa que todavía requiere validación.
            </Typography>
          </Box>

          <Divider />

          <Box>
            <Typography variant="h6">Resultados de la investigación</Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              Se integró un catálogo de {aforeErrors.length} registros. Actualmente {documentedCount} están marcados
              como documentados y {aforeErrors.length - documentedCount} permanecen en investigación. Los errores se
              organizan por plataforma, categoría, proceso, nivel de confianza y número de reportes localizados.
            </Typography>
          </Box>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} useFlexGap flexWrap="wrap">
            <Chip label={`${aforeErrors.length} códigos registrados`} color="primary" />
            <Chip label={`${categories.length - 1} categorías`} />
            <Chip label="AforeMóvil · AforeWeb · Sistema AFORE" variant="outlined" />
          </Stack>
        </Stack>
      </Paper>

      <Accordion disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Soluciones más frecuentes encontradas</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box component="ol" sx={{ mt: 0, mb: 0, pl: 2.5 }}>
            {frequentSolutions.map((solution) => (
              <li key={solution}><Typography color="text.secondary" sx={{ mb: 0.75 }}>{solution}</Typography></li>
            ))}
          </Box>
        </AccordionDetails>
      </Accordion>

      <Alert severity="info">
        Este catálogo es una guía de apoyo. La AFORE correspondiente debe confirmar la causa definitiva y la solución
        oficial de cada caso. Cuando un registro aparece como “En investigación”, requiere evidencia adicional.
      </Alert>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Catálogo completo</Typography>
          <TextField
            fullWidth
            label="Buscar error"
            placeholder="Ejemplo: MC G04, D96, CURP, selfie, token, sesión o documentos"
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
                    <Typography variant="subtitle2">Solución recomendada</Typography>
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
