import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { App } from './App';
import './styles.css';

const router = createBrowserRouter([
  { path: '/', element: <App />, children: [
    { index: true, element: <Navigate to="expedientes" replace /> },
    { path: 'expedientes', lazy: () => import('./pages/Expedientes') },
    { path: 'expedientes/:id', lazy: () => import('./pages/DetalleExpediente') },
    { path: 'configuracion', lazy: () => import('./pages/Configuracion') },
  ] },
], {
  basename: import.meta.env.BASE_URL,
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
