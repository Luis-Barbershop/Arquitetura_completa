import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, Legend,
} from 'recharts';
import styles from './PanelShared.module.css';

function AgendaThermometerPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const chartData = data.slice(-14).map(d => ({
        dia: d.agendaDate?.slice(5) ?? d.agendaDate,
        Ativos: d.activeAppointments ?? 0,
        Perdidos: d.lostAppointments ?? 0,
    }));

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" />
                    <XAxis dataKey="dia" tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <YAxis allowDecimals={false} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        labelStyle={{ color: '#fff' }}
                    />
                    <Legend wrapperStyle={{ fontSize: 12, color: '#a0a0a0' }} />
                    <Bar dataKey="Ativos" stackId="a" fill="#c19006" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="Perdidos" stackId="a" fill="#ef4444" radius={[4, 4, 0, 0]} />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}

function AgendaThermometerTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr><th>Data</th><th>Total</th><th>Ativos</th><th>Perdidos</th></tr>
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
