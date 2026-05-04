import React from 'react';
import styles from './PanelShared.module.css';

function StockHealthPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    const alerts = data.filter(d => d.requiresRestock);
    const ok = data.filter(d => !d.requiresRestock);

    return (
        <div className={styles.summaryGrid}>
            <div className={`${styles.summaryCard} ${styles.danger}`}>
                <span className={styles.summaryValue}>{alerts.length}</span>
                <span className={styles.summaryLabel}>Reposição necessária</span>
            </div>
            <div className={`${styles.summaryCard} ${styles.ok}`}>
                <span className={styles.summaryValue}>{ok.length}</span>
                <span className={styles.summaryLabel}>Estoque saudável</span>
            </div>
        </div>
    );
}

function StockHealthTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Produto</th>
                        <th>Categoria</th>
                        <th>Estoque atual</th>
                        <th>Mínimo previsto</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.productId} className={d.requiresRestock ? styles.rowDanger : ''}>
                            <td>{d.productName}</td>
                            <td>{d.category}</td>
                            <td>{d.currentStock}</td>
                            <td>{d.predictedMinimum}</td>
                            <td>{d.requiresRestock ? '⚠️ Repor' : '✅ OK'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { StockHealthPanel, StockHealthTable };
