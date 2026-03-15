import Styles from './CSS/Stock.module.css'

function Stock() {
  return (
    <div className={Styles.container}>
        <div className={Styles.stockCard}>
            <div className={Styles.stockIcon}></div>
            <h3>Estoque Baixo</h3>
        </div>

        <div className={Styles.stockNumber}>
            <h1>03</h1>
        </div>

        <div className={Styles.stockDetails}>
            <p>Produtos com estoque baixo</p>
        </div>

        <div>
        <button className={Styles.stockButton}>
            Ver Detalhes
        </button>
        </div>

    </div>
  )
}

export default Stock