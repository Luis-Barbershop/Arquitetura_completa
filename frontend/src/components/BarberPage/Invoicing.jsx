import React, { useEffect, useMemo, useState } from 'react'
import api from '../../services/api';
import Styles from "./CSS/invoicing.module.css"

const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

function Invoicing({ barber }) {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [series, setSeries] = useState([]);
  const [seriesError, setSeriesError] = useState(false);

  const barbershopId = barber?.barbershopId;
  const isOwner = Boolean(barber?.isOwner ?? barber?.owner);

  useEffect(() => {
    const loadOverview = async () => {
      if (!barbershopId) {
        setData(null);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const now = new Date();
        const today = now.toISOString().slice(0, 10);
        const fromDate = new Date(now);
        fromDate.setDate(fromDate.getDate() - 6);
        const from = fromDate.toISOString().slice(0, 10);

        const response = await api.get('/payments/my-shop/overview', {
          params: {
            barbershopId,
            from: today,
            to: today,
          },
        });
        setData(response.data || null);

        if (isOwner) {
          try {
            const seriesResponse = await api.get('/payments/my-shop/series', {
              params: {
                barbershopId,
                from,
                to: today,
                groupBy: 'DAY',
              },
            });
            const points = Array.isArray(seriesResponse.data?.points) ? seriesResponse.data.points : [];
            setSeries(points);
            setSeriesError(false);
          } catch (seriesLoadError) {
            console.error('Erro ao carregar serie financeira:', seriesLoadError);
            setSeries([]);
            setSeriesError(true);
          }
        } else {
          setSeries([]);
          setSeriesError(false);
        }
      } catch (error) {
        console.error('Erro ao carregar overview financeiro:', error);
        setData(null);
        setSeries([]);
      } finally {
        setLoading(false);
      }
    };

    loadOverview();
  }, [barbershopId, isOwner]);

  const operationalResult = useMemo(() => Number(data?.operationalResult || 0), [data]);
  const maxSeriesRevenue = useMemo(() => {
    if (!series.length) return 1;
    const maxValue = Math.max(...series.map((point) => Number(point?.serviceRevenue || 0)));
    return maxValue > 0 ? maxValue : 1;
  }, [series]);

  return (

    <div className={Styles.containerFaturamento}>
        <div className={Styles.containerFaturamentoLeft}>
        <h2>Faturamento Hoje:</h2>
        <h1>{loading ? 'Carregando...' : asCurrency(data?.serviceRevenue)}</h1>
        <p>Gastos de produtos: {loading ? '...' : asCurrency(data?.productExpenses)}</p>
        <p>Bens em estoque: {loading ? '...' : asCurrency(data?.inventoryAssetValue)}</p>
        <p>Resultado operacional: {loading ? '...' : `${operationalResult >= 0 ? '+' : '-'} ${asCurrency(Math.abs(operationalResult))}`}</p>

        {isOwner && !loading && (
          <div className={Styles.seriesWrapper}>
            <p className={Styles.seriesTitle}>Receita dos ultimos 7 dias</p>

            {seriesError ? (
              <p className={Styles.seriesHint}>Nao foi possivel carregar a serie.</p>
            ) : series.length ? (
              <div className={Styles.seriesBars}>
                {series.map((point) => {
                  const revenue = Number(point?.serviceRevenue || 0);
                  const width = Math.max(8, Math.round((revenue / maxSeriesRevenue) * 100));
                  const dayLabel = String(point?.date || '').slice(5);

                  return (
                    <div key={`${point.date}-${revenue}`} className={Styles.seriesRow}>
                      <span className={Styles.seriesLabel}>{dayLabel}</span>
                      <div className={Styles.seriesTrack}>
                        <div className={Styles.seriesBar} style={{ width: `${width}%` }} />
                      </div>
                      <span className={Styles.seriesValue}>{asCurrency(revenue)}</span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className={Styles.seriesHint}>Sem receita aprovada no periodo.</p>
            )}
          </div>
        )}
        </div>

        <div className={Styles.containerFaturamentoRight}>
            <img src="/Icons/moneyIcon.png" alt="Icone de Dinheiro" />
        </div>
    </div>
  )
}

export default Invoicing