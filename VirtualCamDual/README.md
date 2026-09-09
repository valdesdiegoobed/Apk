# AforeWeb Dual 1.1.0

Aplicación Android independiente con dos funciones separadas:

1. **AforeWeb en Chrome real**: abre `https://www.aforeweb.com.mx/login` con la app Chrome instalada. Inicio de sesión, permisos y cámara son gestionados por Chrome/Android.
2. **Cámara virtual de prueba**: página interna de WebView que permite elegir una foto o un video y convertirlo en un `MediaStream` de prueba mediante `canvas.captureStream(30)`.

El modo de prueba está deliberadamente aislado: no se inyecta en Chrome ni en AforeWeb.

- Paquete: `com.vaguer.aforewebdual`
- minSdk: 26
- targetSdk: 35
- versionName: 1.1.0
- Sin Firebase, bot ni base de datos.

## Compilar

Con Gradle 8.9 y JDK 17:

```bash
gradle :app:assembleDebug
```

APK esperado: `app/build/outputs/apk/debug/app-debug.apk`.
