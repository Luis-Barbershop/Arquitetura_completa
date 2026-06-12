import React, { useEffect, useMemo, useState } from 'react'
import { getBarberFinancialSummary, getFinancialOverview, getFinancialSeries } from '../../services/analyticsService';
import Styles from "./CSS/invoicing.module.css"

const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

function Invoicing({ barber }) {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [series, setSeries] = useState([]);
  const [seriesError, setSeriesError] = useState(false);

  const barbershopId = barber?.barbershopId || localStorage.getItem('barbershopId');
  const isOwner = useMemo(() => {
    const ownerFlag = barber?.isOwner ?? barber?.owner;
    const ownerFromProfile = ownerFlag === true || ownerFlag === 'true';
    const role = barber?.role;
    const ownerFromRole = Array.isArray(role)
      ? role.some((item) => String(item).toUpperCase().includes('OWNER'))
      : String(role || '').toUpperCase().includes('OWNER');
    const hasProfileOwnerSignal = barber && (
      Object.prototype.hasOwnProperty.call(barber, 'isOwner') ||
      Object.prototype.hasOwnProperty.call(barber, 'owner') ||
      Object.prototype.hasOwnProperty.call(barber, 'role')
    );

    const ownerFromStorage =
      localStorage.getItem('isOwner') === 'true' ||
      String(localStorage.getItem('userRole') || '').toUpperCase().includes('OWNER');

    if (hasProfileOwnerSignal) {
      return ownerFromProfile || ownerFromRole;
    }

    return ownerFromStorage;
  }, [barber]);

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
        const today = now.toLocaleDateString('en-CA');
        const fromDate = new Date(now);
        fromDate.setDate(fromDate.getDate() - 6);
        const from = fromDate.toLocaleDateString('en-CA');

        if (isOwner) {
          const response = await getFinancialOverview(barbershopId, {
            from: today,
            to: today,
          });
          setData(response || null);

          try {
            const seriesResponse = await getFinancialSeries(barbershopId, {
              from,
              to: today,
              groupBy: 'DAY',
            });
            const points = Array.isArray(seriesResponse?.points) ? seriesResponse.points : [];
            setSeries(points);
            setSeriesError(false);
          } catch (seriesLoadError) {
            console.error('Erro ao carregar serie financeira:', seriesLoadError);
            setSeries([]);
            setSeriesError(true);
          }
        } else {
          const response = await getBarberFinancialSummary(barbershopId, {
            from: today,
            to: today,
          });
          setData(response || null);
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

  const panelTitle = isOwner ? 'Faturamento Hoje:' : 'Você recebe hoje:';
  const serviceLabel = isOwner ? 'Receita com transacao' : 'Comissão com transacao';
  const walkInLabel = isOwner ? 'Receita de walk-in' : 'Comissão de walk-in';
  const operationalResult = useMemo(() => Number(data?.operationalResult || 0), [data]);
  const operationalResultWithWalkIn = useMemo(
    () => Number(data?.operationalResultWithWalkIn ?? data?.operationalResult ?? 0),
    [data]
  );
  const serviceRevenue = useMemo(
    () => Number((isOwner ? data?.serviceRevenue : data?.barberServiceCommission) || 0),
    [data, isOwner]
  );
  const walkInRevenue = useMemo(
    () => Number((isOwner ? data?.walkInRevenue : data?.barberWalkInCommission) || 0),
    [data, isOwner]
  );
  const totalServiceRevenue = useMemo(
    () => Number((isOwner ? data?.totalServiceRevenue : data?.barberTotalCommission) ?? (serviceRevenue + walkInRevenue)),
    [data, isOwner, serviceRevenue, walkInRevenue]
  );
  const grossTotalRevenue = useMemo(() => Number(data?.grossTotalRevenue || 0), [data]);
  const barbershopTotalCommission = useMemo(() => Number(data?.barbershopTotalCommission || 0), [data]);
  const maxSeriesRevenue = useMemo(() => {
    if (!series.length) return 1;
    const maxValue = Math.max(...series.map((point) => Number(point?.totalServiceRevenue ?? (point?.serviceRevenue || 0))));
    return maxValue > 0 ? maxValue : 1;
  }, [series]);

  return (

    <div className={Styles.containerFaturamento}>
        <div className={Styles.containerFaturamentoLeft}>
        <h2>{panelTitle}</h2>
        <h1>{loading ? 'Carregando...' : asCurrency(totalServiceRevenue)}</h1>
        {!isOwner && (
          <p>Valor bruto gerado: {loading ? '...' : asCurrency(grossTotalRevenue)}</p>
        )}
        <p>{serviceLabel}: {loading ? '...' : asCurrency(serviceRevenue)}</p>
        <p>{walkInLabel}: {loading ? '...' : asCurrency(walkInRevenue)}</p>
        {!isOwner && (
          <p>Comissão da barbearia: {loading ? '...' : asCurrency(barbershopTotalCommission)}</p>
        )}
        {isOwner && (
          <>
            <p>Gastos de produtos: {loading ? '...' : asCurrency(data?.productExpenses)}</p>
            <p>Bens em estoque: {loading ? '...' : asCurrency(data?.inventoryAssetValue)}</p>
            <p>Resultado operacional (transacao): {loading ? '...' : `${operationalResult >= 0 ? '+' : '-'} ${asCurrency(Math.abs(operationalResult))}`}</p>
            <p>Resultado operacional total: {loading ? '...' : `${operationalResultWithWalkIn >= 0 ? '+' : '-'} ${asCurrency(Math.abs(operationalResultWithWalkIn))}`}</p>
          </>
        )}

        {isOwner && !loading && (
          <div className={Styles.seriesWrapper}>
            <p className={Styles.seriesTitle}>Receita dos ultimos 7 dias (transacao + walk-in)</p>
            <div className={Styles.seriesLegend}>
              <span className={Styles.legendItem}><span className={`${Styles.legendDot} ${Styles.legendDotService}`} />Transacao</span>
              <span className={Styles.legendItem}><span className={`${Styles.legendDot} ${Styles.legendDotWalkIn}`} />Walk-in</span>
            </div>

            {seriesError ? (
              <p className={Styles.seriesHint}>Não foi possível carregar a série.</p>
            ) : series.length ? (
              <div className={Styles.seriesBars}>
                {series.map((point) => {
                  const revenue = Number(point?.serviceRevenue || 0);
                  const walkIn = Number(point?.walkInRevenue || 0);
                  const totalRevenue = Number(point?.totalServiceRevenue ?? (revenue + walkIn));
                  const width = Math.max(8, Math.round((totalRevenue / maxSeriesRevenue) * 100));
                  const dayLabel = String(point?.date || '').slice(5);
                  const serviceShare = totalRevenue > 0 ? (revenue / totalRevenue) * 100 : 0;
                  const walkInShare = totalRevenue > 0 ? (walkIn / totalRevenue) * 100 : 0;

                  return (
                    <div key={`${point.date}-${totalRevenue}`} className={Styles.seriesRow}>
                      <span className={Styles.seriesLabel}>{dayLabel}</span>
                      <div className={Styles.seriesTrack}>
                        <div className={Styles.seriesBarStack} style={{ width: `${width}%` }}>
                          <div className={Styles.seriesBarService} style={{ width: `${serviceShare}%` }} />
                          <div className={Styles.seriesBarWalkIn} style={{ width: `${walkInShare}%` }} />
                        </div>
                      </div>
                      <span className={Styles.seriesValue}>{asCurrency(totalRevenue)}</span>
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
            <img src="/Icons/moneyIcon.png" alt="Ícone de dinheiro" />
        </div>
    </div>
  )
}

export default Invoicing
