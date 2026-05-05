import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts';
import styles from './PanelShared.module.css';

const GOLD_SHADES = ['#c19006', '#e8b923', '#a07005', '#f5d76e', '#7d5604'];
const BRL = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

function BarberPerformancePanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const chartData = data.map(d => ({
        name: d.barberName?.split(' ')[0] ?? 'Barbeiro',
        receita: parseFloat(d.generatedRevenue) || 0,
    }));

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" />
                    <XAxis dataKey="name" tick={{ fill: '#a0a0a0', fontSize: 12 }} />
                    <YAxis tickFormatter={v => `R$${(v / 1000).toFixed(0)}k`} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        labelStyle={{ color: '#fff' }}
                        formatter={v => [BRL(v), 'Receita']}
                    />
                    <Bar dataKey="receita" radius={[4, 4, 0, 0]}>
                        {chartData.map((_, i) => (
                            <Cell key={i} fill={GOLD_SHADES[i % GOLD_SHADES.length]} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}

function BarberPerformanceTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const pieData = data.map(d => ({
        name: d.barberName?.split(' ')[0] ?? 'Barbeiro',
        value: parseFloat(d.contributionPercentage) || 0,
    }));

    return (
        <div className={styles.chartWithTable}>
            <div className={styles.pieContainer}>
                <ResponsiveContainer width="100%" height={180}>
                    <PieChart>
                        <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={75}
                            paddingAngle={3} dataKey="value"
                            label={({ name, value }) => `${name} ${value.toFixed(0)}%`}
                        >
                            {pieData.map((_, i) => <Cell key={i} fill={GOLD_SHADES[i % GOLD_SHADES.length]} />)}
                        </Pie>
                        <Tooltip formatter={v => [`${v.toFixed(1)}%`, 'Contribuição']}
                            contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }} />
                    </PieChart>
                </ResponsiveContainer>
            </div>
            <div className={styles.tableWrapper}>
                <table className={styles.table}>
                    <thead><tr><th>Barbeiro</th><th>Atend.</th><th>Receita</th><th>%</th></tr></thead>
                    <tbody>
                        {data.map(d => (
                            <tr key={d.barberId}>
                                <td>{d.barberName}</td>
                                <td>{d.totalAppointments}</td>
                                <td>{BRL(d.generatedRevenue)}</td>
                                <td>{Number(d.contributionPercentage).toFixed(1)}%</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export { BarberPerformancePanel, BarberPerformanceTable };
