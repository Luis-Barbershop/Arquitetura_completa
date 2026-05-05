import React from 'react';
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer,
} from 'recharts';
import styles from './PanelShared.module.css';

function CustomerRetentionPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const chartData = data.slice(-12).map(d => ({
        mes: d.referenceMonth?.slice(0, 7) ?? d.referenceMonth,
        Recorrentes: d.returningCustomers ?? 0,
    }));

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={chartData} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
                    <defs>
                        <linearGradient id="retGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" />
                    <XAxis dataKey="mes" tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <YAxis allowDecimals={false} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        labelStyle={{ color: '#fff' }}
                        formatter={v => [v, 'Recorrentes']}
                    />
                    <Area type="monotone" dataKey="Recorrentes" stroke="#3b82f6" fill="url(#retGradient)" strokeWidth={2} dot={{ fill: '#3b82f6', r: 3 }} />
                </AreaChart>
            </ResponsiveContainer>
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
