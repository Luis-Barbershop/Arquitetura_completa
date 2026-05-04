import React from 'react';
import styles from './PanelShared.module.css';

function BarberPerformancePanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const topRevenue = Math.max(...data.map(d => parseFloat(d.generatedRevenue) || 0));

    return (
        <div className={styles.cardList}>
            {data.map(d => (
                <div key={d.barberId} className={styles.card}>
                    <div className={styles.cardTop}>
                        <span className={styles.cardName}>{d.barberName}</span>
                        <span className={styles.cardBadge}>{Number(d.contributionPercentage).toFixed(1)}%</span>
                    </div>
                    <div className={styles.barTrack}>
                        <div
                            className={styles.barFill}
                            style={{ width: topRevenue > 0 ? `${(parseFloat(d.generatedRevenue) / topRevenue) * 100}%` : '0%' }}
                        />
                    </div>
                    <div className={styles.cardFooter}>
                        <span>{d.totalAppointments} atend.</span>
                        <span>R$ {Number(d.generatedRevenue).toFixed(2)}</span>
                    </div>
                </div>
            ))}
        </div>
    );
}

function BarberPerformanceTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Barbeiro</th>
                        <th>Atendimentos</th>
                        <th>Receita (R$)</th>
                        <th>Contribuição</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.barberId}>
                            <td>{d.barberName}</td>
                            <td>{d.totalAppointments}</td>
                            <td>{Number(d.generatedRevenue).toFixed(2)}</td>
                            <td>{Number(d.contributionPercentage).toFixed(1)}%</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { BarberPerformancePanel, BarberPerformanceTable };
