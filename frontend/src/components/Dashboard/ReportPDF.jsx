import React from 'react';
import { Document, Page, Text, View, Image, StyleSheet } from '@react-pdf/renderer';

const BRL = (v) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(v) || 0);

const styles = StyleSheet.create({
    page:    { padding: 40, fontFamily: 'Helvetica', fontSize: 10, color: '#222', backgroundColor: '#fff' },
    header:  { flexDirection: 'row', alignItems: 'center', marginBottom: 24, borderBottom: '2pt solid #1a1a2e', paddingBottom: 12 },
    logo:    { width: 52, height: 52, marginRight: 16 },
    titleBlock: { flexDirection: 'column' },
    title:   { fontSize: 16, fontFamily: 'Helvetica-Bold', color: '#1a1a2e', marginBottom: 2 },
    subtitle: { fontSize: 9, color: '#555' },
    section: { marginTop: 18 },
    sectionTitle: { fontSize: 11, fontFamily: 'Helvetica-Bold', color: '#1a1a2e', marginBottom: 6, paddingBottom: 4, borderBottom: '1pt solid #ddd' },
    thead:   { flexDirection: 'row', backgroundColor: '#1a1a2e', padding: '5 4', borderRadius: 2, marginBottom: 2 },
    theadCell: { flex: 1, fontSize: 9, fontFamily: 'Helvetica-Bold', color: '#fff', paddingHorizontal: 4 },
    row:     { flexDirection: 'row', borderBottom: '0.5pt solid #eee', paddingVertical: 5 },
    rowAlt:  { flexDirection: 'row', borderBottom: '0.5pt solid #eee', paddingVertical: 5, backgroundColor: '#f9f9f9' },
    cell:    { flex: 1, fontSize: 9, paddingHorizontal: 4, color: '#333' },
    empty:   { fontSize: 9, color: '#888', fontStyle: 'italic', paddingVertical: 6 },
    footer:  { position: 'absolute', bottom: 24, left: 40, right: 40, textAlign: 'center', color: '#999', fontSize: 8 },
    badge:   { fontSize: 8, color: '#c19006', fontFamily: 'Helvetica-Bold' },
});

function SectionBlock({ section }) {
    if (!section.rows?.length) {
        return (
            <View style={styles.section}>
                <Text style={styles.sectionTitle}>{section.title}</Text>
                <Text style={styles.empty}>Sem dados no período.</Text>
            </View>
        );
    }
    return (
        <View style={styles.section}>
            <Text style={styles.sectionTitle}>{section.title}</Text>
            {section.headers && (
                <View style={styles.thead}>
                    {section.headers.map((h, i) => (
                        <Text key={i} style={styles.theadCell}>{h}</Text>
                    ))}
                </View>
            )}
            {section.rows.map((row, j) => (
                <View key={j} style={j % 2 === 0 ? styles.row : styles.rowAlt}>
                    {row.map((cell, k) => (
                        <Text key={k} style={styles.cell}>{cell ?? '—'}</Text>
                    ))}
                </View>
            ))}
        </View>
    );
}

/**
 * Props:
 *   barbershopName  string
 *   period          { start: string, end: string }   datas formatadas
 *   sections        Array<{ title, headers?, rows: string[][] }>
 */
export function ReportPDF({ barbershopName, period, sections }) {
    const now = new Date().toLocaleString('pt-BR');
    return (
        <Document title={`CortaAi — Relatório ${barbershopName}`} author="CortaAi">
            <Page size="A4" style={styles.page}>
                <View style={styles.header}>
                    <Image src="/CortaAiLogo.png" style={styles.logo} />
                    <View style={styles.titleBlock}>
                        <Text style={styles.title}>CortaAi — Relatório</Text>
                        <Text style={styles.subtitle}>{barbershopName}</Text>
                        <Text style={styles.subtitle}>Período: {period.start} a {period.end}</Text>
                    </View>
                </View>

                {sections.map((s, i) => (
                    <SectionBlock key={i} section={s} />
                ))}

                <Text
                    style={styles.footer}
                    render={({ pageNumber, totalPages }) =>
                        `Gerado em ${now}  ·  Página ${pageNumber} de ${totalPages}  ·  CortaAi`
                    }
                    fixed
                />
            </Page>
        </Document>
    );
}

// ── Helpers para montar sections a partir dos dados da dashboard ──────────────

const fmtDate = (iso) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('pt-BR');
};

export function buildPDFSections({ barberPerf, stockHealth, agendaThermo, skillMatrix, customerAcq, customerRet }) {
    return [
        {
            title: 'Performance dos Barbeiros',
            headers: ['Barbeiro', 'Agendamentos', 'Valor Gerado', 'Contribuição (%)'],
            rows: (barberPerf ?? []).map(d => [
                d.barberName ?? '—',
                String(d.totalAppointments ?? 0),
                BRL(d.generatedRevenue),
                `${Number(d.contributionPercentage ?? 0).toFixed(1)}%`,
            ]),
        },
        {
            title: 'Saúde do Estoque',
            headers: ['Produto', 'Estoque Atual', 'Estoque Mín.', 'Status'],
            rows: (stockHealth ?? []).map(d => [
                d.productName ?? '—',
                String(d.currentStock ?? 0),
                String(d.minimumStock ?? 0),
                d.alertLevel ?? '—',
            ]),
        },
        {
            title: 'Termômetro de Agenda',
            headers: ['Data', 'Total Agend.', 'Concluídos', 'Cancelados', 'Taxa Conclusão (%)'],
            rows: (agendaThermo ?? []).map(d => [
                fmtDate(d.date),
                String(d.totalAppointments ?? 0),
                String(d.completedAppointments ?? 0),
                String(d.cancelledAppointments ?? 0),
                `${Number(d.completionRate ?? 0).toFixed(1)}%`,
            ]),
        },
        {
            title: 'Matriz de Habilidades',
            headers: ['Barbeiro', 'Habilidade', 'Execuções', 'Receita'],
            rows: (skillMatrix ?? []).map(d => [
                d.barberName ?? '—',
                d.activityName ?? '—',
                String(d.executionCount ?? 0),
                BRL(d.generatedRevenue),
            ]),
        },
        {
            title: 'Aquisição de Clientes',
            headers: ['Período', 'Novos Clientes', 'Agendamentos', 'Receita'],
            rows: (customerAcq ?? []).map(d => [
                d.period ?? fmtDate(d.date),
                String(d.newCustomers ?? 0),
                String(d.totalAppointments ?? 0),
                BRL(d.totalRevenue),
            ]),
        },
        {
            title: 'Retenção de Clientes',
            headers: ['Cliente', 'Visitas', 'Última Visita', 'Receita Total'],
            rows: (customerRet ?? []).map(d => [
                d.customerName ?? '—',
                String(d.visitCount ?? 0),
                fmtDate(d.lastVisit),
                BRL(d.totalRevenue),
            ]),
        },
    ];
}
