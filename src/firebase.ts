import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: 'AIzaSyA_pvV6d-sr7OpeBdRuf1w5uhmXTa6EE9c',
  authDomain: 'control-expedientes-privados.firebaseapp.com',
  projectId: 'control-expedientes-privados',
  storageBucket: 'control-expedientes-privados.firebasestorage.app',
  messagingSenderId: '939272933933',
  appId: '1:939272933933:web:c53508c545367c05f71ea4',
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
