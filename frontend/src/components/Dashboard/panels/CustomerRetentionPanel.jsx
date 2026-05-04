import React from 'react';
import styles from './PanelShared.module.css';

function CustomerRetentionPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    const maxVal = Math.max(...data.map(d => d.returningCustomers || 0), 1);
    return (
        <div className={styles.barChartList}>
            {data.slice(-12).map(d => (
                <div key={d.referenceMonth} className={styles.barChartRow}>
                    <span className={styles.barChartLabel}>{d.referenceMonth}</span>
                    <div className={styles.barTrack}>
                        <div className={`${styles.barFill} ${styles.barFillRetention}`} style={{ width: `${(d.returningCustomers / maxVal) * 100}%` }} />
                    </div>
                    <span className={styles.barChartValue}>{d.returningCustomers}</span>
                </div>
            ))}
        </div>
    );
}

function CustomerRetentionTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead><tr><th>Mês</th><th>Clientes Recorrentes</th></tr></thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.referenceMonth}>
                            <td>{d.referenceMonth}</td>
                            <td>{d.returningCustomers}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { CustomerRetentionPanel, CustomerRetentionTable };
