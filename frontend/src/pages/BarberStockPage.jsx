import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberStockPage.module.css';

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

const PRODUCT_CATEGORIES = [
  { value: 'OTHER', label: 'Outros' },
  { value: 'POMADE', label: 'Pomada' },
  { value: 'WAX', label: 'Cera' },
  { value: 'OIL', label: 'Oleo capilar' },
  { value: 'BEARD_OIL', label: 'Oleo para barba' },
  { value: 'AFTERSHAVE', label: 'Pos barba' },
  { value: 'SHAMPOO', label: 'Shampoo' },
  { value: 'CONDITIONER', label: 'Condicionador' },
  { value: 'RAZOR', label: 'Navalha/Lamina' },
  { value: 'SCISSORS', label: 'Tesoura' },
  { value: 'COMB', label: 'Pente' },
  { value: 'BRUSH', label: 'Escova' },
  { value: 'ACCESSORY', label: 'Acessorio' },
];

const categoryLabelByValue = PRODUCT_CATEGORIES.reduce((acc, item) => {
  acc[item.value] = item.label;
  return acc;
}, {});

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
  const [category, setCategory] = useState('OTHER');
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

    api.get('/auth/me')
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
    const loadProducts = async () => {
      if (!barber?.barbershopId) {
        setItems(defaultItems);
        setLoadingItems(false);
        return;
      }

      try {
        setLoadingItems(true);
        const response = await api.get('/products', {
          params: { barbershopId: barber.barbershopId },
        });

        const apiItems = Array.isArray(response.data) ? response.data : [];
        const normalized = apiItems.map((item) => ({
          id: item.id,
          name: item.name || '',
          category: categoryLabelByValue[item.category] || 'Outros',
          quantity: Number(item.stockQuantity || 0),
          minQuantity: Number(item.minStockQuantity || 0),
          costPrice: Number(item.price || 0),
        }));

        setItems(normalized);
      } catch (error) {
        console.error('Erro ao carregar estoque da API:', error);
        setItems(defaultItems);
      } finally {
        setLoadingItems(false);
      }
    };

    loadProducts();
  }, [barber?.barbershopId]);

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

  const handleAddItem = async (e) => {
    e.preventDefault();

    if (!name.trim() || !category.trim() || !quantity || !minQuantity || !costPrice) {
      return;
    }

    if (!barber?.barbershopId) return;

    try {
      setIsSaving(true);

      const response = await api.post('/products', {
        barbershopId: barber.barbershopId,
        name: name.trim(),
        category,
        stockQuantity: Number(quantity),
        minStockQuantity: Number(minQuantity),
        price: Number(costPrice),
      });

      const created = response.data;
      const newItem = {
        id: created.id,
        name: created.name || name.trim(),
        category: categoryLabelByValue[created.category] || categoryLabelByValue[category] || 'Outros',
        quantity: Number(created.stockQuantity ?? quantity),
        minQuantity: Number(created.minStockQuantity ?? minQuantity),
        costPrice: Number(created.price ?? costPrice),
      };

      setItems((prev) => [newItem, ...prev]);
      setName('');
      setCategory('OTHER');
      setQuantity('1');
      setMinQuantity('3');
      setCostPrice('');
    } catch (error) {
      console.error('Erro ao criar produto no estoque:', error);
      toast.error('Nao foi possivel salvar o produto. Verifique os dados e tente novamente.');
    } finally {
      setIsSaving(false);
    }
  };

  const updateItemQuantity = async (id, delta) => {
    const target = items.find((item) => item.id === id);
    if (!target) return;

    const nextQuantity = Math.max(0, Number(target.quantity || 0) + delta);

    try {
      await api.put(`/products/${id}`, { stockQuantity: nextQuantity });
      setItems((prev) => prev.map((item) => (item.id === id ? { ...item, quantity: nextQuantity } : item)));
    } catch (error) {
      console.error('Erro ao atualizar estoque:', error);
      toast.error('Nao foi possivel atualizar a quantidade agora.');
    }
  };

  const handleDeleteItem = async (itemId) => {
    if (!window.confirm('Deseja realmente excluir este produto do estoque?')) return;

    try {
      await api.delete(`/products/${itemId}`);
      setItems((prev) => prev.filter((item) => item.id !== itemId));
    } catch (error) {
      console.error('Erro ao excluir produto:', error);
      toast.error('Nao foi possivel excluir o produto.');
    }
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
                  <select
                    id="stock-category"
                    className={styles.formInput}
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    required
                  >
                    {PRODUCT_CATEGORIES.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
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
