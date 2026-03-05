import React from 'react';
import styles from '../../pages/CSS/BarberHomePage.module.css';

function NoBarbershopPanel({ onCreateShop, onJoinShop }) {
    return (
        <div className={styles.noBarbershopPanel}>
            <h2 className={styles.noBarbershopTitle}>Você ainda não faz parte de uma Barbearia</h2>
            <p className={styles.noBarbershopSubtitle}>Escolha como deseja começar:</p>

            <div className={styles.noBarbershopActions}>
                <button onClick={onCreateShop} className={styles.createShopButton}>
                    🏢 Criar Minha Barbearia
                </button>
                <button onClick={onJoinShop} className={styles.joinShopButton}>
                    🤝 Entrar em Barbearia
                </button>
            </div>
        </div>
    );
}

export default NoBarbershopPanel;
