import React from 'react';
import styles from './PanelShared.module.css';

function AgendaThermometerPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    const maxTotal = Math.max(...data.map(d => d.totalAppointments || 0), 1);

    return (
        <div className={styles.thermometerList}>
            {data.slice(-14).map(d => {
                const fill = (d.totalAppointments / maxTotal) * 100;
                const lostPct = d.totalAppointments > 0 ? (d.lostAppointments / d.totalAppointments) * 100 : 0;
                return (
                    <div key={d.agendaDate} className={styles.thermoRow}>
                        <span className={styles.thermoDate}>{d.agendaDate}</span>
                        <div className={styles.thermoBarTrack}>
                            <div className={styles.thermoBarActive} style={{ width: `${fill - lostPct * fill / 100}%` }} />
                            <div className={styles.thermoBarLost} style={{ width: `${lostPct * fill / 100}%` }} />
                        </div>
                        <span className={styles.thermoCount}>{d.totalAppointments}</span>
                    </div>
                );
            })}
            <div className={styles.thermoLegend}>
                <span className={styles.legendActive}>■ Ativos</span>
                <span className={styles.legendLost}>■ Perdidos</span>
            </div>
        </div>
    );
}

function AgendaThermometerTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Data</th>
                        <th>Total</th>
                        <th>Ativos</th>
                        <th>Perdidos</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.agendaDate}>
                            <td>{d.agendaDate}</td>
                            <td>{d.totalAppointments}</td>
                            <td>{d.activeAppointments}</td>
                            <td>{d.lostAppointments}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { AgendaThermometerPanel, AgendaThermometerTable };
