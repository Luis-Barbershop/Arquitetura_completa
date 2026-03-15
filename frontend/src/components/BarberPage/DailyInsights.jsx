import Styles from "./CSS/DailyInsights.module.css"

function DailyInsights() {
  return (
    <div className={Styles.container}>
        <div className={Styles.headerDailyInsights}>
            <div className={Styles.dailyInsightIcon}>
                <img src="./Icons/dailyInsights.png" alt="Ícone de Insights" />
            </div>

            <div>
            <h4 className={Styles.title}>Insights Diários</h4>
            <p>INTELIGÊNCIA CORTA AÍ</p>
            </div>

        </div>

        <div className={Styles.content}>
            <p>Sua taxa de ocupação para <span>Sábado</span> está em 95%. Considere Abrir um horário extra para maximizar o lucro</p>
        </div>

        <div className={Styles.footer}>
            <p>POTENCIAL DE + R$250,00</p>
            <button className={Styles.seeMoreButton}>Ver Detalhes</button>
        </div>

    </div>
  )
}

export default DailyInsights