# Análisis funcional de DV Control de Trámites v1.8.3

APK analizada: `DV-Control-de-Tramites-v1.8.3(7).apk`.

## Arquitectura encontrada

- Interfaz principal web embebida en Android WebView.
- Persistencia local mediante IndexedDB.
- Integración nativa para selector de archivos, cámara, compartir, respaldos y generación de PDF.
- Módulos incrementales de interfaz y comportamiento: `dv-v172` a `dv-v183`, `ui-v1-2` a `ui-v1-7`, agenda, respaldo, acciones y licencia.

## Funciones principales identificadas

### Acceso y apariencia
- Acceso protegido mediante PIN.
- Cambio entre tema claro y oscuro.
- Pantalla de licencia y administración.

### Expedientes
- Crear expediente.
- Editar expediente.
- Eliminar expediente.
- Buscar por nombre, CURP o teléfono.
- Filtrar todos, próximos, vencidos e incompletos.
- Ordenar expedientes y documentos.
- Mostrar fecha de creación y actualización.

### Datos del cliente
- Fotografía tomada con cámara o seleccionada desde archivos.
- Nombre completo.
- CURP.
- Teléfono / WhatsApp.
- Contraseña AFORE.
- Notas con límite de caracteres.

### Trámite
- Fecha de inicio.
- Cálculo automático de fecha de solicitud a 46 días.
- Indicadores para hoy, próximos siete días y vencidos.
- Agenda mensual y listado de prioridades.
- Conteos por categoría.

### Documentos
- Identificación frontal.
- Identificación trasera.
- Pagaré.
- Estado de cuenta.
- Constancia fiscal.
- Visualización, ordenamiento y detección de documentos faltantes.

### Acciones
- Abrir WhatsApp del cliente.
- Compartir información.
- Guardar o generar ficha PDF.
- Respaldo manual.
- Respaldo automático.
- Restauración desde archivo JSON.

## Migración al Nuevo Proyecto Android

La migración se realizará por módulos, conservando el comportamiento útil y reemplazando el código incremental por componentes React/TypeScript claros:

1. Expedientes y fechas automáticas.
2. Fotografía y documentos.
3. Agenda, prioridades y filtros.
4. Respaldo y restauración.
5. PDF, compartir y WhatsApp.
6. PIN, licencia y configuración.
