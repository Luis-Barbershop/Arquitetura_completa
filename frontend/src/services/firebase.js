import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';

const firebaseConfig = {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyBTiE5tjRLXvFZ4TizPSeb4GOjAPGkk9Pc",
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "cortaai-480b8.firebaseapp.com",
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "cortaai-480b8",
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "cortaai-480b8.firebasestorage.app",
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "674825342051",
    appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:674825342051:web:2a80d756d8196b30ddbc56",
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });
export default app;
