import React from 'react';
import styles from '../../pages/CSS/BarberHomePage.module.css';

function NoBarbershopPanel({ onCreateShop, onJoinShop }) {
    return (
        <section className={styles.noBarbershopPanel}>
            <p className={styles.noBarbershopKicker}>PAINEL DO PROFISSIONAL</p>
            <h2 className={styles.noBarbershopTitle}>Voce ainda nao esta vinculado a uma barbearia</h2>
            <p className={styles.noBarbershopSubtitle}>Comece criando seu proprio espaco ou solicitando entrada em uma equipe existente.</p>

            <div className={styles.noBarbershopActions}>
                <article className={styles.actionCard}>
                    <h3>Criar minha barbearia</h3>
                    <p>Ideal para quem vai gerenciar equipe, servicos e agenda completa.</p>
                    <button onClick={onCreateShop} className={styles.createShopButton}>
                        Criar agora
                    </button>
                </article>

                <article className={styles.actionCard}>
                    <h3>Entrar em uma barbearia</h3>
                    <p>Ja trabalha em uma equipe? Envie o CNPJ para solicitar vinculacao.</p>
                    <button onClick={onJoinShop} className={styles.joinShopButton}>
                        Solicitar entrada
                    </button>
                </article>
            </div>

            <p className={styles.noBarbershopHint}>Depois da vinculacao, seu menu completo sera liberado automaticamente.</p>
        </section>
    );
}

export default NoBarbershopPanel;
