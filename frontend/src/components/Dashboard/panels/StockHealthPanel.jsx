import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import styles from './PanelShared.module.css';

function StockHealthPanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;

    const alerts = data.filter(d => d.requiresRestock).length;
    const ok = data.filter(d => !d.requiresRestock).length;

    const pieData = [
        { name: 'Saudável', value: ok },
        { name: 'Repor', value: alerts },
    ];

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                    <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        outerRadius={85}
                        paddingAngle={4}
                        dataKey="value"
                        label={({ name, value }) => `${name}: ${value}`}
                    >
                        <Cell fill="#10b981" />
                        <Cell fill="#ef4444" />
                    </Pie>
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        formatter={(v, name) => [v, name]}
                    />
                    <Legend wrapperStyle={{ fontSize: 12, color: '#a0a0a0' }} />
                </PieChart>
            </ResponsiveContainer>
        </div>
    );
}

function StockHealthTable({ data }) {
    if (!data?.length) return <p className={styles.empty}>Sem dados disponíveis.</p>;
    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Produto</th>
                        <th>Categoria</th>
                        <th>Estoque atual</th>
                        <th>Mínimo previsto</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map(d => (
                        <tr key={d.productId} className={d.requiresRestock ? styles.rowDanger : ''}>
                            <td>{d.productName}</td>
                            <td>{d.category}</td>
                            <td>{d.currentStock}</td>
                            <td>{d.predictedMinimum}</td>
                            <td>{d.requiresRestock ? '⚠️ Repor' : '✅ OK'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export { StockHealthPanel, StockHealthTable };
