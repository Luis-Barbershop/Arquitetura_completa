import React from 'react';
import styles from '../../pages/CSS/BarberHomePage.module.css';

function NoBarbershopPanel({ onCreateShop, onGoToProfile }) {
    return (
        <section className={styles.noBarbershopPanel}>
            <p className={styles.noBarbershopKicker}>PAINEL DO PROFISSIONAL</p>
            <h2 className={styles.noBarbershopTitle}>Voce ainda nao esta vinculado a uma barbearia</h2>
            <p className={styles.noBarbershopSubtitle}>Crie seu proprio espaco ou aguarde o convite de um dono de barbearia pelo seu CPF.</p>

            <div className={styles.noBarbershopActions}>
                <article className={styles.actionCard}>
                    <h3>Criar minha barbearia</h3>
                    <p>Ideal para quem vai gerenciar equipe, servicos e agenda completa.</p>
                    <button onClick={onCreateShop} className={styles.createShopButton}>
                        Criar agora
                    </button>
                </article>

                <article className={styles.actionCard}>
                    <h3>Aguardando convite?</h3>
                    <p>Verifique se ha convites pendentes no seu perfil.</p>
                    <button onClick={onGoToProfile} className={styles.joinShopButton}>
                        Ver meu perfil
                    </button>
                </article>
            </div>

            <p className={styles.noBarbershopHint}>Depois da vinculacao, seu menu completo sera liberado automaticamente.</p>
        </section>
    );
}

export default NoBarbershopPanel;
