import React, { useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import styles from './PanelShared.module.css';

const COLORS = ['#c19006', '#e8b923', '#a07005', '#f5d76e', '#7d5604', '#3b82f6', '#10b981'];
const BRL = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

function BarberSkillMatrixPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const barberIds = [...new Set(data.map(d => d.barberId))];
    const [selectedBarber, setSelectedBarber] = useState(barberIds[0]);

    const barberName = data.find(d => d.barberId === selectedBarber)?.barberName ?? 'Barbeiro';
    const chartData = data
        .filter(d => d.barberId === selectedBarber)
        .map(d => ({ name: d.activityName, Execuções: d.timesExecuted }));

    return (
        <div className={styles.chartContainer}>
            {barberIds.length > 1 && (
                <div className={styles.tabRow}>
                    {barberIds.map(id => {
                        const name = data.find(d => d.barberId === id)?.barberName ?? id;
                        return (
                            <button
                                key={id}
                                type="button"
                                className={id === selectedBarber ? styles.tabBtnActive : styles.tabBtn}
                                onClick={() => setSelectedBarber(id)}
                            >
                                {name.split(' ')[0]}
                            </button>
                        );
                    })}
                </div>
            )}
            <p className={styles.chartSubtitle}>{barberName}</p>
            <ResponsiveContainer width="100%" height={190}>
                <BarChart data={chartData} margin={{ top: 4, right: 12, left: 0, bottom: 4 }} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" horizontal={false} />
                    <XAxis type="number" allowDecimals={false} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <YAxis type="category" dataKey="name" width={90} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        labelStyle={{ color: '#fff' }}
                        formatter={v => [v, 'Execuções']}
                    />
                    <Bar dataKey="Execuções" radius={[0, 4, 4, 0]}>
                        {chartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}

function BarberSkillMatrixTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr><th>Barbeiro</th><th>Atividade</th><th>Execuções</th><th>Receita (R$)</th></tr>
                </thead>
                <tbody>
                    {data.map((d, i) => (
                        <tr key={`${d.barberId}-${d.activityName}-${i}`}>
                            <td>{d.barberName}</td>
                            <td>{d.activityName}</td>
                            <td>{d.timesExecuted}</td>
                            <td>{BRL(d.totalGeneratedByActivity)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { BarberSkillMatrixPanel, BarberSkillMatrixTable };
