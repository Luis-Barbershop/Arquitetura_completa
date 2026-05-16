import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, Legend,
} from 'recharts';
import styles from './PanelShared.module.css';

function AgendaThermometerPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const toStatusCounts = (d) => {
        const total = d.totalAppointments ?? 0;
        const active = d.activeAppointments ?? 0;
        const walkin = d.walkinAppointments ?? 0;
        const pending = d.pendingAppointments ?? 0;
        const lost = d.lostAppointments ?? 0;
        const completed = d.completedAppointments ?? Math.max(total - active - walkin - pending - lost, 0);

        return { active, walkin, pending, completed, lost };
    };

    const chartData = data.slice(-14).map(d => {
        const counts = toStatusCounts(d);
        return {
            dia: d.agendaDate?.slice(5) ?? d.agendaDate,
            Ativos: counts.active,
            Pendentes: counts.pending,
            Encaixes: counts.walkin,
            Concluidos: counts.completed,
            Perdidos: counts.lost,
        };
    });

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
                    <Bar dataKey="Pendentes" stackId="a" fill="#f97316" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="Encaixes" stackId="a" fill="#7c3aed" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="Concluidos" stackId="a" fill="#10b981" radius={[0, 0, 0, 0]} />
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
                    <tr><th>Data</th><th>Total</th><th>Ativos</th><th>Pendentes</th><th>Encaixes</th><th>Concluídos</th><th>Perdidos</th></tr>
                </thead>
                <tbody>
                    {data.map(d => {
                        const total = d.totalAppointments ?? 0;
                        const active = d.activeAppointments ?? 0;
                        const walkin = d.walkinAppointments ?? 0;
                        const pending = d.pendingAppointments ?? 0;
                        const lost = d.lostAppointments ?? 0;
                        const completed = d.completedAppointments ?? Math.max(total - active - walkin - pending - lost, 0);

                        return (
                            <tr key={d.agendaDate}>
                                <td>{d.agendaDate}</td>
                                <td>{total}</td>
                                <td>{active}</td>
                                <td>{pending}</td>
                                <td>{walkin}</td>
                                <td>{completed}</td>
                                <td>{lost}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}

export { AgendaThermometerPanel, AgendaThermometerTable };
