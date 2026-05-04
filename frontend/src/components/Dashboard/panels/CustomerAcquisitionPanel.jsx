import React from 'react';
import styles from './PanelShared.module.css';

function CustomerAcquisitionPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    const maxVal = Math.max(...data.map(d => d.newCustomers || 0), 1);
    return (
        <div className={styles.barChartList}>
            {data.slice(-12).map(d => (
                <div key={d.referenceMonth} className={styles.barChartRow}>
                    <span className={styles.barChartLabel}>{d.referenceMonth}</span>
                    <div className={styles.barTrack}>
                        <div className={styles.barFill} style={{ width: `${(d.newCustomers / maxVal) * 100}%` }} />
                    </div>
                    <span className={styles.barChartValue}>{d.newCustomers}</span>
                </div>
            ))}
        </div>
    );
}

function CustomerAcquisitionTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead><tr><th>Mês</th><th>Novos Clientes</th></tr></thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.referenceMonth}>
                            <td>{d.referenceMonth}</td>
                            <td>{d.newCustomers}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { CustomerAcquisitionPanel, CustomerAcquisitionTable };
