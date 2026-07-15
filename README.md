# Control de Expedientes

Primera versión funcional de una PWA en español para administrar expedientes privados de clientes con datos ficticios.

## Funcionalidades incluidas

- React + TypeScript + Vite.
- Material UI y React Router.
- Listado y detalle de expedientes ficticios.
- Preparación de Firebase Authentication, Cloud Firestore y Cloud Storage mediante variables de entorno.
- Manifest y service worker para instalación como PWA en Android.

## Desarrollo local

```bash
npm install
cp .env.example .env.local
npm run dev
```

## Verificaciones

```bash
npm run build
npm run lint
```

## Configuración manual de Firebase

1. Crear un proyecto en Firebase Console.
2. Registrar una aplicación web y copiar sus valores a `.env.local` usando `.env.example` como plantilla.
3. Habilitar Firebase Authentication con Email/Password y crear usuarios autorizados.
4. Crear Cloud Firestore en modo producción y definir reglas para que cada usuario acceda únicamente a sus expedientes.
5. Crear Cloud Storage y reglas privadas para documentos de expedientes.
6. Publicar `dist/` en Firebase Hosting u otro hosting HTTPS para probar la instalación PWA desde Android.

No se incluyen credenciales reales ni datos personales reales en este repositorio.
