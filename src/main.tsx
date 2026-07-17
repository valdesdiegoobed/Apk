import React from 'react';
import ReactDOM from 'react-dom/client';
import { createHashRouter, Navigate, RouterProvider } from 'react-router-dom';
import { App } from './App';
import './styles.css';

if ('serviceWorker' in navigator) {
  void navigator.serviceWorker.getRegistrations().then((registrations) => {
    registrations.forEach((registration) => void registration.unregister());
  });
}

if ('caches' in window) {
  void caches.keys().then((keys) => {
    keys.forEach((key) => void caches.delete(key));
  });
}

const router = createHashRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Navigate to="expedientes" replace /> },
      { path: 'expedientes', lazy: () => import('./pages/Expedientes') },
      { path: 'expedientes/:id', lazy: () => import('./pages/DetalleExpediente') },
      { path: 'errores-afore', lazy: () => import('./pages/ErroresAfore') },
      { path: 'configuracion', lazy: () => import('./pages/Configuracion') },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
