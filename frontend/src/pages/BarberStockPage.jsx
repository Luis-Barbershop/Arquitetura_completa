import React, { useEffect, useMemo, useState } from 'react';
import { PencilSimple, Trash } from '@phosphor-icons/react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import StockMovementModal from '../components/StockMovementModal/StockMovementModal';
import styles from './CSS/BarberStockPage.module.css';

const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

const normalizeProduct = (item) => ({
  id: item.id,
  name: item.name || '',
  categoryId: item.categoryId || null,
  category: item.categoryName || item.category || 'Sem categoria',
  quantity: Number(item.stockQuantity || 0),
  minQuantity: Number(item.minStockQuantity || 0),
  costPrice: Number(item.price || 0),
});

function BarberStockPage() {
  const navigate = useNavigate();

  const [barber, setBarber] = useState(null);
  const [loadingBarber, setLoadingBarber] = useState(true);
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loadingItems, setLoadingItems] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('Todas');
  const [activePanel, setActivePanel] = useState('products');
  const [movementProduct, setMovementProduct] = useState(null);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [editingCategory, setEditingCategory] = useState(null);

  const [name, setName] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [minQuantity, setMinQuantity] = useState('3');
  const [costPrice, setCostPrice] = useState('');

  const handleTabChange = (tab) => {
    if (tab === 'estoque') return;
    if (tab === 'home') navigate('/barberHome');
    if (tab === 'agenda') navigate('/meus-agendamentos');
    if (tab === 'servicos') navigate('/barberHome/servicos');
    if (tab === 'novo-agendamento') navigate('/barberHome/novo-agendamento');
    if (tab === 'perfil') navigate('/barberHome/perfil');
    if (tab === 'time') navigate('/barberHome/time');
    if (tab === 'dashboards') navigate('/barberHome/dashboard');
    if (tab === 'gerenciar-barbearia') navigate('/barberHome/gerenciar-barbearia');
    if (tab === 'agenda-equipe') navigate('/meus-agendamentos?view=team');
    if (tab === 'indisponibilidade') navigate('/barber/indisponibilidade');
  };

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  useEffect(() => {
    if (isCustomer()) {
      navigate('/homepage', { replace: true });
      return;
    }
    if (!isOwnerUser()) {
      navigate('/barberHome', { replace: true });
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/', { replace: true });
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
        navigate('/');
      });
  }, [navigate]);

  const loadStock = async () => {
    if (!barber?.barbershopId) return;

    try {
      setLoadingItems(true);
      const [productsResponse, categoriesResponse] = await Promise.all([
        api.get('/products', { params: { barbershopId: barber.barbershopId } }),
        api.get('/products/categories', { params: { barbershopId: barber.barbershopId } }),
      ]);

      setItems((Array.isArray(productsResponse.data) ? productsResponse.data : []).map(normalizeProduct));
      setCategories(Array.isArray(categoriesResponse.data) ? categoriesResponse.data : []);
    } catch (error) {
      console.error('Erro ao carregar estoque da API:', error);
      toast.error('Não foi possível carregar o estoque.');
    } finally {
      setLoadingItems(false);
    }
  };

  useEffect(() => {
    loadStock();
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

  const filterOptions = useMemo(() => {
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

    if (!name.trim() || !categoryId || !quantity || !minQuantity || !costPrice || !barber?.barbershopId) {
      toast.info('Preencha produto, categoria, quantidade e valor.');
      return;
    }

    try {
      setIsSaving(true);

      const response = await api.post('/products', {
        barbershopId: barber.barbershopId,
        name: name.trim(),
        categoryId,
        stockQuantity: Number(quantity),
        minStockQuantity: Number(minQuantity),
        price: Number(costPrice),
      });

      setItems((prev) => [normalizeProduct(response.data), ...prev]);
      setName('');
      setCategoryId('');
      setQuantity('1');
      setMinQuantity('3');
      setCostPrice('');
    } catch (error) {
      console.error('Erro ao criar produto no estoque:', error);
      toast.error('Não foi possível salvar o produto. Verifique os dados e tente novamente.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCreateCategory = async (event) => {
    event.preventDefault();
    if (!newCategoryName.trim() || !barber?.barbershopId) return;

    try {
      const response = await api.post('/products/categories', { name: newCategoryName.trim() }, {
        params: { barbershopId: barber.barbershopId },
      });
      setCategories((prev) => [...prev, response.data].sort((a, b) => a.name.localeCompare(b.name)));
      setNewCategoryName('');
      toast.success('Categoria criada.');
    } catch (error) {
      console.error('Erro ao criar categoria:', error);
      toast.error(error?.response?.data?.message || 'Nao foi possivel criar a categoria.');
    }
  };

  const handleUpdateCategory = async (event) => {
    event.preventDefault();
    if (!editingCategory?.name?.trim() || !barber?.barbershopId) return;

    try {
      const response = await api.put(`/products/categories/${editingCategory.id}`, { name: editingCategory.name.trim() }, {
        params: { barbershopId: barber.barbershopId },
      });
      setCategories((prev) => prev.map((cat) => (cat.id === response.data.id ? response.data : cat)));
      setItems((prev) => prev.map((item) => (
        item.categoryId === response.data.id ? { ...item, category: response.data.name } : item
      )));
      setEditingCategory(null);
      toast.success('Categoria atualizada.');
    } catch (error) {
      console.error('Erro ao atualizar categoria:', error);
      toast.error(error?.response?.data?.message || 'Nao foi possivel atualizar a categoria.');
    }
  };

  const handleDeleteCategory = async (cat) => {
    if (!window.confirm(`Excluir a categoria "${cat.name}"?`)) return;

    try {
      await api.delete(`/products/categories/${cat.id}`, {
        params: { barbershopId: barber.barbershopId },
      });
      setCategories((prev) => prev.filter((item) => item.id !== cat.id));
      if (categoryId === cat.id) setCategoryId('');
      toast.success('Categoria excluida.');
    } catch (error) {
      console.error('Erro ao excluir categoria:', error);
      toast.error(error?.response?.data?.message || 'Reclassifique produtos ativos antes de excluir.');
    }
  };

  const handleCreateMovement = async (payload) => {
    try {
      await api.post('/products/stock-movements', payload);
      await loadStock();
      setMovementProduct(null);
      toast.success('Movimentacao registrada.');
    } catch (error) {
      console.error('Erro ao movimentar estoque:', error);
      toast.error(error?.response?.data?.message || 'Nao foi possivel registrar a movimentacao.');
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
    <div className={styles.pageContainer} data-onboarding-id="owner-stock-page">
      <div className={styles.contentWrapper}>
        <BarberHeader
          barber={barber}
          onLogout={handleLogout}
          activeTab="estoque"
          onTabChange={handleTabChange}
          isOwner={true}
          barbershopId={barber?.barbershopId}
        />

        <section className={`${styles.heroSection} ${styles.animateItem} ${styles.delay1}`}>
          <div>
            <p className={styles.heroKicker}>GESTAO DE ESTOQUE</p>
            <h1>Controle o estoque com categorias e baixas rastreaveis</h1>
            <p>
              Cadastre produtos, organize categorias e registre consumo, venda, perda ou devolucao sem perder historico.
            </p>
          </div>

          <div className={styles.heroInfoCard}>
            <span>Painel ativo</span>
            <strong>{categories.length} categorias</strong>
            <small>{lowStockItems.length} produtos em alerta</small>
          </div>
        </section>

        <section className={`${styles.metricsGrid} ${styles.animateItem} ${styles.delay2}`}>
          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Produtos cadastrados</span>
            <strong className={styles.metricValue}>{totalProducts}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Unidades em estoque</span>
            <strong className={styles.metricValue}>{totalUnits}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Valor total estimado</span>
            <strong className={styles.metricValue}>{asCurrency(totalCostValue)}</strong>
          </article>
        </section>

        <div className={styles.segmentedControl}>
          <button
            type="button"
            className={activePanel === 'products' ? styles.segmentActive : ''}
            onClick={() => setActivePanel('products')}
          >
            Produtos
          </button>
          <button
            type="button"
            className={activePanel === 'categories' ? styles.segmentActive : ''}
            onClick={() => setActivePanel('categories')}
          >
            Categorias
          </button>
        </div>

        {activePanel === 'products' ? (
          <section className={`${styles.managementGrid} ${styles.animateItem} ${styles.delay3}`}>
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
                      value={categoryId}
                      onChange={(e) => setCategoryId(e.target.value)}
                      required
                    >
                      <option value="">Selecione</option>
                      {categories.map((option) => (
                        <option key={option.id} value={option.id}>{option.name}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className={styles.formLabel} htmlFor="stock-cost">Valor unitario (R$)</label>
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

                <button type="submit" className={styles.primaryButton} disabled={isSaving || categories.length === 0}>
                  {isSaving ? 'Salvando...' : 'Adicionar produto'}
                </button>
              </form>

              <p className={styles.helperText}>Crie ao menos uma categoria antes de cadastrar produtos.</p>
            </aside>

            <article className={`${styles.panelCard} ${styles.listPanel}`}>
              <div className={styles.panelHeader}>
                <h2>Inventario de produtos</h2>
                <span className={styles.lowStockBadge}>
                  {lowStockItems.length === 0 ? 'Nenhum com estoque baixo' : `${lowStockItems.length} com estoque baixo`}
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
                  {filterOptions.map((option) => (
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
                          <span className={styles.quantityPill}>{item.quantity} un.</span>

                          <button
                            type="button"
                            className={styles.iconActionButton}
                            onClick={() => setMovementProduct(item)}
                            aria-label="Movimentar estoque"
                            title="Movimentar estoque"
                          >
                            <PencilSimple size={18} weight="bold" />
                          </button>

                          <button
                            type="button"
                            className={styles.deleteButton}
                            onClick={() => handleDeleteItem(item.id)}
                            aria-label="Excluir item"
                            title="Excluir item"
                          >
                            <Trash size={18} weight="bold" />
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
        ) : (
          <section className={`${styles.panelCard} ${styles.categoriesPanel} ${styles.animateItem} ${styles.delay3}`}>
            <div className={styles.panelHeader}>
              <h2>Categorias</h2>
              <span className={styles.lowStockBadge}>{categories.length} cadastradas</span>
            </div>

            <form className={styles.categoryForm} onSubmit={handleCreateCategory}>
              <input
                className={styles.formInput}
                type="text"
                placeholder="Ex: Finalizacao"
                value={newCategoryName}
                onChange={(event) => setNewCategoryName(event.target.value)}
              />
              <button type="submit" className={styles.primaryButton}>Criar categoria</button>
            </form>

            {editingCategory && (
              <form className={styles.categoryForm} onSubmit={handleUpdateCategory}>
                <input
                  className={styles.formInput}
                  type="text"
                  value={editingCategory.name}
                  onChange={(event) => setEditingCategory((prev) => ({ ...prev, name: event.target.value }))}
                  autoFocus
                />
                <button type="submit" className={styles.primaryButton}>Salvar</button>
                <button type="button" className={styles.secondaryButton} onClick={() => setEditingCategory(null)}>Cancelar</button>
              </form>
            )}

            <ul className={styles.categoryList}>
              {categories.map((cat) => (
                <li key={cat.id} className={styles.categoryItem}>
                  <span>{cat.name}</span>
                  <div>
                    <button
                      type="button"
                      className={styles.iconActionButton}
                      onClick={() => setEditingCategory(cat)}
                      aria-label="Editar categoria"
                      title="Editar categoria"
                    >
                      <PencilSimple size={17} weight="bold" />
                    </button>
                    <button
                      type="button"
                      className={styles.deleteButton}
                      onClick={() => handleDeleteCategory(cat)}
                      aria-label="Excluir categoria"
                      title="Excluir categoria"
                    >
                      <Trash size={17} weight="bold" />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>

      <BarberNavbar activeTab="estoque" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

      {movementProduct && (
        <StockMovementModal
          product={movementProduct}
          onClose={() => setMovementProduct(null)}
          onConfirm={handleCreateMovement}
        />
      )}
    </div>
  );
}

export default BarberStockPage;
