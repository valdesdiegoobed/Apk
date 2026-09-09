# VirtualCam Shizuku Check 1.2.0

Aplicación Android de diagnóstico para el proyecto de cámara virtual.

## Qué comprueba

- Si Shizuku está activo.
- Si la app fue autorizada por Shizuku.
- UID del servidor: `2000` = ADB shell, `0` = root/Sui.
- Contexto SELinux y versión del servidor Shizuku.
- Permisos remotos relevantes.
- Cámaras que Camera2 expone a una app normal.
- UserService de Shizuku con identidad shell/root.
- `service list`, `/dev/video*`, propiedades de cámara y resumen de `dumpsys media.camera`.

## Qué NO hace todavía

Esta versión no registra una cámara virtual en Android. Sirve para saber si el teléfono permite avanzar con Shizuku solamente o si será necesario root/Sui/Magisk o integración a nivel Camera Provider/HAL.

## Uso

1. Inicia Shizuku mediante depuración inalámbrica.
2. Instala esta APK.
3. Pulsa **AUTORIZAR CON SHIZUKU** y acepta el permiso en Shizuku.
4. Pulsa **EJECUTAR DIAGNÓSTICO COMPLETO**.
5. Pulsa **COPIAR REPORTE** y conserva el resultado para la siguiente etapa del proyecto.

Dependencias oficiales: `dev.rikka.shizuku:api:13.1.5` y `provider:13.1.5`.
