import React from 'react';
import styles from './PanelShared.module.css';

function BarberSkillMatrixPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const byBarber = data.reduce((acc, d) => {
        if (!acc[d.barberId]) acc[d.barberId] = { name: d.barberName, activities: [] };
        acc[d.barberId].activities.push(d);
        return acc;
    }, {});

    return (
        <div className={styles.skillGrid}>
            {Object.entries(byBarber).map(([id, b]) => (
                <div key={id} className={styles.skillCard}>
                    <p className={styles.skillBarberName}>{b.name}</p>
                    {b.activities.map(a => (
                        <div key={a.activityName} className={styles.skillRow}>
                            <span className={styles.skillActivity}>{a.activityName}</span>
                            <span className={styles.skillTimes}>{a.timesExecuted}×</span>
                        </div>
                    ))}
                </div>
            ))}
        </div>
    );
}

function BarberSkillMatrixTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Barbeiro</th>
                        <th>Atividade</th>
                        <th>Execuções</th>
                        <th>Receita Gerada (R$)</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((d, i) => (
                        <tr key={`${d.barberId}-${d.activityName}-${i}`}>
                            <td>{d.barberName}</td>
                            <td>{d.activityName}</td>
                            <td>{d.timesExecuted}</td>
                            <td>{Number(d.totalGeneratedByActivity).toFixed(2)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { BarberSkillMatrixPanel, BarberSkillMatrixTable };
