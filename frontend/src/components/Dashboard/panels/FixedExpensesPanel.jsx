import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import styles from './PanelShared.module.css';

const COLORS = ['#c19006', '#e8b923', '#a07005', '#f5d76e', '#7d5604', '#d4af37', '#b8860b', '#ffd700', '#daa520', '#c8a951', '#e6c84e', '#8b6914'];
const BRL = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

export function FixedExpensesPiePanel({ data }) {
    if (!data?.length) return <p className={styles.empty}>Nenhum gasto fixo cadastrado.</p>;

    const pieData = data.map(e => ({
        name: e.customName || e.categoryLabel,
        value: parseFloat(e.amount) || 0,
    }));

    const total = pieData.reduce((s, e) => s + e.value, 0);

    return (
        <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                    <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        paddingAngle={2}
                        dataKey="value"
                        label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                        labelLine={false}
                    >
                        {pieData.map((_, i) => (
                            <Cell key={i} fill={COLORS[i % COLORS.length]} />
                        ))}
                    </Pie>
                    <Tooltip
                        contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: 8 }}
                        formatter={v => [BRL(v), 'Valor']}
                    />
                    <Legend wrapperStyle={{ fontSize: '0.75rem', color: '#a0a0a0' }} />
                </PieChart>
            </ResponsiveContainer>
            <p style={{ textAlign: 'center', color: '#a0a0a0', fontSize: '0.85rem', marginTop: '0.5rem' }}>
                Total: <strong style={{ color: '#e8b923' }}>{BRL(total)}</strong>
            </p>
        </div>
    );
}

export function FixedExpensesTable({ data, onDelete }) {
    if (!data?.length) return <p className={styles.empty}>Nenhum gasto fixo cadastrado.</p>;

    const total = data.reduce((s, e) => s + parseFloat(e.amount), 0);

    return (
        <div className={styles.tableWrapper}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>Categoria</th>
                        <th>Nome</th>
                        <th>Valor</th>
                        {onDelete && <th></th>}
                    </tr>
                </thead>
                <tbody>
                    {data.map(e => (
                        <tr key={e.id}>
                            <td>{e.categoryLabel}</td>
                            <td>{e.customName || '—'}</td>
                            <td>{BRL(e.amount)}</td>
                            {onDelete && (
                                <td>
                                    <button
                                        type="button"
                                        onClick={() => onDelete(e.id)}
                                        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#e53e3e', fontSize: '1rem' }}
                                        title="Remover"
                                    >
                                        🗑️
                                    </button>
                                </td>
                            )}
                        </tr>
                    ))}
                </tbody>
                <tfoot>
                    <tr>
                        <td colSpan={2}><strong>Total</strong></td>
                        <td><strong style={{ color: '#e8b923' }}>{BRL(total)}</strong></td>
                        {onDelete && <td />}
                    </tr>
                </tfoot>
            </table>
        </div>
    );
}
