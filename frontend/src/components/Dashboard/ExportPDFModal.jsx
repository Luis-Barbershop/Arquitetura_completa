import React, { useState, useMemo } from 'react';
import { PDFDownloadLink } from '@react-pdf/renderer';
import { ReportPDF, buildPDFSections } from './ReportPDF';
import styles from './ExportPDFModal.module.css';

const fmtPt = (iso) => {
    if (!iso) return '';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
};

/**
 * Props:
 *   barbershopName  string
 *   analyticsData   { barberPerf, stockHealth, agendaThermo, skillMatrix, customerAcq, customerRet }
 *   onClose         () => void
 */
export function ExportPDFModal({ barbershopName, analyticsData, onClose }) {
    const [start, setStart] = useState('');
    const [end,   setEnd]   = useState('');

    const sections = useMemo(
        () => buildPDFSections(analyticsData ?? {}),
        [analyticsData],
    );

    const ready = start && end && start <= end;

    const fileName = ready
        ? `cortaai-relatorio-${start}-${end}.pdf`
        : 'cortaai-relatorio.pdf';

    return (
        <div className={styles.overlay} onClick={onClose} role="dialog" aria-modal="true">
            <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
                <h3 className={styles.title}>Exportar Relatório em PDF</h3>
                <p className={styles.hint}>
                    Selecione o período de referência. O PDF inclui todos os painéis visíveis na dashboard.
                </p>

                <div className={styles.fields}>
                    <label className={styles.field}>
                        <span>De</span>
                        <input
                            type="date"
                            value={start}
                            onChange={(e) => setStart(e.target.value)}
                            max={end || undefined}
                        />
                    </label>
                    <label className={styles.field}>
                        <span>Até</span>
                        <input
                            type="date"
                            value={end}
                            onChange={(e) => setEnd(e.target.value)}
                            min={start || undefined}
                        />
                    </label>
                </div>

                {ready ? (
                    <PDFDownloadLink
                        document={
                            <ReportPDF
                                barbershopName={barbershopName ?? 'Barbearia'}
                                period={{ start: fmtPt(start), end: fmtPt(end) }}
                                sections={sections}
                            />
                        }
                        fileName={fileName}
                        className={styles.downloadBtn}
                    >
                        {({ loading }) => loading ? 'Gerando PDF...' : '⬇ Baixar PDF'}
                    </PDFDownloadLink>
                ) : (
                    <button className={styles.downloadBtnDisabled} disabled>
                        ⬇ Baixar PDF
                    </button>
                )}

                <button className={styles.closeBtn} onClick={onClose} type="button">
                    Fechar
                </button>
            </div>
        </div>
    );
}
