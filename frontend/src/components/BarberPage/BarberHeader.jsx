import React from 'react';
import styles from '../../pages/CSS/BarberHomePage.module.css';

function BarberHeader({ barber, onLogout }) {
    return (
        <header className={styles.header}>
          
{/* 
            <div className={styles.headerCenter}>
                <h1 className={styles.headerTitle}>Painel do Profissional</h1>
                <p className={styles.headerWelcome}>Bem-vindo, {barber?.name}</p>
                {barber?.barbershopName && (
                    <span className={styles.headerShopName}>@{barber.barbershopName}</span>
                )}
            </div> */}

            <div className={styles.headerleft}>
                <h2 className={styles.headerTitle}>CortaAÍ</h2>
                <p className={styles.headerWelcome}>Olá, {barber?.name}! 👋</p>
            </div>

            <div className={styles.headerRight}>
                <button className={styles.notificationButton}>
                    <img src="./Icons/bellicon.png" alt="Sino de Notificação" />
                </button>
                <button onClick={onLogout} className={styles.logoutButton}>
                    Sair ➜
                </button>
            </div>
        </header>
    );
}

export default BarberHeader;
