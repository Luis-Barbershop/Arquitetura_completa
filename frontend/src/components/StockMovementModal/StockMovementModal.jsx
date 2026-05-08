import { useMemo, useState } from 'react';
import styles from './StockMovementModal.module.css';

const FLAGS = [
  { type: 'OUT_CONSUMPTION', label: 'Consumo interno' },
  { type: 'OUT_SALE', label: 'Venda' },
  { type: 'LOSS', label: 'Perda / Descarte' },
  { type: 'RETURN', label: 'Devolucao' },
  { type: 'IN', label: 'Entrada' },
];

function StockMovementModal({ product, onClose, onConfirm }) {
  const [type, setType] = useState('OUT_CONSUMPTION');
  const [quantity, setQuantity] = useState('1');
  const [unitSalePrice, setUnitSalePrice] = useState('');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);

  const selectedFlag = useMemo(() => FLAGS.find((flag) => flag.type === type), [type]);
  const requiresPrice = type === 'OUT_SALE';

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!product || Number(quantity) <= 0) return;
    if (requiresPrice && !unitSalePrice) return;

    try {
      setSaving(true);
      await onConfirm({
        productId: product.id,
        type,
        quantity: Number(quantity),
        unitSalePrice: requiresPrice ? Number(unitSalePrice) : null,
        notes: notes.trim() || null,
      });
    } finally {
      setSaving(false);
    }
  };

  if (!product) return null;

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(event) => event.stopPropagation()}>
        <div className={styles.header}>
          <div>
            <p className={styles.kicker}>MOVIMENTACAO</p>
            <h3>{product.name}</h3>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Fechar">
            x
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.flagsGrid}>
            {FLAGS.map((flag) => (
              <button
                key={flag.type}
                type="button"
                className={`${styles.flagButton} ${type === flag.type ? styles.flagButtonActive : ''}`}
                onClick={() => setType(flag.type)}
              >
                {flag.label}
              </button>
            ))}
          </div>

          <label className={styles.label} htmlFor="movement-quantity">Quantidade</label>
          <input
            id="movement-quantity"
            className={styles.input}
            type="number"
            min="1"
            step="1"
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
            required
          />

          {requiresPrice && (
            <>
              <label className={styles.label} htmlFor="movement-sale-price">Preco unitario de venda</label>
              <input
                id="movement-sale-price"
                className={styles.input}
                type="number"
                min="0"
                step="0.01"
                value={unitSalePrice}
                onChange={(event) => setUnitSalePrice(event.target.value)}
                required
              />
            </>
          )}

          <label className={styles.label} htmlFor="movement-notes">Observacao</label>
          <textarea
            id="movement-notes"
            className={styles.textarea}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            placeholder={`Opcional para ${selectedFlag?.label || 'movimentacao'}`}
          />

          <div className={styles.actions}>
            <button type="button" className={styles.secondaryButton} onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className={styles.primaryButton} disabled={saving}>
              {saving ? 'Salvando...' : 'Registrar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default StockMovementModal;
