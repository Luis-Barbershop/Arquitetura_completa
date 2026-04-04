import { useEffect, useState } from 'react';
import api from '../../services/api';
import Styles from './CSS/Stock.module.css'

function Stock({ onNavigateToStock, barbershopId }) {
  const [lowStockCount, setLowStockCount] = useState(null);

  useEffect(() => {
    const loadLowStock = async () => {
      if (!barbershopId) {
        setLowStockCount(0);
        return;
      }

      try {
        const response = await api.get('/products/inventory', {
          params: {
            barbershopId,
            lowStock: true,
            page: 0,
            size: 1,
          },
        });

        setLowStockCount(Number(response.data?.total ?? 0));
      } catch (error) {
        console.error('Erro ao carregar estoque baixo:', error);
        setLowStockCount(null);
      }
    };

    loadLowStock();
  }, [barbershopId]);

  return (
    <div className={Styles.container}>
        <div className={Styles.stockCard}>
            <div className={Styles.stockIcon}></div>
            <h3>Estoque Baixo</h3>
        </div>

        <div className={Styles.stockNumber}>
            <h1>{lowStockCount === null ? '--' : String(lowStockCount).padStart(2, '0')}</h1>
        </div>

        <div className={Styles.stockDetails}>
            <p>Produtos com estoque baixo</p>
        </div>

        <div>
        <button className={Styles.stockButton} onClick={onNavigateToStock}>
            Gerenciar Estoque
        </button>
        </div>

    </div>
  )
}

export default Stock