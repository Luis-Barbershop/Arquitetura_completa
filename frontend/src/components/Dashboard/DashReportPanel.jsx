import React, { useEffect, useRef, useState } from 'react';
import styles from './DashReportPanel.module.css';

/**
 * Painel dividido Dash / Relatório.
 * Props:
 *   title        - string (título do painel)
 *   dashContent  - ReactNode (gráficos/cards)
 *   reportContent - ReactNode (tabela/lista)
 *   onRefresh    - função assíncrona chamada no intervalo
 *   refreshInterval - ms (padrão 30000)
 */
function DashReportPanel({ title, dashContent, reportContent, onRefresh, refreshInterval = 30000 }) {
    const [dashVisible, setDashVisible] = useState(true);
    const [reportVisible, setReportVisible] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const intervalRef = useRef(null);

    const doRefresh = async () => {
        if (!onRefresh) return;
        setRefreshing(true);
        try { await onRefresh(); } finally { setRefreshing(false); }
    };

    useEffect(() => {
        if (!onRefresh) return;
        intervalRef.current = setInterval(doRefresh, refreshInterval);
        return () => clearInterval(intervalRef.current);
    }, [onRefresh, refreshInterval]);

    const bothHidden = !dashVisible && !reportVisible;

    return (
        <section className={styles.panelWrapper}>
            <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>{title}</h2>
                <div className={styles.panelControls}>
                    <button
                        className={`${styles.toggleBtn} ${!dashVisible ? styles.inactive : ''}`}
                        onClick={() => setDashVisible(v => !v)}
                    >
                        {dashVisible ? 'Ocultar Dashboard' : 'Mostrar Dashboard'}
                    </button>
                    <button
                        className={`${styles.toggleBtn} ${!reportVisible ? styles.inactive : ''}`}
                        onClick={() => setReportVisible(v => !v)}
                    >
                        {reportVisible ? 'Ocultar Relatório' : 'Mostrar Relatório'}
                    </button>
                    {onRefresh && (
                        <button className={styles.refreshBtn} onClick={doRefresh} disabled={refreshing}>
                            {refreshing ? '↻' : '🔄'}
                        </button>
                    )}
                </div>
            </div>

            {bothHidden ? (
                <p className={styles.allHiddenMsg}>Todos os painéis estão ocultos.</p>
            ) : (
                <div className={styles.panelBody}>
                    {dashVisible && (
                        <div className={`${styles.pane} ${!reportVisible ? styles.fullWidth : ''}`}>
                            <p className={styles.paneLabel}>Dashboard</p>
                            {dashContent}
                        </div>
                    )}
                    {reportVisible && (
                        <div className={`${styles.pane} ${!dashVisible ? styles.fullWidth : ''}`}>
                            <p className={styles.paneLabel}>Relatório</p>
                            {reportContent}
                        </div>
                    )}
                </div>
            )}
        </section>
    );
}

export default DashReportPanel;
