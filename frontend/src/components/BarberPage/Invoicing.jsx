import React, { useEffect, useMemo, useState } from 'react'
import api from '../../services/api';
import Styles from "./CSS/invoicing.module.css"

const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

function Invoicing({ barber }) {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);

  const barbershopId = barber?.barbershopId;

  useEffect(() => {
    const loadOverview = async () => {
      if (!barbershopId) {
        setData(null);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const today = new Date().toISOString().slice(0, 10);
        const response = await api.get('/payments/my-shop/overview', {
          params: {
            barbershopId,
            from: today,
            to: today,
          },
        });
        setData(response.data || null);
      } catch (error) {
        console.error('Erro ao carregar overview financeiro:', error);
        setData(null);
      } finally {
        setLoading(false);
      }
    };

    loadOverview();
  }, [barbershopId]);

  const operationalResult = useMemo(() => Number(data?.operationalResult || 0), [data]);

  return (

    <div className={Styles.containerFaturamento}>
        <div className={Styles.containerFaturamentoLeft}>
        <h2>Faturamento Hoje:</h2>
        <h1>{loading ? 'Carregando...' : asCurrency(data?.serviceRevenue)}</h1>
        <p>Gastos de produtos: {loading ? '...' : asCurrency(data?.productExpenses)}</p>
        <p>Bens em estoque: {loading ? '...' : asCurrency(data?.inventoryAssetValue)}</p>
        <p>Resultado operacional: {loading ? '...' : `${operationalResult >= 0 ? '+' : '-'} ${asCurrency(Math.abs(operationalResult))}`}</p>
        </div>

        <div className={Styles.containerFaturamentoRight}>
            <img src="/Icons/moneyIcon.png" alt="Icone de Dinheiro" />
        </div>
    </div>
  )
}

export default Invoicing