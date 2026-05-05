import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, Cell,
} from 'recharts';
import styles from './PanelShared.module.css';

function CustomerAcquisitionPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const chartData = data.slice(-12).map(d => ({
        mes: d.referenceMonth?.slice(0, 7) ?? d.referenceMonth,
        Novos: d.newCustomers ?? 0,
    }));

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" />
                    <XAxis dataKey="mes" tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <YAxis allowDecimals={false} tick={{ fill: '#a0a0a0', fontSize: 11 }} />
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        labelStyle={{ color: '#fff' }}
                        formatter={v => [v, 'Novos clientes']}
                    />
                    <Bar dataKey="Novos" radius={[4, 4, 0, 0]}>
                        {chartData.map((_, i) => (
                            <Cell key={i} fill={i === chartData.length - 1 ? '#e8b923' : '#c19006'} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
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
