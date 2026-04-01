import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberStockPage.module.css';

const STOCK_STORAGE_PREFIX = 'barber_stock_items_';

const defaultItems = [
  {
    id: 'default-1',
    name: 'Pomada Modeladora',
    category: 'Finalizacao',
    quantity: 12,
    minQuantity: 5,
    costPrice: 22.5,
  },
  {
    id: 'default-2',
    name: 'Navalha Descartavel',
    category: 'Acessorios',
    quantity: 8,
    minQuantity: 10,
    costPrice: 3.2,
  },
  {
    id: 'default-3',
    name: 'Toalha Profissional',
    category: 'Higiene',
    quantity: 16,
    minQuantity: 6,
    costPrice: 12.0,
  },
];

const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

function BarberStockPage() {
  const navigate = useNavigate();

  const [barber, setBarber] = useState(null);
  const [loadingBarber, setLoadingBarber] = useState(true);
  const [items, setItems] = useState([]);
  const [loadingItems, setLoadingItems] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('Todas');

  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [minQuantity, setMinQuantity] = useState('3');
  const [costPrice, setCostPrice] = useState('');

  const handleTabChange = (tab) => {
    if (tab === 'estoque') return;

    if (tab === 'home') {
      navigate('/barberHome');
      return;
    }

    if (tab === 'agenda') {
      navigate('/meus-agendamentos');
      return;
    }

    if (tab === 'servicos') {
      navigate('/barberHome/servicos');
      return;
    }

    navigate('/barberHome', { state: { activeTab: tab } });
  };

  const handleLogout = () => {
    logoutUser();
    navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
  };

  useEffect(() => {
    const token = localStorage.getItem('token');

    if (!token) {
      navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
      return;
    }

    api.get('/barbers/me')
      .then((response) => {
        setBarber(response.data);
        setLoadingBarber(false);
      })
      .catch((err) => {
        console.error(err);
        setLoadingBarber(false);
        navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
      });
  }, [navigate]);

  useEffect(() => {
    const userId = localStorage.getItem('userId');

    if (!userId) {
      setItems(defaultItems);
      setLoadingItems(false);
      return;
    }

    const storageKey = `${STOCK_STORAGE_PREFIX}${userId}`;
    const raw = localStorage.getItem(storageKey);

    if (!raw) {
      localStorage.setItem(storageKey, JSON.stringify(defaultItems));
      setItems(defaultItems);
      setLoadingItems(false);
      return;
    }

    try {
      const parsed = JSON.parse(raw);
      setItems(Array.isArray(parsed) ? parsed : defaultItems);
    } catch (error) {
      console.error('Erro ao ler estoque local:', error);
      setItems(defaultItems);
    } finally {
      setLoadingItems(false);
    }
  }, []);

  useEffect(() => {
    const userId = localStorage.getItem('userId');
    if (!userId || loadingItems) return;

    const storageKey = `${STOCK_STORAGE_PREFIX}${userId}`;
    localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items, loadingItems]);

  const totalProducts = items.length;
  const totalUnits = useMemo(
    () => items.reduce((sum, item) => sum + Number(item.quantity || 0), 0),
    [items],
  );

  const lowStockItems = useMemo(
    () => items.filter((item) => Number(item.quantity) <= Number(item.minQuantity)),
    [items],
  );

  const totalCostValue = useMemo(
    () => items.reduce((sum, item) => sum + (Number(item.quantity || 0) * Number(item.costPrice || 0)), 0),
    [items],
  );

  const categories = useMemo(() => {
    const unique = [...new Set(items.map((item) => item.category).filter(Boolean))];
    return ['Todas', ...unique.sort((a, b) => a.localeCompare(b))];
  }, [items]);

  const filteredItems = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    return items.filter((item) => {
      const matchesCategory = categoryFilter === 'Todas' || item.category === categoryFilter;
      const matchesSearch = !normalizedSearch
        || item.name.toLowerCase().includes(normalizedSearch)
        || item.category.toLowerCase().includes(normalizedSearch);

      return matchesCategory && matchesSearch;
    });
  }, [items, searchTerm, categoryFilter]);

  const handleAddItem = (e) => {
    e.preventDefault();

    if (!name.trim() || !category.trim() || !quantity || !minQuantity || !costPrice) {
      return;
    }

    setIsSaving(true);

    const newItem = {
      id: `stock-${Date.now()}`,
      name: name.trim(),
      category: category.trim(),
      quantity: Number(quantity),
      minQuantity: Number(minQuantity),
      costPrice: Number(costPrice),
    };

    setItems((prev) => [newItem, ...prev]);

    setName('');
    setCategory('');
    setQuantity('1');
    setMinQuantity('3');
    setCostPrice('');

    setIsSaving(false);
  };

  const updateItemQuantity = (id, delta) => {
    setItems((prev) => prev.map((item) => {
      if (item.id !== id) return item;

      const nextQuantity = Math.max(0, Number(item.quantity || 0) + delta);
      return { ...item, quantity: nextQuantity };
    }));
  };

  const handleDeleteItem = (itemId) => {
    const confirmed = window.confirm('Deseja realmente excluir este produto do estoque?');
    if (!confirmed) return;

    setItems((prev) => prev.filter((item) => item.id !== itemId));
  };

  if (loadingBarber) {
    return <div className={styles.loadingContainer}>Carregando...</div>;
  }

  return (
    <div className={styles.pageContainer}>
      <div className={styles.contentWrapper}>
        <BarberHeader
          barber={barber}
          onLogout={handleLogout}
          activeTab="estoque"
          onTabChange={handleTabChange}
        />

        <section className={styles.heroSection}>
          <div>
            <p className={styles.heroKicker}>GESTAO DE ESTOQUE</p>
            <h1>Controle o estoque com mais clareza e rapidez</h1>
            <p>
              Cadastre produtos, ajuste quantidades e acompanhe alertas de nivel minimo em um painel unico.
            </p>
          </div>

          <div className={styles.heroInfoCard}>
            <span>Painel ativo</span>
            <strong>Atualizacao local em tempo real</strong>
            <small>Seu assistente para a sua organização</small>
          </div>
        </section>

        <section className={styles.metricsGrid}>
          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Produtos cadastrados</span>
            <strong className={styles.metricValue}>{totalProducts}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Unidades em estoque</span>
            <strong className={styles.metricValue}>{totalUnits}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Custo total estimado</span>
            <strong className={styles.metricValue}>{asCurrency(totalCostValue)}</strong>
          </article>
        </section>

        <section className={styles.managementGrid}>
          <aside className={`${styles.panelCard} ${styles.createPanel}`}>
            <div className={styles.panelHeader}>
              <h2>Novo produto</h2>
            </div>

            <form className={styles.form} onSubmit={handleAddItem}>
              <label className={styles.formLabel} htmlFor="stock-name">Nome do produto</label>
              <input
                id="stock-name"
                className={styles.formInput}
                type="text"
                placeholder="Ex: Gel fixador"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />

              <div className={styles.inlineFields}>
                <div>
                  <label className={styles.formLabel} htmlFor="stock-category">Categoria</label>
                  <input
                    id="stock-category"
                    className={styles.formInput}
                    type="text"
                    placeholder="Ex: Finalizacao"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className={styles.formLabel} htmlFor="stock-cost">Custo unitario (R$)</label>
                  <input
                    id="stock-cost"
                    className={styles.formInput}
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="0.00"
                    value={costPrice}
                    onChange={(e) => setCostPrice(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className={styles.inlineFields}>
                <div>
                  <label className={styles.formLabel} htmlFor="stock-quantity">Quantidade inicial</label>
                  <input
                    id="stock-quantity"
                    className={styles.formInput}
                    type="number"
                    min="0"
                    step="1"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className={styles.formLabel} htmlFor="stock-min">Quantidade minima</label>
                  <input
                    id="stock-min"
                    className={styles.formInput}
                    type="number"
                    min="0"
                    step="1"
                    value={minQuantity}
                    onChange={(e) => setMinQuantity(e.target.value)}
                    required
                  />
                </div>
              </div>

              <button type="submit" className={styles.primaryButton} disabled={isSaving}>
                {isSaving ? 'Salvando...' : 'Adicionar produto'}
              </button>
            </form>

            <p className={styles.helperText}>Dica: mantenha o minimo alinhado com sua demanda semanal.</p>
          </aside>

          <article className={`${styles.panelCard} ${styles.listPanel}`}>
            <div className={styles.panelHeader}>
              <h2>Inventario de produtos</h2>
              <span className={styles.lowStockBadge}>
                {lowStockItems.length} com estoque baixo
              </span>
            </div>

            <div className={styles.filtersRow}>
              <input
                className={styles.searchInput}
                type="text"
                placeholder="Buscar por nome ou categoria"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />

              <select
                className={styles.filterSelect}
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                {categories.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            </div>

            {loadingItems ? (
              <p className={styles.mutedText}>Carregando estoque...</p>
            ) : filteredItems.length ? (
              <ul className={styles.itemsList}>
                {filteredItems.map((item) => {
                  const isLow = Number(item.quantity) <= Number(item.minQuantity);

                  return (
                    <li key={item.id} className={styles.itemCard}>
                      <div className={styles.itemMainInfo}>
                        <p className={styles.itemName}>{item.name}</p>
                        <div className={styles.itemMetaRow}>
                          <span className={styles.itemMeta}>{item.category}</span>
                          <span className={isLow ? styles.itemAlert : styles.itemMeta}>
                            Minimo: {item.minQuantity}
                          </span>
                        </div>
                      </div>

                      <div className={styles.itemRightInfo}>
                        <strong className={styles.itemCost}>{asCurrency(item.costPrice)}</strong>

                        <div className={styles.quantityControl}>
                          <button type="button" onClick={() => updateItemQuantity(item.id, -1)}>-</button>
                          <span>{item.quantity}</span>
                          <button type="button" onClick={() => updateItemQuantity(item.id, 1)}>+</button>
                        </div>

                        <button
                          type="button"
                          className={styles.deleteButton}
                          onClick={() => handleDeleteItem(item.id)}
                        >
                          Excluir
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className={styles.mutedText}>Nenhum produto encontrado com esse filtro.</p>
            )}
          </article>
        </section>
      </div>

      <BarberNavbar activeTab="estoque" onTabChange={handleTabChange} />
    </div>
  );
}

export default BarberStockPage;
